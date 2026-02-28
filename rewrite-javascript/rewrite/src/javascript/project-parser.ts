/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as path from "path";
import * as fs from "fs";
import * as fsp from "fs/promises";
import * as os from "os";
import {spawnSync} from "child_process";
import picomatch from "picomatch";
import {create as produce} from "mutative";
import {SourceFile} from "../tree";
import {Parsers} from "../parser";
import {PrettierConfigLoader} from "./format/prettier-config-loader";
import {ExecutionContext} from "../execution";
import {Marker, replaceMarkerByKind} from "../markers";
import {TsConfig, detectTsConfigPath, readTsConfigFromPath} from "./tsconfig";
import {JavaScriptParser} from "./parser";

// Lock file names defined here to avoid circular dependency with package-manager.ts
// These must be kept in sync with the definitions in package-manager.ts
const JSON_LOCK_FILE_NAMES = ['bun.lock', 'package-lock.json'] as const;
const YAML_LOCK_FILE_NAMES = ['pnpm-lock.yaml'] as const;
const TEXT_LOCK_FILE_NAMES = ['yarn.lock'] as const;

/**
 * Detects if a yarn.lock file is Yarn Berry (v2+) format based on content.
 * Yarn Berry lock files contain a `__metadata:` key which is not present in Classic.
 */
function isYarnBerryLockFile(content: string): boolean {
    return content.includes('__metadata:');
}

/**
 * Options for project parsing.
 */
export interface ProjectParserOptions {
    /**
     * Optional execution context.
     */
    ctx?: ExecutionContext;

    /**
     * Glob patterns to exclude from source file discovery.
     * Default excludes: node_modules, dist, build, .git, coverage, *.min.js, *.bundle.js
     */
    exclusions?: string[];

    /**
     * Whether to use git to discover files (respects .gitignore).
     * Default: true if in a git repository.
     */
    useGit?: boolean;

    /**
     * Progress callback for file parsing.
     */
    onProgress?: (phase: "discovering" | "parsing", current: number, total: number, filePath?: string) => void;

    /**
     * Whether to enable verbose logging.
     */
    verbose?: boolean;

    /**
     * Optional predicate to filter which files should be parsed.
     * Called with the absolute file path after file discovery.
     * Return true to include the file, false to exclude it.
     * If not provided, all discovered files are parsed.
     */
    fileFilter?: (absolutePath: string) => boolean;

    /**
     * Optional path to make source file paths relative to.
     * If not specified, paths are relative to projectPath.
     * Use this when parsing a subdirectory but wanting paths relative to the repository root.
     */
    relativeTo?: string;
}

/**
 * Result of file discovery.
 */
export interface DiscoveredFiles {
    /** package.json files - parsed with PackageJsonParser for NodeResolutionResult */
    packageJsonFiles: string[];
    /** Lock files grouped by parser type */
    lockFiles: {
        json: string[];   // package-lock.json, bun.lock
        yaml: string[];   // pnpm-lock.yaml, yarn.lock (Berry)
        text: string[];   // yarn.lock (Classic)
    };
    /** JavaScript/TypeScript files (includes .prettierrc.js, prettier.config.js) */
    jsFiles: string[];
    /** JSON files (tsconfig.json, .prettierrc.json, other .json) */
    jsonFiles: string[];
    /** YAML files (.prettierrc.yaml, other .yaml/.yml) */
    yamlFiles: string[];
    /** Plain text config files (.prettierignore, .gitignore, etc.) */
    textFiles: string[];
}

/**
 * Default exclusion patterns for file discovery.
 */
export const DEFAULT_EXCLUSIONS = [
    "**/node_modules/**",
    "**/dist/**",
    "**/build/**",
    "**/.git/**",
    "**/coverage/**",
    "**/*.min.js",
    "**/*.bundle.js"
];

/**
 * Source file extensions for JavaScript/TypeScript.
 */
