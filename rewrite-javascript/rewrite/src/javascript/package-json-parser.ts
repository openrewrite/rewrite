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
import {Parser, ParserInput, parserInputFile, parserInputRead, ParserOptions, Parsers} from "../parser";
import {SourceFile} from "../tree";
import {Json, JsonParser} from "../json";
import {
    createNodeResolutionResultMarker,
    NodeResolutionResult,
    PackageLockContent,
    PackageLockEntry,
    PackageManager,
    readNpmrcConfigs
} from "./node-resolution-result";
import * as fs from "fs";
import * as fsp from "fs/promises";
import * as path from "path";
import * as YAML from "yaml";
import {getLockFileDetectionConfig, runList} from "./package-manager";

/**
 * Bun.lock package entry metadata.
 */
interface BunLockMetadata {
    readonly dependencies?: Record<string, string>;
    readonly devDependencies?: Record<string, string>;
    readonly peerDependencies?: Record<string, string>;
    readonly optionalDependencies?: Record<string, string>;
}

/**
 * Bun.lock package entry: [name@version, url, metadata, integrity]
 * Note: Using unknown for first element since we need runtime validation of parsed JSON.
 */
type BunLockPackageEntry = [unknown, string?, BunLockMetadata?, string?];

/**
 * Parsed bun.lock content structure.
 */
interface BunLockContent {
    readonly packages?: Record<string, BunLockPackageEntry>;
}

export interface PackageJsonParserOptions extends ParserOptions {
    /**
     * If true, skips reading and parsing lock files for dependency resolution.
     * The NodeResolutionResult marker will still be created, but without resolved dependencies.
     */
    skipDependencyResolution?: boolean;
}

/**
 * A parser for package.json files that wraps the JsonParser.
 *
 * Similar to how MavenParser wraps XmlParser in Java, this parser:
 * - Parses package.json files as JSON documents
 * - Attaches NodeResolutionResult markers with dependency information
 * - Optionally reads corresponding lock files (package-lock.json, yarn.lock, etc.)
 *   to provide resolved dependency versions
 */
export class PackageJsonParser extends Parser {
    private readonly jsonParser: JsonParser;
    private readonly skipDependencyResolution: boolean;

    /** Fields to copy from package.json that contain dependency maps */
    private static readonly DEPENDENCY_FIELDS = [
        'dependencies',
        'devDependencies',
        'peerDependencies',
        'optionalDependencies'
    ] as const;

    constructor(options: PackageJsonParserOptions = {}) {
        super(options);
        this.jsonParser = new JsonParser(options);
        this.skipDependencyResolution = options.skipDependencyResolution ?? false;
    }

    /**
     * Extracts package metadata from a package.json object into a lock file entry format.
     * Copies version, dependency fields, engines, and license.
     */
    private static extractPackageMetadata(pkgJson: any, fallbackVersion?: string): Record<string, any> {
        const entry: Record<string, any> = {
            version: pkgJson.version || fallbackVersion,
        };

        for (const field of PackageJsonParser.DEPENDENCY_FIELDS) {
            if (pkgJson[field] && Object.keys(pkgJson[field]).length > 0) {
                entry[field] = pkgJson[field];
            }
        }

        if (pkgJson.engines) {
            entry.engines = pkgJson.engines;
        }
        if (pkgJson.license) {
            // Normalize legacy license formats to string
            entry.license = PackageJsonParser.normalizeLicense(pkgJson.license);
        }

        return entry;
    }

    /**
     * Normalizes the license field from package.json.
     * Older packages may have license in legacy formats:
     * - Array: ["MIT", "Apache2"] -> "(MIT OR Apache2)"
     * - Object: { type: "MIT", url: "..." } -> "MIT"
     */
    private static normalizeLicense(license: any): string | undefined {
        if (!license) return undefined;
        if (Array.isArray(license)) {
            return license.length > 0 ? `(${license.join(' OR ')})` : undefined;
        }
        if (typeof license === 'object' && license.type) {
            return license.type;
        }
        return typeof license === 'string' ? license : undefined;
    }

