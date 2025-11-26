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
import {createNodeResolutionResultMarker, NodeResolutionResult, PackageManager} from "./node-resolution-result";
import * as fs from "fs";
import * as path from "path";
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

            return createNodeResolutionResultMarker(relativePath, packageJson, lockContent, undefined, packageManager);
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

        // Try pnpm-lock.yaml (pnpm) - use CLI for JSON output
        const pnpmLockPath = path.join(dir, 'pnpm-lock.yaml');
        if (fs.existsSync(pnpmLockPath)) {
            try {
                const content = this.getPnpmDependencies(dir);
                if (content) {
                    return { content, packageManager: PackageManager.Pnpm };
                }
            } catch (error) {
                // Silently ignore errors
            }
        }

        // Try yarn.lock (yarn) - use CLI for JSON output
        const yarnLockPath = path.join(dir, 'yarn.lock');
        if (fs.existsSync(yarnLockPath)) {
            try {
                const result = this.getYarnDependencies(dir);
                if (result) {
                    return result;
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
        try {
            const output = execSync('pnpm list --json --depth=Infinity', {
                cwd: dir,
                encoding: 'utf-8',
                stdio: ['pipe', 'pipe', 'pipe'],
                timeout: 30000
            });

            const pnpmList = JSON.parse(output);
            return this.convertPnpmListToNpmFormat(pnpmList);
        } catch (error) {
            return undefined;
        }
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

        return {
            lockfileVersion: 3,
            packages
        };
    }

    /**
     * Recursively extracts dependencies from pnpm list output.
     * Uses name@version as key to handle multiple versions of the same package.
     */
    private extractPnpmDependencies(deps: any, packages: Record<string, any>): void {
        if (!deps) return;

        for (const [name, info] of Object.entries(deps as Record<string, any>)) {
            const version = info.version;
            const pkgKey = `node_modules/${name}@${version}`;

            if (!packages[pkgKey]) {
                const pkgEntry: any = { version };

                // Extract nested dependency version constraints
                if (info.dependencies) {
                    const nestedDeps: Record<string, string> = {};
                    for (const [depName, depInfo] of Object.entries(info.dependencies as Record<string, any>)) {
                        nestedDeps[depName] = (depInfo as any).from || '*';
                        // Recursively add to packages
                        this.extractPnpmDependencies({[depName]: depInfo}, packages);
                    }
                    pkgEntry.dependencies = nestedDeps;
                }

                packages[pkgKey] = pkgEntry;
            }
        }
    }

    /**
     * Gets dependency information from yarn using its CLI.
     * For Yarn Classic (v1), uses `yarn list --json`.
     * For Yarn Berry (v2+), uses `yarn info --all --json`.
     * Returns both the content and which type of yarn was detected.
     */
    private getYarnDependencies(dir: string): { content: any; packageManager: PackageManager } | undefined {
        // Try Yarn Berry first (v2+)
        try {
            const output = execSync('yarn info --all --recursive --json', {
                cwd: dir,
                encoding: 'utf-8',
                stdio: ['pipe', 'pipe', 'pipe'],
                timeout: 30000
            });
            const result = this.convertYarnBerryOutputToNpmFormat(output);
            if (result) return { content: result, packageManager: PackageManager.YarnBerry };
        } catch {
            // Yarn Berry command failed, try Yarn Classic
        }

        // Try Yarn Classic (v1)
        try {
            const output = execSync('yarn list --json', {
                cwd: dir,
                encoding: 'utf-8',
                stdio: ['pipe', 'pipe', 'pipe'],
                timeout: 30000
            });
            const content = this.convertYarnClassicOutputToNpmFormat(output);
            if (content) return { content, packageManager: PackageManager.YarnClassic };
        } catch {
            // Yarn Classic command failed
        }
        return undefined;
    }

    /**
     * Converts Yarn Berry (v2+) output to npm package-lock.json format.
     * Input format: newline-delimited JSON with entries like:
     * {"value":"is-odd@npm:3.0.1","children":{"Version":"3.0.1","Dependencies":[{"descriptor":"is-number@npm:^6.0.0","locator":"is-number@npm:6.0.0"}]}}
     */
    private convertYarnBerryOutputToNpmFormat(output: string): any {
        const packages: Record<string, any> = {
            "": {} // Root package placeholder
        };

        // Yarn Berry outputs newline-delimited JSON
        const lines = output.trim().split('\n');

        for (const line of lines) {
            if (!line.trim()) continue;

            try {
                const entry = JSON.parse(line);

                // Yarn Berry format: { value: "pkg@npm:version", children: { Version: "x.y.z", Dependencies: [...] } }
                if (entry.value && entry.children?.Version) {
                    // Skip workspace entries
                    if (entry.value.includes('@workspace:')) continue;

                    // Parse name from value like "is-odd@npm:3.0.1"
                    const npmIndex = entry.value.indexOf('@npm:');
                    if (npmIndex <= 0) continue;

                    const name = entry.value.substring(0, npmIndex);
                    const version = entry.children.Version;
                    const pkgKey = `node_modules/${name}@${version}`;

                    const pkgEntry: any = {
                        version,
                        license: entry.children.License,
                    };

                    // Parse dependencies from Dependencies array
                    if (entry.children.Dependencies && Array.isArray(entry.children.Dependencies)) {
                        const deps: Record<string, string> = {};
                        for (const dep of entry.children.Dependencies) {
                            // descriptor: "is-number@npm:^6.0.0"
                            if (dep.descriptor) {
                                const depNpmIndex = dep.descriptor.indexOf('@npm:');
                                if (depNpmIndex > 0) {
                                    const depName = dep.descriptor.substring(0, depNpmIndex);
                                    const depConstraint = dep.descriptor.substring(depNpmIndex + 5);
                                    deps[depName] = depConstraint;
                                }
                            }
                        }
                        if (Object.keys(deps).length > 0) {
                            pkgEntry.dependencies = deps;
                        }
                    }

                    packages[pkgKey] = pkgEntry;
                }
            } catch {
                // Skip unparseable lines
            }
        }

        return Object.keys(packages).length > 1 ? {
            lockfileVersion: 3,
            packages
        } : undefined;
    }

    /**
     * Converts Yarn Classic (v1) output to npm package-lock.json format.
     * Recursively extracts packages and their dependencies from the tree structure.
     */
    private convertYarnClassicOutputToNpmFormat(output: string): any {
        const packages: Record<string, any> = {
            "": {} // Root package placeholder
        };

        try {
            const entry = JSON.parse(output);

            // Yarn Classic format: { type: "tree", data: { trees: [...] } }
            if (entry.type === 'tree' && entry.data?.trees) {
                this.extractYarnClassicDependencies(entry.data.trees, packages);
            }
        } catch {
            // Parse error
        }

        return Object.keys(packages).length > 1 ? {
            lockfileVersion: 3,
            packages
        } : undefined;
    }

    /**
     * Recursively extracts dependencies from yarn classic tree structure.
     */
    private extractYarnClassicDependencies(trees: any[], packages: Record<string, any>): void {
        for (const tree of trees) {
            // Skip shadow entries (references to dependencies)
            if (tree.shadow) continue;

            const match = tree.name?.match(/^(.+)@(.+)$/);
            if (match) {
                const [, name, version] = match;
                const pkgKey = `node_modules/${name}@${version}`;

                if (!packages[pkgKey]) {
                    const pkgEntry: any = { version };

                    // Extract children as dependencies (only non-shadow ones that have version)
                    if (tree.children && tree.children.length > 0) {
                        const deps: Record<string, string> = {};
                        for (const child of tree.children) {
                            if (child.shadow) {
                                // Shadow entries show the version constraint
                                const childMatch = child.name?.match(/^(.+)@(.+)$/);
                                if (childMatch) {
                                    deps[childMatch[1]] = childMatch[2];
                                }
                            }
                        }
                        if (Object.keys(deps).length > 0) {
                            pkgEntry.dependencies = deps;
                        }

                        // Recursively process non-shadow children
                        this.extractYarnClassicDependencies(tree.children, packages);
                    }

                    packages[pkgKey] = pkgEntry;
                }
            }
        }
    }
}

// Register with the Parsers registry for RPC support
Parsers.registerParser("packageJson", PackageJsonParser);