const SOURCE_EXTENSIONS = new Set([
    ".js", ".jsx", ".ts", ".tsx", ".mjs", ".cjs", ".mts", ".cts"
]);

/**
 * All lock file names for quick lookup.
 */
const ALL_LOCK_FILE_NAMES = new Set<string>([
    ...JSON_LOCK_FILE_NAMES,
    ...YAML_LOCK_FILE_NAMES,
    ...TEXT_LOCK_FILE_NAMES
]);

/**
 * Plain text config files (no extension).
 */
const TEXT_CONFIG_FILES = new Set([
    ".prettierignore",
    ".gitignore",
    ".npmignore",
    ".eslintignore"
]);

/**
 * Parses an entire JavaScript/TypeScript project.
 *
 * This class handles:
 * - File discovery (source files, package.json, lock files)
 * - Prettier configuration detection (once per project)
 * - Parsing all files with appropriate parsers
 * - Sharing PrettierConfigLoader across all JavaScript parsers
 *
 * Can be used directly for CLI tools or wrapped by RPC handlers.
 */
export class ProjectParser {
    private readonly projectPath: string;
    private readonly relativeTo: string;
    private readonly exclusions: string[];
    private readonly ctx: ExecutionContext;
    private readonly useGit: boolean;
    private readonly onProgress?: ProjectParserOptions["onProgress"];
    private readonly verbose: boolean;
    private readonly fileFilter?: (absolutePath: string) => boolean;

    constructor(projectPath: string, options: ProjectParserOptions = {}) {
        this.projectPath = path.resolve(projectPath);
        this.relativeTo = options.relativeTo ? path.resolve(options.relativeTo) : this.projectPath;
        this.exclusions = options.exclusions ?? DEFAULT_EXCLUSIONS;
        this.ctx = options.ctx ?? new ExecutionContext();
        this.useGit = options.useGit ?? this.isGitRepository();
        this.onProgress = options.onProgress;
        this.verbose = options.verbose ?? false;
        this.fileFilter = options.fileFilter;
    }

    /**
     * Creates and initializes a PrettierConfigLoader for this project.
     * Use this when you need to handle Prettier detection separately from parsing.
     */
    async createPrettierLoader(): Promise<PrettierConfigLoader> {
        this.log("Detecting Prettier configuration...");
        const prettierLoader = new PrettierConfigLoader(this.projectPath);
        await prettierLoader.detectPrettier();
        return prettierLoader;
    }

    /**
     * Builds an Autodetect marker from the given source files.
     * Samples all files to detect common formatting styles.
     * Uses dynamic import to avoid circular dependencies.
     */
    async buildAutodetectMarker(sourceFiles: SourceFile[]): Promise<Marker> {
        // Dynamic import to break circular dependency at module load time:
        // parse-project.ts → project-parser.ts → autodetect.ts → visitor.ts → java → rpc → parse-project.ts
        // By deferring the import until runtime, all modules are already loaded when this executes.
        const {Autodetect} = await import("./autodetect.js");
        const {JS} = await import("./tree.js");

        const detector = Autodetect.detector();
        for (const sf of sourceFiles) {
            if (sf.kind === JS.Kind.CompilationUnit) {
                await detector.sample(sf);
            }
        }
        return detector.build();
    }