    /**
     * Accepts package.json files.
     */
    accept(sourcePath: string): boolean {
        const fileName = path.basename(sourcePath);
        return fileName === 'package.json';
    }

    async *parse(...inputs: ParserInput[]): AsyncGenerator<SourceFile> {
        // Group inputs by directory to share NodeResolutionResult markers
        const inputsByDir = new Map<string, ParserInput[]>();

        for (const input of inputs) {
            const filePath = parserInputFile(input);
            const dir = path.dirname(filePath);

            if (!inputsByDir.has(dir)) {
                inputsByDir.set(dir, []);
            }
            inputsByDir.get(dir)!.push(input);
        }

        // Process each directory's package.json files
        for (const [dir, dirInputs] of inputsByDir) {
            // Create a shared marker for this directory
            let marker: NodeResolutionResult | null = null;

            for (const input of dirInputs) {
                // Parse as JSON first
                const jsonGenerator = this.jsonParser.parse(input);
                const jsonResult = await jsonGenerator.next();

                if (jsonResult.done || !jsonResult.value) {
                    continue;
                }

                const jsonDoc = jsonResult.value as Json.Document;

                // Create NodeResolutionResult marker if not already created for this directory
                if (!marker) {
                    marker = await this.createMarker(input, dir);
                }

                // Attach the marker to the JSON document
                if (marker) {
                    yield {
                        ...jsonDoc,
                        markers: {
                            ...jsonDoc.markers,
                            markers: [...jsonDoc.markers.markers, marker]
                        }
                    };
                } else {
                    yield jsonDoc;
                }
            }
        }
    }

    /**
     * Creates a NodeResolutionResult marker from the package.json content and optional lock file.
     */
    private async createMarker(input: ParserInput, dir: string): Promise<NodeResolutionResult | null> {
        try {
            const content = parserInputRead(input);
            const packageJson = JSON.parse(content);

            // Determine the relative path for the marker
            const filePath = parserInputFile(input);
            const relativePath = this.relativeTo
                ? path.relative(this.relativeTo, filePath)
                : filePath;

            // Try to read lock file if dependency resolution is not skipped
            // First try the directory containing the package.json, then walk up toward relativeTo
            let lockContent: PackageLockContent | undefined = undefined;
            let packageManager: PackageManager | undefined = undefined;
            if (!this.skipDependencyResolution) {
                // Resolve dir relative to relativeTo if dir is relative (e.g., '.' when package.json is at root)
                const absoluteDir = this.relativeTo && !path.isAbsolute(dir)
                    ? path.resolve(this.relativeTo, dir)
                    : dir;
                const lockResult = await this.tryReadLockFileWithWalkUp(absoluteDir, this.relativeTo);
                lockContent = lockResult?.content;
                packageManager = lockResult?.packageManager;
            }

            // Read .npmrc configurations from all scopes
            const projectDir = this.relativeTo || dir;
            const npmrcConfigs = await readNpmrcConfigs(projectDir);

            return createNodeResolutionResultMarker(
                relativePath,
                packageJson,
                lockContent,
                undefined,
                packageManager,
                npmrcConfigs.length > 0 ? npmrcConfigs : undefined
            );
        } catch (error) {
            console.warn(`Failed to create NodeResolutionResult marker: ${error}`);
            return null;
        }
    }

    /**
     * Attempts to find and read a lock file by walking up the directory tree.
     * Starts from the directory containing the package.json and walks up toward
     * the root directory (or relativeTo if specified).
     *
     * This handles both standalone projects (lock file next to package.json) and
     * workspace scenarios (lock file at workspace root).
     *
     * @param startDir The directory containing the package.json being parsed
     * @param rootDir Optional root directory to stop walking at (e.g., relativeTo/git root)
     * @returns Object with parsed lock file content and detected package manager, or undefined if none found
     */
    private async tryReadLockFileWithWalkUp(
        startDir: string,
        rootDir?: string
    ): Promise<{ content: PackageLockContent; packageManager: PackageManager } | undefined> {
        // Normalize paths for comparison
        const normalizedRoot = rootDir ? path.resolve(rootDir) : undefined;
        let currentDir = path.resolve(startDir);

        // Walk up the directory tree looking for a lock file
        while (true) {
            const result = await this.tryReadLockFile(currentDir);
            if (result) {
                return result;
            }

            // If we've reached rootDir, stop walking (don't go above it)
            if (normalizedRoot && currentDir === normalizedRoot) {
                break;
            }

            // Check if we've reached the filesystem root
            const parentDir = path.dirname(currentDir);
            if (parentDir === currentDir) {
                break;
            }

            currentDir = parentDir;
        }

        return undefined;
    }

