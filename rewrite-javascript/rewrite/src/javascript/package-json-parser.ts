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
import {createNodeResolutionResultMarker, NodeResolutionResult, PackageManager, readNpmrcConfigs} from "./node-resolution-result";
import * as fs from "fs";
import * as path from "path";
import * as YAML from "yaml";
import {execSync} from "child_process";

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

    constructor(options: PackageJsonParserOptions = {}) {
        super(options);
        this.jsonParser = new JsonParser(options);
        this.skipDependencyResolution = options.skipDependencyResolution ?? false;
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
                    marker = this.createMarker(input, dir);
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
    private createMarker(input: ParserInput, dir: string): NodeResolutionResult | null {
        try {
            const content = parserInputRead(input);
            const packageJson = JSON.parse(content);

            // Determine the relative path for the marker
            const filePath = parserInputFile(input);
            const relativePath = this.relativeTo
                ? path.relative(this.relativeTo, filePath)
                : filePath;

            // Try to read lock file if dependency resolution is not skipped
            // Use relativeTo directory if available (for tests), otherwise use the directory from input path
            let lockContent: any = undefined;
            let packageManager: PackageManager | undefined = undefined;
            if (!this.skipDependencyResolution) {
                const lockDir = this.relativeTo || dir;
                const lockResult = this.tryReadLockFile(lockDir);
                lockContent = lockResult?.content;
                packageManager = lockResult?.packageManager;
            }

            // Read .npmrc configurations from all scopes
            const projectDir = this.relativeTo || dir;
            const npmrcConfigs = readNpmrcConfigs(projectDir);

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
     * Attempts to read and parse a lock file from the given directory.
     * Supports npm (package-lock.json), bun (bun.lock), pnpm, and yarn.
     *
     * @returns Object with parsed lock file content and detected package manager, or undefined if no lock file found
     */
    private tryReadLockFile(dir: string): { content: any; packageManager: PackageManager } | undefined {
        // Try package-lock.json (npm) - already JSON
        const npmLockPath = path.join(dir, 'package-lock.json');
        if (fs.existsSync(npmLockPath)) {
            try {
                const content = fs.readFileSync(npmLockPath, 'utf-8');
                return { content: JSON.parse(content), packageManager: PackageManager.Npm };
            } catch (error) {
                // Silently ignore parse errors
            }
        }

        // Try bun.lock (bun v1.2+) - JSONC format (JSON with comments)
        const bunLockPath = path.join(dir, 'bun.lock');
        if (fs.existsSync(bunLockPath)) {
            try {
                const content = fs.readFileSync(bunLockPath, 'utf-8');
                const bunLock = this.parseJsonc(content);
                return { content: this.convertBunLockToNpmFormat(bunLock), packageManager: PackageManager.Bun };
            } catch (error) {
                // Silently ignore parse errors
            }
        }

        // Try pnpm-lock.yaml (pnpm) - prefer node_modules, fall back to CLI
        const pnpmLockPath = path.join(dir, 'pnpm-lock.yaml');
        if (fs.existsSync(pnpmLockPath)) {
            try {
                // Try node_modules first for 100% accuracy
                const parsed = this.walkNodeModules(dir);
                if (parsed) {
                    return { content: parsed, packageManager: PackageManager.Pnpm };
                }
                // Fall back to pnpm CLI
                const cliParsed = this.getPnpmDependencies(dir);
                if (cliParsed) {
                    return { content: cliParsed, packageManager: PackageManager.Pnpm };
                }
            } catch (error) {
                // Silently ignore errors (CLI not available)
            }
        }

        // Try yarn.lock (yarn) - prefer node_modules, fall back to lock file parsing
        const yarnLockPath = path.join(dir, 'yarn.lock');
        if (fs.existsSync(yarnLockPath)) {
            try {
                const content = fs.readFileSync(yarnLockPath, 'utf-8');
                // Detect yarn version for packageManager field
                const isYarnBerry = content.includes('__metadata:');
                const packageManager = isYarnBerry ? PackageManager.YarnBerry : PackageManager.YarnClassic;

                // Try node_modules first for 100% accuracy
                const parsed = this.walkNodeModules(dir);
                if (parsed) {
                    return { content: parsed, packageManager };
                }
                // Fall back to parsing lock file directly
                const lockParsed = this.parseYarnLock(content);
                if (lockParsed) {
                    return { content: lockParsed, packageManager };
                }
            } catch (error) {
                // Silently ignore errors
            }
        }

        return undefined;
    }

    /**
     * Parses JSONC (JSON with Comments and trailing commas) content.
     */
    private parseJsonc(content: string): any {
        // Remove single-line comments (// ...)
        let stripped = content.replace(/\/\/.*$/gm, '');
        // Remove multi-line comments (/* ... */)
        stripped = stripped.replace(/\/\*[\s\S]*?\*\//g, '');
        // Remove trailing commas before ] or }
        stripped = stripped.replace(/,(\s*[}\]])/g, '$1');
        return JSON.parse(stripped);
    }

    /**
     * Walks the node_modules directory to build an npm-format packages structure.
     * This provides 100% accurate resolution for all package managers since it reads
     * the actual installed packages rather than trying to interpret lock file formats.
     *
     * @param dir The project directory containing node_modules
     * @returns npm package-lock.json format with packages map, or undefined if node_modules doesn't exist
     */
    private walkNodeModules(dir: string): any {
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
            this.walkPnpmNodeModules(pnpmPath, packages);
        } else {
            this.walkNodeModulesRecursive(nodeModulesPath, 'node_modules', packages);
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
    private walkPnpmNodeModules(pnpmPath: string, packages: Record<string, any>): void {
        let entries: fs.Dirent[];
        try {
            entries = fs.readdirSync(pnpmPath, { withFileTypes: true });
        } catch {
            return;
        }

        for (const entry of entries) {
            // Skip non-directories and special files
            if (!entry.isDirectory() || entry.name === 'node_modules') {
                continue;
            }

            // Parse name@version from directory name
            // Handle scoped packages: @scope+name@version
            const atIndex = entry.name.lastIndexOf('@');
            if (atIndex <= 0) continue;

            let name = entry.name.substring(0, atIndex);
            const version = entry.name.substring(atIndex + 1);

            // pnpm encodes @ as + in scoped packages: @scope+name -> @scope/name
            if (name.startsWith('@') && name.includes('+')) {
                name = name.replace('+', '/');
            }

            // The actual package is at .pnpm/<name>@<version>/node_modules/<name>/
            const pkgDir = path.join(pnpmPath, entry.name, 'node_modules', name.replace('/', path.sep));
            const packageJsonPath = path.join(pkgDir, 'package.json');

            if (!fs.existsSync(packageJsonPath)) continue;

            let pkgJson: any;
            try {
                const content = fs.readFileSync(packageJsonPath, 'utf-8');
                pkgJson = JSON.parse(content);
            } catch {
                continue;
            }

            // Use name@version as the key for pnpm (flat structure with version)
            const pkgKey = `node_modules/${name}@${version}`;

            const pkgEntry: any = {
                version: pkgJson.version || version,
            };

            // Copy dependency fields
            if (pkgJson.dependencies && Object.keys(pkgJson.dependencies).length > 0) {
                pkgEntry.dependencies = pkgJson.dependencies;
            }
            if (pkgJson.devDependencies && Object.keys(pkgJson.devDependencies).length > 0) {
                pkgEntry.devDependencies = pkgJson.devDependencies;
            }
            if (pkgJson.peerDependencies && Object.keys(pkgJson.peerDependencies).length > 0) {
                pkgEntry.peerDependencies = pkgJson.peerDependencies;
            }
            if (pkgJson.optionalDependencies && Object.keys(pkgJson.optionalDependencies).length > 0) {
                pkgEntry.optionalDependencies = pkgJson.optionalDependencies;
            }
            if (pkgJson.engines) {
                pkgEntry.engines = pkgJson.engines;
            }
            if (pkgJson.license) {
                pkgEntry.license = pkgJson.license;
            }

            packages[pkgKey] = pkgEntry;
        }
    }

    /**
     * Recursively walks a node_modules directory, reading package.json files
     * and building the packages map.
     *
     * @param nodeModulesPath Absolute path to the node_modules directory
     * @param relativePath Relative path from project root (e.g., "node_modules" or "node_modules/foo/node_modules")
     * @param packages The packages map to populate
     */
    private walkNodeModulesRecursive(
        nodeModulesPath: string,
        relativePath: string,
        packages: Record<string, any>
    ): void {
        let entries: fs.Dirent[];
        try {
            entries = fs.readdirSync(nodeModulesPath, { withFileTypes: true });
        } catch {
            return; // Directory not readable
        }

        for (const entry of entries) {
            // Skip hidden files
            if (entry.name.startsWith('.')) {
                continue;
            }

            // Accept directories and symlinks (pnpm uses symlinks)
            const isDirectoryOrSymlink = entry.isDirectory() || entry.isSymbolicLink();
            if (!isDirectoryOrSymlink) {
                continue;
            }

            // Handle scoped packages (@scope/name)
            if (entry.name.startsWith('@')) {
                const scopePath = path.join(nodeModulesPath, entry.name);
                let scopeEntries: fs.Dirent[];
                try {
                    scopeEntries = fs.readdirSync(scopePath, { withFileTypes: true });
                } catch {
                    continue;
                }

                for (const scopeEntry of scopeEntries) {
                    // Accept directories and symlinks for scoped packages too
                    if (!scopeEntry.isDirectory() && !scopeEntry.isSymbolicLink()) continue;

                    const scopedName = `${entry.name}/${scopeEntry.name}`;
                    const pkgPath = path.join(scopePath, scopeEntry.name);
                    this.processPackage(pkgPath, `${relativePath}/${scopedName}`, packages);
                }
            } else {
                const pkgPath = path.join(nodeModulesPath, entry.name);
                this.processPackage(pkgPath, `${relativePath}/${entry.name}`, packages);
            }
        }
    }

    /**
     * Processes a single package directory, reading its package.json and
     * recursively processing nested node_modules.
     */
    private processPackage(
        pkgPath: string,
        relativePath: string,
        packages: Record<string, any>
    ): void {
        const packageJsonPath = path.join(pkgPath, 'package.json');

        // Read and parse the package's package.json
        let pkgJson: any;
        try {
            const content = fs.readFileSync(packageJsonPath, 'utf-8');
            pkgJson = JSON.parse(content);
        } catch {
            return; // Not a valid package
        }

        const pkgEntry: any = {
            version: pkgJson.version,
        };

        // Copy dependency fields if present
        if (pkgJson.dependencies && Object.keys(pkgJson.dependencies).length > 0) {
            pkgEntry.dependencies = pkgJson.dependencies;
        }
        if (pkgJson.devDependencies && Object.keys(pkgJson.devDependencies).length > 0) {
            pkgEntry.devDependencies = pkgJson.devDependencies;
        }
        if (pkgJson.peerDependencies && Object.keys(pkgJson.peerDependencies).length > 0) {
            pkgEntry.peerDependencies = pkgJson.peerDependencies;
        }
        if (pkgJson.optionalDependencies && Object.keys(pkgJson.optionalDependencies).length > 0) {
            pkgEntry.optionalDependencies = pkgJson.optionalDependencies;
        }
        if (pkgJson.engines) {
            pkgEntry.engines = pkgJson.engines;
        }
        if (pkgJson.license) {
            pkgEntry.license = pkgJson.license;
        }

        packages[relativePath] = pkgEntry;

        // Recursively process nested node_modules
        const nestedNodeModules = path.join(pkgPath, 'node_modules');
        if (fs.existsSync(nestedNodeModules)) {
            this.walkNodeModulesRecursive(nestedNodeModules, `${relativePath}/node_modules`, packages);
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
    private convertBunLockToNpmFormat(bunLock: any): any {
        if (!bunLock?.packages) {
            return undefined;
        }

        const packages: Record<string, any> = {
            "": {} // Root package placeholder
        };

        for (const [key, value] of Object.entries(bunLock.packages)) {
            if (!Array.isArray(value)) continue;

            // bun.lock array format: [name@version, url, metadata, integrity]
            const [nameAtVersion, , metadata] = value as any[];

            if (typeof nameAtVersion !== 'string') continue;

            // Parse name@version from first element
            const atIndex = nameAtVersion.lastIndexOf('@');
            if (atIndex <= 0) continue;

            const name = nameAtVersion.substring(0, atIndex);
            const version = nameAtVersion.substring(atIndex + 1);

            const pkgEntry: any = {
                version,
            };

            // bun.lock metadata object has dependencies, devDependencies, etc as direct properties
            if (metadata && typeof metadata === 'object') {
                if (metadata.dependencies && Object.keys(metadata.dependencies).length > 0) {
                    pkgEntry.dependencies = metadata.dependencies;
                }
                if (metadata.devDependencies && Object.keys(metadata.devDependencies).length > 0) {
                    pkgEntry.devDependencies = metadata.devDependencies;
                }
                if (metadata.peerDependencies && Object.keys(metadata.peerDependencies).length > 0) {
                    pkgEntry.peerDependencies = metadata.peerDependencies;
                }
                if (metadata.optionalDependencies && Object.keys(metadata.optionalDependencies).length > 0) {
                    pkgEntry.optionalDependencies = metadata.optionalDependencies;
                }
            }

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
    private getPnpmDependencies(dir: string): any {
        const output = execSync('pnpm list --json --depth=Infinity', {
            cwd: dir,
            encoding: 'utf-8',
            stdio: ['pipe', 'pipe', 'pipe'],
            timeout: 30000
        });

        const pnpmList = JSON.parse(output);
        return this.convertPnpmListToNpmFormat(pnpmList);
    }

    /**
     * Converts pnpm list --json output to npm package-lock.json format.
     */
    private convertPnpmListToNpmFormat(pnpmList: any): any {
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