    /**
     * Detects and reads the TsConfig for the project by examining package.json scripts.
     * Looks for tsc commands with -p/--project flags to determine the config file.
     *
     * @param packageJsonFiles List of discovered package.json file paths (relative to relativeTo)
     * @returns A TsConfig marker if detected, undefined otherwise
     */
    private async detectTsConfig(packageJsonFiles: string[]): Promise<TsConfig | undefined> {
        // Try the root package.json first (shortest path = closest to root)
        const sortedPackageJsons = [...packageJsonFiles].sort((a, b) => a.length - b.length);

        for (const pkgJsonPath of sortedPackageJsons) {
            const absolutePath = path.join(this.relativeTo, pkgJsonPath);
            try {
                const content = await fsp.readFile(absolutePath, 'utf-8');
                const packageJson = JSON.parse(content);

                const configPath = detectTsConfigPath(packageJson);
                if (configPath) {
                    const pkgDir = path.dirname(absolutePath);
                    const absoluteConfigPath = path.resolve(pkgDir, configPath);

                    const tsConfig = readTsConfigFromPath(absoluteConfigPath, this.relativeTo);
                    if (tsConfig) {
                        this.log(`Detected TypeScript config: ${tsConfig.configPath}`);
                        return tsConfig;
                    }
                }
            } catch (error) {
                // Ignore errors reading package.json, continue to next one
                this.log(`Could not read ${pkgJsonPath}: ${error}`);
            }
        }

        // Fallback: check if tsconfig.json exists at project root
        const defaultConfigPath = path.join(this.relativeTo, 'tsconfig.json');
        if (fs.existsSync(defaultConfigPath)) {
            const tsConfig = readTsConfigFromPath(defaultConfigPath, this.relativeTo);
            if (tsConfig) {
                this.log(`Using default TypeScript config: tsconfig.json`);
                return tsConfig;
            }
        }

        this.log("No TypeScript configuration detected");
        return undefined;
    }