    /**
     * Attempts to read and parse a lock file from the given directory.
     * Supports npm (package-lock.json), bun (bun.lock), pnpm, and yarn.
     *
     * @returns Object with parsed lock file content and detected package manager, or undefined if no lock file found
     */
    private async tryReadLockFile(dir: string): Promise<{ content: PackageLockContent; packageManager: PackageManager } | undefined> {
        // Use shared lock file detection config (first match wins based on priority)
        for (const config of getLockFileDetectionConfig()) {
            const lockPath = path.join(dir, config.filename);
            if (!fs.existsSync(lockPath)) {
                continue;
            }

            try {
                const fileContent = await fsp.readFile(lockPath, 'utf-8');
                const packageManager = typeof config.packageManager === 'function'
                    ? config.packageManager(fileContent)
                    : config.packageManager;

                // For package managers where lock file omits details, prefer node_modules
                if (config.preferNodeModules) {
                    const parsed = await this.walkNodeModules(dir);
                    if (parsed) {
                        return { content: parsed, packageManager };
                    }
                }

                // Parse lock file based on package manager
                const content = await this.parseLockFileContent(config.filename, fileContent, dir);
                if (content) {
                    return { content, packageManager };
                }
            } catch (error) {
                console.debug?.(`Failed to parse ${config.filename}: ${error}`);
            }
        }

        // Fallback: if node_modules exists but no lock file was found (e.g., symlinked from another workspace),
        // walk node_modules to get resolved dependency information
        const parsed = await this.walkNodeModules(dir);
        if (parsed) {
            // Assume npm as the default package manager when only node_modules exists
            return { content: parsed, packageManager: PackageManager.Npm };
        }

        return undefined;
    }

    /**
     * Parses lock file content based on the lock file type.
     */
    private async parseLockFileContent(
        filename: string,
        content: string,
        dir: string
    ): Promise<PackageLockContent | undefined> {
        switch (filename) {
            case 'package-lock.json':
                return JSON.parse(content);
            case 'bun.lock':
                return this.convertBunLockToNpmFormat(this.parseJsonc(content) as BunLockContent);
            case 'pnpm-lock.yaml':
                // Fall back to pnpm CLI when node_modules unavailable
                return this.getPnpmDependencies(dir);
            case 'yarn.lock':
                return this.parseYarnLock(content);
            default:
                return undefined;
        }
    }

    /**
     * Parses JSONC (JSON with Comments and trailing commas) content.
     */
    private parseJsonc(content: string): Record<string, any> {
        const {parse} = require('jsonc-parser');
        return parse(content);
    }

    /**
     * Walks the node_modules directory to build an npm-format packages structure.
     * This provides 100% accurate resolution for all package managers since it reads
     * the actual installed packages rather than trying to interpret lock file formats.
     *
     * @param dir The project directory containing node_modules
     * @returns npm package-lock.json format with packages map, or undefined if node_modules doesn't exist
     */
    private async walkNodeModules(dir: string): Promise<any> {
        const nodeModulesPath = path.join(dir, 'node_modules');
        if (!fs.existsSync(nodeModulesPath)) {
            return undefined;
        }

        const packages: Record<string, any> = {
            "": {} // Root package placeholder
        };

        // Check if this is a pnpm project (has .pnpm directory)
        const pnpmPath = path.join(nodeModulesPath, '.pnpm');
        if (fs.existsSync(pnpmPath)) {
            await this.walkPnpmNodeModules(pnpmPath, packages);
        } else {
            await this.walkNodeModulesRecursive(nodeModulesPath, 'node_modules', packages);
        }

        return Object.keys(packages).length > 1 ? {
            lockfileVersion: 3,
            packages
        } : undefined;
    }

