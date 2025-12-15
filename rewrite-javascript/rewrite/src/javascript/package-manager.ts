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

import {
    createNodeResolutionResultMarker,
    findNodeResolutionResult,
    PackageJsonContent,
    PackageLockContent,
    PackageManager,
    readNpmrcConfigs
} from "./node-resolution-result";
import {replaceMarkerByKind} from "../markers";
import {Json, JsonParser, JsonVisitor} from "../json";
import {isDocuments, Yaml, YamlParser, YamlVisitor} from "../yaml";
import {PlainTextParser} from "../text";
import {SourceFile} from "../tree";
import {TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import * as fs from "fs";
import * as fsp from "fs/promises";
import * as path from "path";
import * as os from "os";
import {spawnSync} from "child_process";
import * as YAML from "yaml";

/**
 * Configuration for each package manager.
 */
interface PackageManagerConfig {
    /** The lock file name for this package manager */
    lockFile: string;

    /** Command to install dependencies and update lock file only (no node_modules) */
    installLockOnlyCommand: string[];

    /** Command to install dependencies fully */
    installCommand: string[];

    /** Command to list dependencies in JSON format (if supported) */
    listCommand?: string[];
}

/**
 * Package manager configurations.
 */
const PACKAGE_MANAGER_CONFIGS: Record<PackageManager, PackageManagerConfig> = {
    [PackageManager.Npm]: {
        lockFile: 'package-lock.json',
        installLockOnlyCommand: ['npm', 'install', '--package-lock-only'],
        installCommand: ['npm', 'install'],
        listCommand: ['npm', 'list', '--json', '--all'],
    },
    [PackageManager.YarnClassic]: {
        lockFile: 'yarn.lock',
        // Yarn Classic doesn't have a lock-only mode
        installLockOnlyCommand: ['yarn', 'install', '--ignore-scripts'],
        installCommand: ['yarn', 'install'],
        listCommand: ['yarn', 'list', '--json'],
    },
    [PackageManager.YarnBerry]: {
        lockFile: 'yarn.lock',
        // Yarn Berry's mode skip-build skips post-install scripts
        installLockOnlyCommand: ['yarn', 'install', '--mode', 'skip-build'],
        installCommand: ['yarn', 'install'],
        listCommand: ['yarn', 'info', '--all', '--json'],
    },
    [PackageManager.Pnpm]: {
        lockFile: 'pnpm-lock.yaml',
        installLockOnlyCommand: ['pnpm', 'install', '--lockfile-only'],
        installCommand: ['pnpm', 'install'],
        listCommand: ['pnpm', 'list', '--json', '--depth=Infinity'],
    },
    [PackageManager.Bun]: {
        lockFile: 'bun.lock',
        // Bun doesn't have a lock-only mode, but is very fast anyway
        installLockOnlyCommand: ['bun', 'install', '--ignore-scripts'],
        installCommand: ['bun', 'install'],
    },
};

/**
 * Configuration for lock file detection.
 */
export interface LockFileDetectionConfig {
    /** The lock file name */
    filename: string;
    /** The package manager, or a function to detect it from file content */
    packageManager: PackageManager | ((content: string) => PackageManager);
    /** If true, prefer walking node_modules over parsing lock file (lock file may omit details) */
    preferNodeModules?: boolean;
}

/**
 * Lock file detection configuration with priority order.
 * Priority order determines which package manager is detected when multiple lock files exist.
 */
const LOCK_FILE_DETECTION: ReadonlyArray<LockFileDetectionConfig> = [
    {filename: 'package-lock.json', packageManager: PackageManager.Npm},
    {filename: 'bun.lock', packageManager: PackageManager.Bun},
    {filename: 'pnpm-lock.yaml', packageManager: PackageManager.Pnpm, preferNodeModules: true},
    {
        filename: 'yarn.lock',
        packageManager: (content) =>
            content.includes('__metadata:') ? PackageManager.YarnBerry : PackageManager.YarnClassic,
        // yarn.lock omits transitive dependency details (engines/license), so prefer node_modules
        preferNodeModules: true
    },
];

/**
 * Lock file names that should be parsed as JSON/JSONC format.
 */
export const JSON_LOCK_FILE_NAMES = ['bun.lock', 'package-lock.json'] as const;

/**
 * Lock file names that should be parsed as YAML format.
 */
export const YAML_LOCK_FILE_NAMES = ['pnpm-lock.yaml'] as const;

/**
 * Lock file names that should be parsed as plain text (custom formats like yarn.lock v1).
 * Note: yarn.lock for Yarn Berry (v2+) is actually YAML format and should be parsed as such.
 * Use `getLockFileFormat` with content to determine the correct format for yarn.lock.
 */
export const TEXT_LOCK_FILE_NAMES = ['yarn.lock'] as const;

/**
 * Detects if a yarn.lock file is Yarn Berry (v2+) format based on content.
 * Yarn Berry lock files contain a `__metadata:` key which is not present in Classic.
 *
 * @param content The yarn.lock file content
 * @returns true if this is a Yarn Berry lock file (YAML format), false for Classic
 */
export function isYarnBerryLockFile(content: string): boolean {
    return content.includes('__metadata:');
}

/**
 * Result of running a package manager command.
 */
interface PackageManagerResult {
    success: boolean;
    stdout?: string;
    stderr?: string;
    error?: string;
}

/**
 * Options for running package manager install.
 */
interface InstallOptions {
    /** Working directory */
    cwd: string;

    /** If true, only update lock file without installing to node_modules */
    lockOnly?: boolean;

    /** Timeout in milliseconds (default: 120000 = 2 minutes) */
    timeout?: number;

    /** Additional environment variables */
    env?: Record<string, string>;
}

/**
 * Detects the package manager used in a directory by checking for lock files.
 *
 * @param dir The directory to check
 * @returns The detected package manager, or undefined if none found
 */
export function detectPackageManager(dir: string): PackageManager | undefined {
    for (const config of LOCK_FILE_DETECTION) {
        const lockPath = path.join(dir, config.filename);
        if (fs.existsSync(lockPath)) {
            if (typeof config.packageManager === 'function') {
                try {
                    const content = fs.readFileSync(lockPath, 'utf-8');
                    return config.packageManager(content);
                } catch {
                    continue;
                }
            }
            return config.packageManager;
        }
    }
    return undefined;
}

/**
 * Gets the lock file detection configuration.
 * Returns the array of lock file configs in priority order.
 */
export function getLockFileDetectionConfig(): ReadonlyArray<LockFileDetectionConfig> {
    return LOCK_FILE_DETECTION;
}

/**
 * Gets the lock file name for a package manager.
 */
export function getLockFileName(pm: PackageManager): string {
    return PACKAGE_MANAGER_CONFIGS[pm].lockFile;
}

/**
 * Gets all supported lock file names.
 */
export function getAllLockFileNames(): string[] {
    return LOCK_FILE_DETECTION.map(c => c.filename);
}

/**
 * Runs the package manager install command.
 */
function runInstall(pm: PackageManager, options: InstallOptions): PackageManagerResult {
    const config = PACKAGE_MANAGER_CONFIGS[pm];
    const command = options.lockOnly ? config.installLockOnlyCommand : config.installCommand;
    const [cmd, ...args] = command;

    try {
        const result = spawnSync(cmd, args, {
            cwd: options.cwd,
            encoding: 'utf-8',
            stdio: ['pipe', 'pipe', 'pipe'],
            timeout: options.timeout ?? 120000,
            env: options.env ? {...process.env, ...options.env} : process.env,
        });

        if (result.error) {
            return {
                success: false,
                error: result.error.message,
                stderr: result.stderr,
            };
        }

        if (result.status !== 0) {
            return {
                success: false,
                stdout: result.stdout,
                stderr: result.stderr,
                error: `Command exited with code ${result.status}`,
            };
        }

        return {
            success: true,
            stdout: result.stdout,
            stderr: result.stderr,
        };
    } catch (error: any) {
        return {
            success: false,
            error: error.message,
        };
    }
}

/**
 * Runs a package manager list command to get dependency information.
 *
 * @param pm The package manager to use
 * @param cwd Working directory
 * @param timeout Timeout in milliseconds
 * @returns The JSON output, or undefined if failed
 */
export function runList(pm: PackageManager, cwd: string, timeout: number = 30000): string | undefined {
    const config = PACKAGE_MANAGER_CONFIGS[pm];
    if (!config.listCommand) {
        return undefined;
    }

    const [cmd, ...args] = config.listCommand;

    const result = spawnSync(cmd, args, {
        cwd,
        encoding: 'utf-8',
        stdio: ['pipe', 'pipe', 'pipe'],
        timeout,
    });

    if (result.error || result.status !== 0) {
        return undefined;
    }

    return result.stdout;
}

/**
 * Result of running install in a temporary directory.
 */
export interface TempInstallResult {
    /** Whether the install succeeded */
    success: boolean;
    /** The updated lock file content (if successful and lock file exists) */
    lockFileContent?: string;
    /** Error message (if failed) */
    error?: string;
}

/**
 * Generic accumulator for dependency recipes that run package manager operations.
 * Used by scanning recipes to track state across scanning and editing phases.
 *
 * @typeParam T The recipe-specific project update info type
 */
export interface DependencyRecipeAccumulator<T> {
    /** Projects that need updating: packageJsonPath -> update info */
    projectsToUpdate: Map<string, T>;

    /** After running package manager, store the updated lock file content */
    updatedLockFiles: Map<string, string>;

    /** Updated package.json content (after npm install may have modified it) */
    updatedPackageJsons: Map<string, string>;

    /** Track which projects have been processed (npm install has run) */
    processedProjects: Set<string>;

    /** Track projects where npm install failed: packageJsonPath -> error message */
    failedProjects: Map<string, string>;
}

/**
 * Creates a new empty accumulator for dependency recipes.
 */
export function createDependencyRecipeAccumulator<T>(): DependencyRecipeAccumulator<T> {
    return {
        projectsToUpdate: new Map(),
        updatedLockFiles: new Map(),
        updatedPackageJsons: new Map(),
        processedProjects: new Set(),
        failedProjects: new Map()
    };
}

/**
 * Checks if a source path is a lock file and returns the updated content if available.
 * This is a helper for dependency recipes that need to update lock files.
 *
 * @param sourcePath The source path to check
 * @param acc The recipe accumulator containing updated lock file content
 * @returns The updated lock file content if this is a lock file that was updated, undefined otherwise
 */
export function getUpdatedLockFileContent<T>(
    sourcePath: string,
    acc: DependencyRecipeAccumulator<T>
): string | undefined {
    for (const lockFileName of getAllLockFileNames()) {
        if (sourcePath.endsWith(lockFileName)) {
            // Find the corresponding package.json path
            const packageJsonPath = sourcePath.replace(lockFileName, 'package.json');
            const updateInfo = acc.projectsToUpdate.get(packageJsonPath);

            if (updateInfo && acc.updatedLockFiles.has(sourcePath)) {
                return acc.updatedLockFiles.get(sourcePath);
            }
            break;
        }
    }
    return undefined;
}

/**
 * Determines the appropriate parser for a lock file based on its filename and optionally content.
 *
 * For yarn.lock files, the format depends on the Yarn version:
 * - Yarn Classic (v1): Custom plain text format
 * - Yarn Berry (v2+): YAML format
 *
 * If content is provided for yarn.lock, it will be used to detect the format.
 * Otherwise, defaults to 'text' (Yarn Classic).
 *
 * @param lockFileName The lock file name (e.g., "pnpm-lock.yaml", "package-lock.json", "yarn.lock")
 * @param content Optional file content (used for yarn.lock format detection)
 * @returns 'yaml' for YAML lock files, 'json' for JSON lock files, 'text' for plain text
 */
export function getLockFileFormat(lockFileName: string, content?: string): 'yaml' | 'json' | 'text' {
    if ((YAML_LOCK_FILE_NAMES as readonly string[]).includes(lockFileName)) {
        return 'yaml';
    }
    if (lockFileName === 'yarn.lock') {
        // Yarn Berry (v2+) uses YAML format, Classic uses custom text format
        if (content && isYarnBerryLockFile(content)) {
            return 'yaml';
        }
        return 'text';
    }
    if ((TEXT_LOCK_FILE_NAMES as readonly string[]).includes(lockFileName)) {
        return 'text';
    }
    // package-lock.json, bun.lock
    return 'json';
}

/**
 * Re-parses updated lock file content using the appropriate parser.
 * This is used by dependency recipes to create the updated lock file SourceFile.
 *
 * For yarn.lock files, the content is used to detect whether it's Yarn Berry (YAML)
 * or Yarn Classic (plain text) format.
 *
 * @param content The updated lock file content
 * @param sourcePath The source path of the lock file
 * @param lockFileName The lock file name (e.g., "pnpm-lock.yaml", "yarn.lock")
 * @returns The parsed SourceFile (Json.Document, Yaml.Documents, or PlainText)
 */
export async function parseLockFileContent(
    content: string,
    sourcePath: string,
    lockFileName: string
): Promise<SourceFile> {
    // Pass content to getLockFileFormat for yarn.lock detection
    const format = getLockFileFormat(lockFileName, content);

    switch (format) {
        case 'yaml': {
            const parser = new YamlParser({});
            return await parser.parseOne({text: content, sourcePath}) as Yaml.Documents;
        }
        case 'text': {
            const parser = new PlainTextParser({});
            return await parser.parseOne({text: content, sourcePath});
        }
        case 'json':
        default: {
            const parser = new JsonParser({});
            return await parser.parseOne({text: content, sourcePath}) as Json.Document;
        }
    }
}

/**
 * Base interface for project update info used by dependency recipes.
 * Recipes extend this with additional fields specific to their needs.
 */
export interface BaseProjectUpdateInfo {
    /** Absolute path to the project directory */
    projectDir: string;
    /** Relative path to package.json (from source root) */
    packageJsonPath: string;
    /** The package manager used by this project */
    packageManager: PackageManager;
}

/**
 * Stores the result of a package manager install into the accumulator.
 * This handles the common pattern of storing updated lock files and tracking failures.
 *
 * @param result The result from runInstallInTempDir
 * @param acc The recipe accumulator
 * @param updateInfo The project update info (must have packageJsonPath and packageManager)
 * @param modifiedPackageJson The modified package.json content that was used for install
 */
export function storeInstallResult<T extends BaseProjectUpdateInfo>(
    result: TempInstallResult,
    acc: DependencyRecipeAccumulator<T>,
    updateInfo: T,
    modifiedPackageJson: string
): void {
    if (result.success) {
        acc.updatedPackageJsons.set(updateInfo.packageJsonPath, modifiedPackageJson);

        if (result.lockFileContent) {
            const lockFileName = getLockFileName(updateInfo.packageManager);
            const lockFilePath = updateInfo.packageJsonPath.replace('package.json', lockFileName);
            acc.updatedLockFiles.set(lockFilePath, result.lockFileContent);
        }
    } else {
        acc.failedProjects.set(updateInfo.packageJsonPath, result.error || 'Unknown error');
    }
}

/**
 * Runs the package manager install for a project if it hasn't been processed yet.
 * Updates the accumulator's processedProjects set after running.
 *
 * @param sourcePath The source path (package.json path) being processed
 * @param acc The recipe accumulator
 * @param runInstall Function that performs the actual install (recipe-specific)
 * @returns The failure message if install failed, undefined otherwise
 */
export async function runInstallIfNeeded<T>(
    sourcePath: string,
    acc: DependencyRecipeAccumulator<T>,
    runInstall: () => Promise<void>
): Promise<string | undefined> {
    if (!acc.processedProjects.has(sourcePath)) {
        await runInstall();
        acc.processedProjects.add(sourcePath);
    }
    return acc.failedProjects.get(sourcePath);
}

/**
 * Updates the NodeResolutionResult marker on a JSON document after a package manager operation.
 * This recreates the marker based on the updated package.json and lock file content.
 *
 * @param doc The JSON document containing the marker
 * @param updateInfo Project update info with paths and package manager
 * @param acc The recipe accumulator containing updated content
 * @returns The document with the updated marker, or unchanged if no existing marker
 */
export async function updateNodeResolutionMarker<T extends BaseProjectUpdateInfo>(
    doc: Json.Document,
    updateInfo: T & { originalPackageJson: string },
    acc: DependencyRecipeAccumulator<T>
): Promise<Json.Document> {
    const existingMarker = findNodeResolutionResult(doc);
    if (!existingMarker) {
        return doc;
    }

    // Parse the updated package.json and lock file to create new marker
    const updatedPackageJson = acc.updatedPackageJsons.get(updateInfo.packageJsonPath);
    const lockFileName = getLockFileName(updateInfo.packageManager);
    const updatedLockFile = acc.updatedLockFiles.get(
        updateInfo.packageJsonPath.replace('package.json', lockFileName)
    );

    let packageJsonContent: PackageJsonContent;
    let lockContent: PackageLockContent | undefined;

    try {
        packageJsonContent = JSON.parse(updatedPackageJson || updateInfo.originalPackageJson);
    } catch {
        return doc; // Failed to parse, keep original marker
    }

    if (updatedLockFile) {
        try {
            // Parse lock file based on format
            if (updateInfo.packageManager === PackageManager.Pnpm) {
                // pnpm-lock.yaml is YAML format
                lockContent = YAML.parse(updatedLockFile);
            } else if (updateInfo.packageManager === PackageManager.YarnClassic ||
                       updateInfo.packageManager === PackageManager.YarnBerry) {
                // yarn.lock has a custom format - skip parsing here
                // The marker will still be updated with package.json info
                lockContent = undefined;
            } else {
                // npm (package-lock.json) and bun (bun.lock) use JSON
                lockContent = JSON.parse(updatedLockFile);
            }
        } catch {
            // Continue without lock file content
        }
    }

    // Read npmrc configs from the project directory
    const npmrcConfigs = await readNpmrcConfigs(updateInfo.projectDir);

    // Create new marker
    const newMarker = createNodeResolutionResultMarker(
        existingMarker.path,
        packageJsonContent,
        lockContent,
        existingMarker.workspacePackagePaths,
        existingMarker.packageManager,
        npmrcConfigs.length > 0 ? npmrcConfigs : undefined
    );

    // Replace the marker in the document
    return {
        ...doc,
        markers: replaceMarkerByKind(doc.markers, newMarker)
    };
}

/**
 * Options for running install in a temporary directory.
 */
export interface TempInstallOptions {
    /** Timeout in milliseconds (default: 120000 = 2 minutes) */
    timeout?: number;
    /**
     * If true, only update the lock file without installing node_modules.
     * If false, perform a full install which creates node_modules in the temp dir.
     * Default: true (lock-only is faster and sufficient for most cases)
     */
    lockOnly?: boolean;
}

/**
 * Runs package manager install in a temporary directory.
 *
 * This function:
 * 1. Creates a temp directory
 * 2. Writes the provided package.json content
 * 3. Copies the existing lock file (if present)
 * 4. Copies config files (.npmrc, .yarnrc, etc.)
 * 5. Runs the package manager install
 * 6. Returns the updated lock file content
 * 7. Cleans up the temp directory
 *
 * @param projectDir The original project directory (for copying lock file and configs)
 * @param pm The package manager to use
 * @param modifiedPackageJson The modified package.json content to use
 * @param options Optional settings for timeout and lock-only mode
 * @returns Result containing success status and lock file content or error
 */
export async function runInstallInTempDir(
    projectDir: string,
    pm: PackageManager,
    modifiedPackageJson: string,
    options: TempInstallOptions = {}
): Promise<TempInstallResult> {
    const {timeout = 120000, lockOnly = true} = options;
    const lockFileName = getLockFileName(pm);
    const tempDir = await fsp.mkdtemp(path.join(os.tmpdir(), 'openrewrite-pm-'));

    try {
        // Write modified package.json to temp directory
        await fsp.writeFile(path.join(tempDir, 'package.json'), modifiedPackageJson);

        // Copy existing lock file if present
        const originalLockPath = path.join(projectDir, lockFileName);
        if (fs.existsSync(originalLockPath)) {
            await fsp.copyFile(originalLockPath, path.join(tempDir, lockFileName));
        }

        // Copy config files if present (for registry configuration and workspace setup)
        const configFiles = ['.npmrc', '.yarnrc', '.yarnrc.yml', '.pnpmfile.cjs', 'pnpm-workspace.yaml'];
        for (const configFile of configFiles) {
            const configPath = path.join(projectDir, configFile);
            if (fs.existsSync(configPath)) {
                await fsp.copyFile(configPath, path.join(tempDir, configFile));
            }
        }

        // Run package manager install
        const result = runInstall(pm, {
            cwd: tempDir,
            lockOnly,
            timeout
        });

        if (!result.success) {
            // Combine error message with stderr for more useful diagnostics
            const errorParts: string[] = [];
            if (result.error) {
                errorParts.push(result.error);
            }
            if (result.stderr) {
                // Trim and limit stderr to avoid excessively long error messages
                const stderr = result.stderr.trim();
                if (stderr) {
                    errorParts.push(stderr.length > 2000 ? stderr.slice(0, 2000) + '...' : stderr);
                }
            }
            return {
                success: false,
                error: errorParts.length > 0 ? errorParts.join('\n\n') : 'Unknown error'
            };
        }

        // Read back the updated lock file
        const updatedLockPath = path.join(tempDir, lockFileName);
        let lockFileContent: string | undefined;
        if (fs.existsSync(updatedLockPath)) {
            lockFileContent = await fsp.readFile(updatedLockPath, 'utf-8');
        }

        return {
            success: true,
            lockFileContent
        };

    } finally {
        // Cleanup temp directory
        try {
            await fsp.rm(tempDir, {recursive: true, force: true});
        } catch {
            // Ignore cleanup errors
        }
    }
}

/**
 * Creates a lock file visitor that handles updating YAML lock files (pnpm-lock.yaml).
 * This is a reusable component for dependency recipes.
 *
 * @param acc The recipe accumulator containing updated lock file content
 * @returns A YamlVisitor that updates YAML lock files
 */
export function createYamlLockFileVisitor<T>(
    acc: DependencyRecipeAccumulator<T>
): YamlVisitor<ExecutionContext> {
    return new class extends YamlVisitor<ExecutionContext> {
        protected async visitDocuments(docs: Yaml.Documents, _ctx: ExecutionContext): Promise<Yaml | undefined> {
            const sourcePath = docs.sourcePath;
            const updatedLockContent = getUpdatedLockFileContent(sourcePath, acc);
            if (updatedLockContent) {
                const lockFileName = path.basename(sourcePath);
                return await parseLockFileContent(updatedLockContent, sourcePath, lockFileName) as Yaml.Documents;
            }
            return docs;
        }
    };
}

/**
 * Creates a composite visitor that delegates to the appropriate editor based on tree type.
 * This handles both JSON (package-lock.json, bun.lock) and YAML (pnpm-lock.yaml) lock files.
 *
 * @param jsonEditor The JSON visitor for handling JSON files
 * @param acc The recipe accumulator for YAML lock file handling
 * @returns A TreeVisitor that handles both JSON and YAML files
 */
export function createLockFileEditor<T>(
    jsonEditor: JsonVisitor<ExecutionContext>,
    acc: DependencyRecipeAccumulator<T>
): TreeVisitor<any, ExecutionContext> {
    const yamlEditor = createYamlLockFileVisitor(acc);

    return new class extends TreeVisitor<any, ExecutionContext> {
        async visit(tree: any, ctx: ExecutionContext): Promise<any> {
            if (isDocuments(tree)) {
                return yamlEditor.visit(tree, ctx);
            } else if (tree && tree.kind === Json.Kind.Document) {
                return jsonEditor.visit(tree, ctx);
            }
            return tree;
        }
    };
}
