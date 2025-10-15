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
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import * as crypto from 'crypto';
import {execSync} from 'child_process';

/**
 * Manages workspace directories for TypeScript compilation with dependencies.
 * Creates temporary workspaces with package.json and installed node_modules
 * to enable proper type attribution for templates.
 */
export class DependencyWorkspace {
    private static readonly WORKSPACE_BASE = path.join(os.tmpdir(), 'openrewrite-js-workspaces');
    private static readonly cache = new Map<string, string>();

    /**
     * Gets or creates a workspace directory for the given dependencies.
     * Workspaces are cached by dependency hash to avoid repeated npm installs.
     *
     * @param dependencies NPM dependencies (package name to version mapping)
     * @param targetDir Optional target directory. If provided, creates workspace in this directory
     *                  instead of a hash-based temp directory. Caller is responsible for directory lifecycle.
     * @returns Path to the workspace directory
     */
    static async getOrCreateWorkspace(dependencies: Record<string, string>, targetDir?: string): Promise<string> {
        if (targetDir) {
            // Use provided directory - check if it's already valid
            if (this.isWorkspaceValid(targetDir, dependencies)) {
                return targetDir;
            }

            // Create/update workspace in target directory
            fs.mkdirSync(targetDir, {recursive: true});

            try {
                const packageJson = {
                    name: "openrewrite-template-workspace",
                    version: "1.0.0",
                    private: true,
                    dependencies: dependencies
                };

                fs.writeFileSync(
                    path.join(targetDir, 'package.json'),
                    JSON.stringify(packageJson, null, 2)
                );

                // Run npm install
                execSync('npm install --silent', {
                    cwd: targetDir,
                    stdio: 'pipe' // Suppress output
                });

                return targetDir;
            } catch (error) {
                throw new Error(`Failed to create dependency workspace: ${error}`);
            }
        }

        // Use hash-based cached workspace
        const hash = this.hashDependencies(dependencies);

        // Check cache
        const cached = this.cache.get(hash);
        if (cached && fs.existsSync(cached) && this.isWorkspaceValid(cached, dependencies)) {
            return cached;
        }

        // Create new workspace
        const workspaceDir = path.join(this.WORKSPACE_BASE, hash);

        // Ensure base directory exists
        if (!fs.existsSync(this.WORKSPACE_BASE)) {
            fs.mkdirSync(this.WORKSPACE_BASE, {recursive: true});
        }

        // Remove existing workspace if it exists
        if (fs.existsSync(workspaceDir)) {
            fs.rmSync(workspaceDir, {recursive: true, force: true});
        }

        // Create workspace directory
        fs.mkdirSync(workspaceDir, {recursive: true});

        try {
            // Create package.json
            const packageJson = {
                name: "openrewrite-template-workspace",
                version: "1.0.0",
                private: true,
                dependencies: dependencies
            };

            fs.writeFileSync(
                path.join(workspaceDir, 'package.json'),
                JSON.stringify(packageJson, null, 2)
            );

            // Run npm install
            execSync('npm install --silent', {
                cwd: workspaceDir,
                stdio: 'pipe' // Suppress output
            });

            // Cache the workspace
            this.cache.set(hash, workspaceDir);

            return workspaceDir;
        } catch (error) {
            // Clean up on failure
            if (fs.existsSync(workspaceDir)) {
                fs.rmSync(workspaceDir, {recursive: true, force: true});
            }
            throw new Error(`Failed to create dependency workspace: ${error}`);
        }
    }

    /**
     * Generates a hash from dependencies for caching.
     */
    private static hashDependencies(dependencies: Record<string, string>): string {
        // Sort keys for consistent hashing
        const sorted = Object.keys(dependencies).sort();
        const content = sorted.map(key => `${key}:${dependencies[key]}`).join(',');
        return crypto.createHash('sha256').update(content).digest('hex').substring(0, 16);
    }

    /**
     * Checks if a workspace is valid (has node_modules and matching package.json).
     *
     * @param workspaceDir Directory to check
     * @param expectedDependencies Optional dependencies to check against package.json
     */
    private static isWorkspaceValid(workspaceDir: string, expectedDependencies?: Record<string, string>): boolean {
        const nodeModules = path.join(workspaceDir, 'node_modules');
        const packageJsonPath = path.join(workspaceDir, 'package.json');

        if (!fs.existsSync(nodeModules) || !fs.existsSync(packageJsonPath)) {
            return false;
        }

        // If dependencies provided, check if they match
        if (expectedDependencies) {
            try {
                const packageJsonContent = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
                const existingDeps = packageJsonContent.dependencies || {};

                // Check if all expected dependencies match
                const expectedKeys = Object.keys(expectedDependencies).sort();
                const existingKeys = Object.keys(existingDeps).sort();

                if (expectedKeys.length !== existingKeys.length) {
                    return false;
                }

                for (let i = 0; i < expectedKeys.length; i++) {
                    if (expectedKeys[i] !== existingKeys[i] ||
                        expectedDependencies[expectedKeys[i]] !== existingDeps[existingKeys[i]]) {
                        return false;
                    }
                }
            } catch (error) {
                return false;
            }
        }

        return true;
    }

    /**
     * Cleans up old workspace directories.
     * Removes workspaces older than the specified age.
     *
     * @param maxAgeMs Maximum age in milliseconds (default: 24 hours)
     */
    static cleanupOldWorkspaces(maxAgeMs: number = 24 * 60 * 60 * 1000): void {
        if (!fs.existsSync(this.WORKSPACE_BASE)) {
            return;
        }

        const now = Date.now();
        const entries = fs.readdirSync(this.WORKSPACE_BASE, {withFileTypes: true});

        for (const entry of entries) {
            if (!entry.isDirectory()) {
                continue;
            }

            const workspaceDir = path.join(this.WORKSPACE_BASE, entry.name);
            try {
                const stats = fs.statSync(workspaceDir);
                const age = now - stats.mtimeMs;

                if (age > maxAgeMs) {
                    fs.rmSync(workspaceDir, {recursive: true, force: true});
                    // Remove from cache
                    for (const [hash, dir] of this.cache.entries()) {
                        if (dir === workspaceDir) {
                            this.cache.delete(hash);
                            break;
                        }
                    }
                }
            } catch (error) {
                // Ignore errors, workspace might be in use
            }
        }
    }

    /**
     * Clears all cached workspaces.
     */
    static clearCache(): void {
        this.cache.clear();
    }
}