    /**
     * Walks pnpm's .pnpm directory structure to build packages map.
     * pnpm stores packages in .pnpm/<name>@<version>/node_modules/<name>/
     */
    private async walkPnpmNodeModules(pnpmPath: string, packages: Record<string, any>): Promise<void> {
        let entries: fs.Dirent[];
        try {
            entries = await fsp.readdir(pnpmPath, { withFileTypes: true });
        } catch {
            return;
        }

        // Process entries in parallel for better performance
        await Promise.all(entries.map(async (entry) => {
            // Skip non-directories and special files
            if (!entry.isDirectory() || entry.name === 'node_modules') {
                return;
            }

            // Parse name@version from directory name
            // Handle scoped packages: @scope+name@version
            const atIndex = entry.name.lastIndexOf('@');
            if (atIndex <= 0) return;

            let name = entry.name.substring(0, atIndex);
            const version = entry.name.substring(atIndex + 1);

            // pnpm encodes @ as + in scoped packages: @scope+name -> @scope/name
            if (name.startsWith('@') && name.includes('+')) {
                name = name.replace('+', '/');
            }

            // The actual package is at .pnpm/<name>@<version>/node_modules/<name>/
            const pkgDir = path.join(pnpmPath, entry.name, 'node_modules', name.replace('/', path.sep));
            const packageJsonPath = path.join(pkgDir, 'package.json');

            let pkgJson: any;
            try {
                const content = await fsp.readFile(packageJsonPath, 'utf-8');
                pkgJson = JSON.parse(content);
            } catch {
                return;
            }

            // Use name@version as the key for pnpm (flat structure with version)
            const pkgKey = `node_modules/${name}@${version}`;
            packages[pkgKey] = PackageJsonParser.extractPackageMetadata(pkgJson, version);
        }));
    }

    /**
     * Recursively walks a node_modules directory, reading package.json files
     * and building the packages map.
     *
     * @param nodeModulesPath Absolute path to the node_modules directory
     * @param relativePath Relative path from project root (e.g., "node_modules" or "node_modules/foo/node_modules")
     * @param packages The packages map to populate
     */
    private async walkNodeModulesRecursive(
        nodeModulesPath: string,
        relativePath: string,
        packages: Record<string, any>
    ): Promise<void> {
        let entries: fs.Dirent[];
        try {
            entries = await fsp.readdir(nodeModulesPath, { withFileTypes: true });
        } catch {
            return; // Directory not readable
        }

        // Process entries in parallel for better performance
        await Promise.all(entries.map(async (entry) => {
            // Skip hidden files
            if (entry.name.startsWith('.')) {
                return;
            }

            // Accept directories and symlinks (pnpm uses symlinks)
            const isDirectoryOrSymlink = entry.isDirectory() || entry.isSymbolicLink();
            if (!isDirectoryOrSymlink) {
                return;
            }

            // Handle scoped packages (@scope/name)
            if (entry.name.startsWith('@')) {
                const scopePath = path.join(nodeModulesPath, entry.name);
                let scopeEntries: fs.Dirent[];
                try {
                    scopeEntries = await fsp.readdir(scopePath, { withFileTypes: true });
                } catch {
                    return;
                }

                await Promise.all(scopeEntries.map(async (scopeEntry) => {
                    // Accept directories and symlinks for scoped packages too
                    if (!scopeEntry.isDirectory() && !scopeEntry.isSymbolicLink()) return;

                    const scopedName = `${entry.name}/${scopeEntry.name}`;
                    const pkgPath = path.join(scopePath, scopeEntry.name);
                    await this.processPackage(pkgPath, `${relativePath}/${scopedName}`, packages);
                }));
            } else {
                const pkgPath = path.join(nodeModulesPath, entry.name);
                await this.processPackage(pkgPath, `${relativePath}/${entry.name}`, packages);
            }
        }));
    }