    /**
     * Parses all source files in the project.
     * Yields source files as they are parsed.
     */
    async *parse(): AsyncGenerator<SourceFile> {
        // Discover files
        this.log("Discovering files...");
        this.onProgress?.("discovering", 0, 0);
        let discovered = await this.discoverFiles();

        // Apply file filter if provided
        if (this.fileFilter) {
            discovered = this.applyFileFilter(discovered);
        }

        const totalFiles = this.countFiles(discovered);
        this.log(`Found ${totalFiles} files to parse`);

        // Detect Prettier configuration once for the project
        const prettierLoader = await this.createPrettierLoader();

        let current = 0;

        // Parse package.json files first (they get NodeResolutionResult markers)
        if (discovered.packageJsonFiles.length > 0) {
            this.log(`Parsing ${discovered.packageJsonFiles.length} package.json files...`);
            const parser = Parsers.createParser("packageJson", {
                ctx: this.ctx,
                relativeTo: this.relativeTo
            });
            for await (const sf of parser.parse(...discovered.packageJsonFiles)) {
                current++;
                this.onProgress?.("parsing", current, totalFiles, sf.sourcePath);
                yield sf;
            }
        }

        // Parse JSON lock files
        if (discovered.lockFiles.json.length > 0) {
            this.log(`Parsing ${discovered.lockFiles.json.length} JSON lock files...`);
            const parser = Parsers.createParser("json", {
                ctx: this.ctx,
                relativeTo: this.relativeTo
            });
            for await (const sf of parser.parse(...discovered.lockFiles.json)) {
                current++;
                this.onProgress?.("parsing", current, totalFiles, sf.sourcePath);
                yield sf;
            }
        }

        // Parse YAML lock files
        if (discovered.lockFiles.yaml.length > 0) {
            this.log(`Parsing ${discovered.lockFiles.yaml.length} YAML lock files...`);
            const parser = Parsers.createParser("yaml", {
                ctx: this.ctx,
                relativeTo: this.relativeTo
            });
            for await (const sf of parser.parse(...discovered.lockFiles.yaml)) {
                current++;
                this.onProgress?.("parsing", current, totalFiles, sf.sourcePath);
                yield sf;
            }
        }

        // Parse text lock files (yarn.lock Classic)
        if (discovered.lockFiles.text.length > 0) {
            this.log(`Parsing ${discovered.lockFiles.text.length} text lock files...`);
            const parser = Parsers.createParser("plainText", {
                ctx: this.ctx,
                relativeTo: this.relativeTo
            });
            for await (const sf of parser.parse(...discovered.lockFiles.text)) {
                current++;
                this.onProgress?.("parsing", current, totalFiles, sf.sourcePath);
                yield sf;
            }
        }

        // Parse JavaScript/TypeScript source files
        if (discovered.jsFiles.length > 0) {
            this.log(`Parsing ${discovered.jsFiles.length} JavaScript/TypeScript files...`);

            // Detect TsConfig from root package.json
            const tsConfig = await this.detectTsConfig(discovered.packageJsonFiles);

            const parser = new JavaScriptParser({
                ctx: this.ctx,
                relativeTo: this.relativeTo,
                tsConfig
            });

            // Check if Prettier is available
            const detection = await prettierLoader.detectPrettier();

            if (detection.available) {
                // Prettier is available: add per-file PrettierStyle markers
                for await (const sf of parser.parse(...discovered.jsFiles)) {
                    current++;
                    this.onProgress?.("parsing", current, totalFiles, sf.sourcePath);

                    const prettierMarker = await prettierLoader.getConfigMarker(
                        path.join(this.relativeTo, sf.sourcePath)
                    );
                    if (prettierMarker) {
                        yield produce(sf, draft => {
                            draft.markers = replaceMarkerByKind(draft.markers, prettierMarker);
                        });
                    } else {
                        yield sf;
                    }
                }
            } else {
                // Prettier is NOT available: auto-detect styles from parsed files
                this.log("Prettier not found, auto-detecting styles...");

                // Dynamic import to break circular dependency at module load time
                // (see buildAutodetectMarker for explanation)
                const {Autodetect} = await import("./autodetect.js");
                const {JS} = await import("./tree.js");

                const parsedFiles: SourceFile[] = [];

                // Parse all JS files and collect them for sampling
                for await (const sf of parser.parse(...discovered.jsFiles)) {
                    current++;
                    this.onProgress?.("parsing", current, totalFiles, sf.sourcePath);
                    if (sf.kind === JS.Kind.CompilationUnit) {
                        parsedFiles.push(sf);
                    }
                }

                // Sample all parsed files and build Autodetect marker
                const detector = Autodetect.detector();
                for (const sf of parsedFiles) {
                    await detector.sample(sf);
                }
                const autodetectMarker = detector.build();
                this.log(`Auto-detected styles: indent=${detector.getTabsAndIndentsStyle().indentSize}, ` +
                    `useTabs=${detector.getTabsAndIndentsStyle().useTabCharacter}`);

                // Yield all files with the Autodetect marker
                for (const sf of parsedFiles) {
                    yield produce(sf, draft => {
                        draft.markers = replaceMarkerByKind(draft.markers, autodetectMarker);
                    });
                }
            }
        }

        // Parse other YAML files
        if (discovered.yamlFiles.length > 0) {
            this.log(`Parsing ${discovered.yamlFiles.length} YAML files...`);
            const parser = Parsers.createParser("yaml", {
                ctx: this.ctx,
                relativeTo: this.relativeTo
            });
            for await (const sf of parser.parse(...discovered.yamlFiles)) {
                current++;
                this.onProgress?.("parsing", current, totalFiles, sf.sourcePath);
                yield sf;
            }
        }

        // Parse other JSON files
        if (discovered.jsonFiles.length > 0) {
            this.log(`Parsing ${discovered.jsonFiles.length} JSON files...`);
            const parser = Parsers.createParser("json", {
                ctx: this.ctx,
                relativeTo: this.relativeTo
            });
            for await (const sf of parser.parse(...discovered.jsonFiles)) {
                current++;
                this.onProgress?.("parsing", current, totalFiles, sf.sourcePath);
                yield sf;
            }
        }

        // Parse text config files (.prettierignore, .gitignore, etc.)
        if (discovered.textFiles.length > 0) {
            this.log(`Parsing ${discovered.textFiles.length} text config files...`);
            const parser = Parsers.createParser("plainText", {
                ctx: this.ctx,
                relativeTo: this.relativeTo
            });
            for await (const sf of parser.parse(...discovered.textFiles)) {
                current++;
                this.onProgress?.("parsing", current, totalFiles, sf.sourcePath);
                yield sf;
            }
        }
    }