    /**
     * Processes a single package directory, reading its package.json and
     * recursively processing nested node_modules.
     */
    private async processPackage(
        pkgPath: string,
        relativePath: string,
        packages: Record<string, any>
    ): Promise<void> {
        const packageJsonPath = path.join(pkgPath, 'package.json');

        // Read and parse the package's package.json
        let pkgJson: any;
        try {
            const content = await fsp.readFile(packageJsonPath, 'utf-8');
            pkgJson = JSON.parse(content);
        } catch {
            return; // Not a valid package
        }

        packages[relativePath] = PackageJsonParser.extractPackageMetadata(pkgJson);

        // Recursively process nested node_modules
        const nestedNodeModules = path.join(pkgPath, 'node_modules');
        try {
            await fsp.access(nestedNodeModules);
            await this.walkNodeModulesRecursive(nestedNodeModules, `${relativePath}/node_modules`, packages);
        } catch {
            // No nested node_modules, that's fine
        }
    }

    /**
     * Converts bun.lock format to npm package-lock.json format for unified processing.
     *
     * bun.lock format (v1):
     * - Keys are package names or paths like "is-even/is-odd" for nested deps
     * - Values are arrays: [name@version, url, metadata, integrity]
     * - metadata can have: { dependencies: {...}, devDependencies: {...}, ... }
     */
    private convertBunLockToNpmFormat(bunLock: BunLockContent): PackageLockContent | undefined {
        if (!bunLock.packages) {
            return undefined;
        }

        const packages: Record<string, PackageLockEntry> = {
            "": {} // Root package placeholder
        };

        for (const [key, value] of Object.entries(bunLock.packages)) {
            // bun.lock array format: [name@version, url, metadata, integrity]
            const [nameAtVersion, , metadata] = value;

            if (typeof nameAtVersion !== 'string') continue;

            // Parse name@version from first element
            const atIndex = nameAtVersion.lastIndexOf('@');
            if (atIndex <= 0) continue;

            const name = nameAtVersion.substring(0, atIndex);
            const version = nameAtVersion.substring(atIndex + 1);

            const pkgEntry: PackageLockEntry = {
                version,
                dependencies: metadata?.dependencies && Object.keys(metadata.dependencies).length > 0
                    ? metadata.dependencies : undefined,
                devDependencies: metadata?.devDependencies && Object.keys(metadata.devDependencies).length > 0
                    ? metadata.devDependencies : undefined,
                peerDependencies: metadata?.peerDependencies && Object.keys(metadata.peerDependencies).length > 0
                    ? metadata.peerDependencies : undefined,
                optionalDependencies: metadata?.optionalDependencies && Object.keys(metadata.optionalDependencies).length > 0
                    ? metadata.optionalDependencies : undefined,
            };

            // Convert bun's path format to npm's node_modules format
            // bun uses "parent/child" for nested deps, npm uses "node_modules/parent/node_modules/child"
            let pkgPath: string;
            if (key.includes('/')) {
                // Nested dependency - convert "is-even/is-odd" to "node_modules/is-even/node_modules/is-odd"
                const parts = key.split('/');
                pkgPath = parts.map(p => `node_modules/${p}`).join('/');
            } else {
                pkgPath = `node_modules/${name}`;
            }

            packages[pkgPath] = pkgEntry;
        }

        return {
            lockfileVersion: 3,
            packages
        };
    }

    /**
     * Gets dependency information from pnpm using its CLI.
     * Uses `pnpm list --json --depth=Infinity` to get the full dependency tree.
     */
    private getPnpmDependencies(dir: string): Record<string, any> | undefined {
        const output = runList(PackageManager.Pnpm, dir, 30000);
        if (!output) {
            return undefined;
        }

        const pnpmList = JSON.parse(output);
        return this.convertPnpmListToNpmFormat(pnpmList);
    }

    /**
     * Converts pnpm list --json output to npm package-lock.json format.
     */
    private convertPnpmListToNpmFormat(pnpmList: any): Record<string, any> | undefined {
        const packages: Record<string, any> = {
            "": {} // Root package placeholder
        };

        // pnpm list returns an array of projects (for workspaces) or a single object
        const projects = Array.isArray(pnpmList) ? pnpmList : [pnpmList];

        for (const project of projects) {
            this.extractPnpmDependencies(project.dependencies, packages);
            this.extractPnpmDependencies(project.devDependencies, packages);
            this.extractPnpmDependencies(project.optionalDependencies, packages);
        }

        return Object.keys(packages).length > 1 ? {
            lockfileVersion: 3,
            packages
        } : undefined;
    }