    /**
     * Discovers all files in the project that should be parsed.
     */
    async discoverFiles(): Promise<DiscoveredFiles> {
        const discovered: DiscoveredFiles = {
            packageJsonFiles: [],
            lockFiles: {
                json: [],
                yaml: [],
                text: []
            },
            jsFiles: [],
            jsonFiles: [],
            yamlFiles: [],
            textFiles: []
        };

        let files: string[];
        if (this.useGit) {
            files = await this.discoverFilesWithGit();
        } else {
            files = await this.discoverFilesWithWalk();
        }

        // Classify files
        const yarnLockFiles: string[] = [];

        for (const file of files) {
            const basename = path.basename(file);
            const ext = path.extname(file).toLowerCase();

            if (basename === "package.json") {
                discovered.packageJsonFiles.push(file);
            } else if (SOURCE_EXTENSIONS.has(ext)) {
                discovered.jsFiles.push(file);
            } else if ((JSON_LOCK_FILE_NAMES as readonly string[]).includes(basename)) {
                discovered.lockFiles.json.push(file);
            } else if (basename === "yarn.lock") {
                // yarn.lock needs content-based classification
                yarnLockFiles.push(file);
            } else if ((YAML_LOCK_FILE_NAMES as readonly string[]).includes(basename)) {
                discovered.lockFiles.yaml.push(file);
            } else if ((TEXT_LOCK_FILE_NAMES as readonly string[]).includes(basename)) {
                discovered.lockFiles.text.push(file);
            } else if (ext === ".json") {
                discovered.jsonFiles.push(file);
            } else if (ext === ".yaml" || ext === ".yml") {
                discovered.yamlFiles.push(file);
            } else if (TEXT_CONFIG_FILES.has(basename)) {
                discovered.textFiles.push(file);
            }
        }

        // Classify yarn.lock files by content
        for (const yarnLockPath of yarnLockFiles) {
            const format = await this.classifyYarnLockFile(yarnLockPath);
            if (format === "yaml") {
                discovered.lockFiles.yaml.push(yarnLockPath);
            } else {
                discovered.lockFiles.text.push(yarnLockPath);
            }
        }

        return discovered;
    }

    /**
     * Discovers files using git ls-files (respects .gitignore).
     */
    private async discoverFilesWithGit(): Promise<string[]> {
        const files: string[] = [];

        // Get tracked files
        const tracked = spawnSync("git", ["ls-files"], {
            cwd: this.projectPath,
            encoding: "utf8",
            ...(os.platform() === 'win32' ? { shell: true } : {})
        });

        if (tracked.status !== 0 || tracked.error) {
            // Fall back to walk if git fails
            return this.discoverFilesWithWalk();
        }

        if (tracked.stdout) {
            for (const line of tracked.stdout.split("\n")) {
                const trimmed = line.trim();
                if (trimmed) {
                    const fullPath = path.join(this.projectPath, trimmed);
                    // git ls-files can return deleted files that are still tracked
                    if (fs.existsSync(fullPath)) {
                        files.push(fullPath);
                    }
                }
            }
        }

        // Get untracked but not ignored files
        const untracked = spawnSync("git", ["ls-files", "--others", "--exclude-standard"], {
            cwd: this.projectPath,
            encoding: "utf8",
            ...(os.platform() === 'win32' ? { shell: true } : {})
        });

        if (untracked.stdout) {
            for (const line of untracked.stdout.split("\n")) {
                const trimmed = line.trim();
                if (trimmed) {
                    // Untracked files should exist, but check anyway for robustness
                    const fullPath = path.join(this.projectPath, trimmed);
                    if (fs.existsSync(fullPath)) {
                        files.push(fullPath);
                    }
                }
            }
        }

        // Filter by our exclusion patterns and accepted file types
        return files.filter(file => this.isAcceptedFile(file));
    }