    /**
     * Recursively extracts dependencies from pnpm list output.
     * Uses name@version as key to handle multiple versions of the same package.
     */
    private extractPnpmDependencies(deps: any, packages: Record<string, any>): void {
        if (!deps) return;

        for (const [name, info] of Object.entries(deps as Record<string, any>)) {
            const version = info.version;
            if (!version) continue;

            const pkgKey = `node_modules/${name}@${version}`;

            if (!packages[pkgKey]) {
                const pkgEntry: any = { version };

                // Extract nested dependency version constraints
                if (info.dependencies) {
                    const nestedDeps: Record<string, string> = {};
                    for (const [depName, depInfo] of Object.entries(info.dependencies as Record<string, any>)) {
                        nestedDeps[depName] = (depInfo as any).version || '*';
                    }
                    if (Object.keys(nestedDeps).length > 0) {
                        pkgEntry.dependencies = nestedDeps;
                    }
                }

                packages[pkgKey] = pkgEntry;
            }

            // Recursively process nested dependencies
            if (info.dependencies) {
                this.extractPnpmDependencies(info.dependencies, packages);
            }
        }
    }

    /**
     * Parses yarn.lock file and returns npm-format content.
     * Detects whether it's Yarn Classic (v1) or Yarn Berry (v2+) format.
     */
    private parseYarnLock(content: string): any {
        // Yarn Berry (v2+) has __metadata section at the start
        if (content.includes('__metadata:')) {
            return this.parseYarnBerryLock(content);
        }

        // Yarn Classic (v1) starts with "# yarn lockfile v1"
        if (content.includes('# yarn lockfile v1')) {
            return this.parseYarnClassicLock(content);
        }

        return undefined;
    }

    /**
     * Parses Yarn Berry (v2+) yarn.lock file directly.
     * Format is standard YAML with package entries like:
     * "is-odd@npm:^3.0.1":
     *   version: 3.0.1
     *   resolution: "is-odd@npm:3.0.1"
     *   dependencies:
     *     is-number: "npm:^6.0.0"
     */
    private parseYarnBerryLock(content: string): any {
        const lock = YAML.parse(content);
        if (!lock) return undefined;

        const packages: Record<string, any> = {
            "": {} // Root package placeholder
        };

        for (const [key, entry] of Object.entries(lock as Record<string, any>)) {
            // Skip metadata and workspace entries
            if (key === '__metadata' || key.includes('@workspace:')) continue;
            if (!entry || typeof entry !== 'object') continue;

            const version = entry.version;
            if (!version) continue;

            // Extract package name from resolution like "is-odd@npm:3.0.1"
            let name: string;
            if (entry.resolution) {
                const npmIndex = entry.resolution.indexOf('@npm:');
                if (npmIndex > 0) {
                    name = entry.resolution.substring(0, npmIndex);
                } else {
                    continue;
                }
            } else {
                // Fallback: parse from key like "is-odd@npm:^3.0.1"
                const npmIndex = key.indexOf('@npm:');
                if (npmIndex > 0) {
                    name = key.substring(0, npmIndex);
                } else {
                    continue;
                }
            }

            const pkgKey = `node_modules/${name}@${version}`;

            // Skip if already processed (multiple version constraints can resolve to same version)
            if (packages[pkgKey]) continue;

            const pkgEntry: any = { version };

            // Parse dependencies
            if (entry.dependencies && typeof entry.dependencies === 'object') {
                const deps: Record<string, string> = {};
                for (const [depName, depConstraint] of Object.entries(entry.dependencies as Record<string, string>)) {
                    // Constraint is like "npm:^6.0.0" - strip the "npm:" prefix
                    deps[depName] = depConstraint.startsWith('npm:')
                        ? depConstraint.substring(4)
                        : depConstraint;
                }
                if (Object.keys(deps).length > 0) {
                    pkgEntry.dependencies = deps;
                }
            }

            packages[pkgKey] = pkgEntry;
        }

        return Object.keys(packages).length > 1 ? {
            lockfileVersion: 3,
            packages
        } : undefined;
    }