    /**
     * Discovers files by walking the directory tree.
     */
    private async discoverFilesWithWalk(): Promise<string[]> {
        const files: string[] = [];
        const isExcluded = picomatch(this.exclusions);

        const walk = async (dir: string, relativePath: string = "") => {
            let entries: fs.Dirent[];
            try {
                entries = await fsp.readdir(dir, {withFileTypes: true});
            } catch {
                return;
            }

            for (const entry of entries) {
                const fullPath = path.join(dir, entry.name);
                const relPath = relativePath ? `${relativePath}/${entry.name}` : entry.name;

                // Check exclusion patterns
                if (isExcluded(relPath) || isExcluded(`${relPath}/`)) {
                    continue;
                }

                // Always skip node_modules
                if (entry.name === "node_modules") {
                    continue;
                }

                if (entry.isDirectory()) {
                    await walk(fullPath, relPath);
                } else if (entry.isFile() && this.isAcceptedFile(fullPath)) {
                    files.push(fullPath);
                }
            }
        };

        await walk(this.projectPath);
        return files;
    }

    /**
     * Checks if a file should be accepted for parsing.
     */
    private isAcceptedFile(filePath: string): boolean {
        const basename = path.basename(filePath);
        const ext = path.extname(filePath).toLowerCase();

        // Check exclusion patterns with relative path
        const relativePath = path.relative(this.projectPath, filePath);
        const isExcluded = picomatch(this.exclusions);
        if (isExcluded(relativePath)) {
            return false;
        }

        // JavaScript/TypeScript files
        if (SOURCE_EXTENSIONS.has(ext)) {
            return true;
        }

        // JSON files
        if (ext === ".json") {
            return true;
        }

        // YAML files
        if (ext === ".yaml" || ext === ".yml") {
            return true;
        }

        // Lock files (some have non-standard extensions)
        if (ALL_LOCK_FILE_NAMES.has(basename)) {
            return true;
        }

        // Text config files
        if (TEXT_CONFIG_FILES.has(basename)) {
            return true;
        }

        return false;
    }

    /**
     * Classifies a yarn.lock file as YAML (Berry) or text (Classic).
     */
    private async classifyYarnLockFile(filePath: string): Promise<"yaml" | "text"> {
        try {
            const content = await fsp.readFile(filePath, "utf-8");
            return isYarnBerryLockFile(content) ? "yaml" : "text";
        } catch {
            return "text";
        }
    }

    /**
     * Checks if the project is a git repository.
     */
    private isGitRepository(): boolean {
        return fs.existsSync(path.join(this.projectPath, ".git"));
    }

    /**
     * Counts total files to parse.
     */
    private countFiles(discovered: DiscoveredFiles): number {
        return (
            discovered.packageJsonFiles.length +
            discovered.lockFiles.json.length +
            discovered.lockFiles.yaml.length +
            discovered.lockFiles.text.length +
            discovered.jsFiles.length +
            discovered.jsonFiles.length +
            discovered.yamlFiles.length +
            discovered.textFiles.length
        );
    }

    /**
     * Applies the file filter to discovered files.
     */
    private applyFileFilter(discovered: DiscoveredFiles): DiscoveredFiles {
        const filter = this.fileFilter!;
        return {
            packageJsonFiles: discovered.packageJsonFiles.filter(filter),
            lockFiles: {
                json: discovered.lockFiles.json.filter(filter),
                yaml: discovered.lockFiles.yaml.filter(filter),
                text: discovered.lockFiles.text.filter(filter)
            },
            jsFiles: discovered.jsFiles.filter(filter),
            jsonFiles: discovered.jsonFiles.filter(filter),
            yamlFiles: discovered.yamlFiles.filter(filter),
            textFiles: discovered.textFiles.filter(filter)
        };
    }

    /**
     * Logs a message if verbose mode is enabled.
     */
    private log(message: string): void {
        if (this.verbose) {
            console.log(message);
        }
    }
}