    /**
     * Parses Yarn Classic (v1) yarn.lock file directly.
     * Format is a custom format (not standard YAML):
     *
     * is-odd@^3.0.1:
     *   version "3.0.1"
     *   resolved "https://..."
     *   integrity sha512-...
     *   dependencies:
     *     is-number "^6.0.0"
     */
    private parseYarnClassicLock(content: string): any {
        const packages: Record<string, any> = {
            "": {} // Root package placeholder
        };

        // Split into package blocks - each block starts with an unindented line ending with ":"
        // and may span multiple version constraints (e.g., "pkg@^1.0.0, pkg@^1.2.0:")
        const lines = content.split('\n');
        let currentNames: string[] = [];
        let currentVersion: string | null = null;
        let currentDeps: Record<string, string> = {};
        let inDependencies = false;

        for (const line of lines) {
            // Skip comments and empty lines
            if (line.startsWith('#') || line.trim() === '') {
                continue;
            }

            // New package block (unindented line ending with ":")
            if (!line.startsWith(' ') && line.endsWith(':')) {
                // Save previous package if exists
                if (currentNames.length > 0 && currentVersion) {
                    const pkgKey = `node_modules/${currentNames[0]}@${currentVersion}`;
                    if (!packages[pkgKey]) {
                        const pkgEntry: any = { version: currentVersion };
                        if (Object.keys(currentDeps).length > 0) {
                            pkgEntry.dependencies = currentDeps;
                        }
                        packages[pkgKey] = pkgEntry;
                    }
                }

                // Parse new package names from line like 'is-odd@^3.0.1, is-odd@^3.0.0:'
                // or '"@babel/core@^7.0.0":'
                const namesStr = line.slice(0, -1); // Remove trailing ":"
                currentNames = [];

                // Split by ", " but handle quoted strings
                const parts = namesStr.split(/,\s*(?=(?:[^"]*"[^"]*")*[^"]*$)/);
                for (const part of parts) {
                    // Remove surrounding quotes if present
                    let cleaned = part.trim();
                    if (cleaned.startsWith('"') && cleaned.endsWith('"')) {
                        cleaned = cleaned.slice(1, -1);
                    }
                    // Extract package name (everything before last @)
                    const atIndex = cleaned.lastIndexOf('@');
                    if (atIndex > 0) {
                        currentNames.push(cleaned.substring(0, atIndex));
                    }
                }

                currentVersion = null;
                currentDeps = {};
                inDependencies = false;
                continue;
            }

            // Version line: '  version "3.0.1"'
            const versionMatch = line.match(/^\s+version\s+"([^"]+)"/);
            if (versionMatch) {
                currentVersion = versionMatch[1];
                continue;
            }

            // Dependencies section start
            if (line.match(/^\s+dependencies:\s*$/)) {
                inDependencies = true;
                continue;
            }

            // Other section (resolved, integrity, etc.) - ends dependencies section
            if (line.match(/^\s+\w+:/) && !line.match(/^\s{4}/)) {
                inDependencies = false;
                continue;
            }

            // Dependency entry: '    is-number "^6.0.0"'
            if (inDependencies) {
                const depMatch = line.match(/^\s{4}(.+?)\s+"([^"]+)"/);
                if (depMatch) {
                    currentDeps[depMatch[1]] = depMatch[2];
                }
            }
        }

        // Save last package
        if (currentNames.length > 0 && currentVersion) {
            const pkgKey = `node_modules/${currentNames[0]}@${currentVersion}`;
            if (!packages[pkgKey]) {
                const pkgEntry: any = { version: currentVersion };
                if (Object.keys(currentDeps).length > 0) {
                    pkgEntry.dependencies = currentDeps;
                }
                packages[pkgKey] = pkgEntry;
            }
        }

        return Object.keys(packages).length > 1 ? {
            lockfileVersion: 3,
            packages
        } : undefined;
    }
}

// Register with the Parsers registry for RPC support
Parsers.registerParser("packageJson", PackageJsonParser);
