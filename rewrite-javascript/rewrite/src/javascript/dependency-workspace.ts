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

        // Final workspace location
        const workspaceDir = path.join(this.WORKSPACE_BASE, hash);

        // Check if valid workspace already exists on disk (cross-VM reuse)
        if (fs.existsSync(workspaceDir) && this.isWorkspaceValid(workspaceDir, dependencies)) {
            this.cache.set(hash, workspaceDir);
            return workspaceDir;
        }

        // Ensure base directory exists
        if (!fs.existsSync(this.WORKSPACE_BASE)) {
            fs.mkdirSync(this.WORKSPACE_BASE, {recursive: true});
        }

        // Create workspace in temporary location to ensure atomicity
        // This prevents reusing partially created workspaces from crashes
        // and handles concurrency with other Node processes
        const tempSuffix = `.tmp-${process.pid}-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
        const tempWorkspaceDir = path.join(this.WORKSPACE_BASE, hash + tempSuffix);

        try {
            // Create temporary workspace directory
            fs.mkdirSync(tempWorkspaceDir, {recursive: true});

            // Create package.json
            const packageJson = {
                name: "openrewrite-template-workspace",
                version: "1.0.0",
                private: true,
                dependencies: dependencies
            };

            fs.writeFileSync(
                path.join(tempWorkspaceDir, 'package.json'),
                JSON.stringify(packageJson, null, 2)
            );

            // Run npm install
            execSync('npm install --silent', {
                cwd: tempWorkspaceDir,
                stdio: 'pipe' // Suppress output
            });

            // Atomically move to final location with retry logic for concurrency
            let moved = false;
            let retries = 3;

            while (!moved && retries > 0) {
                try {
                    // Attempt atomic rename (works on POSIX, fails on Windows if target exists)
                    fs.renameSync(tempWorkspaceDir, workspaceDir);
                    moved = true;
                } catch (error: any) {
                    // Handle concurrent creation by another process
                    if (error.code === 'EEXIST' || error.code === 'ENOTEMPTY' || error.code === 'EISDIR' ||
                        (error.code === 'EPERM' && fs.existsSync(workspaceDir))) {
                        // Target exists - check if it's valid
                        if (this.isWorkspaceValid(workspaceDir, dependencies)) {
                            // Another process created a valid workspace - use theirs
                            moved = true; // Don't try again
                        } else {
                            // Invalid workspace exists - try to remove and retry
                            try {
                                fs.rmSync(workspaceDir, {recursive: true, force: true});
                                retries--;
                            } catch (removeError) {
                                // Another process might be using it, give up
                                retries = 0;
                            }
                        }
                    } else if (error.code === 'EXDEV') {
                        // Cross-device link - fallback to copy+remove (not atomic, but rare)
                        try {
                            fs.cpSync(tempWorkspaceDir, workspaceDir, {recursive: true});
                            moved = true;
                        } catch (copyError) {
                            // Check if another process created it while we were copying
                            if (this.isWorkspaceValid(workspaceDir, dependencies)) {
                                moved = true;
                            } else {
                                throw error;
                            }
                        }
                    } else {
                        // Unexpected error
                        throw error;
                    }
                }
            }

            // Clean up temp directory
            try {
                if (fs.existsSync(tempWorkspaceDir)) {
                    fs.rmSync(tempWorkspaceDir, {recursive: true, force: true});
                }
            } catch {
                // Ignore cleanup errors
            }

            // Verify final workspace is valid (might be from another process)
            if (!this.isWorkspaceValid(workspaceDir, dependencies)) {
                throw new Error('Failed to create valid workspace due to concurrent modifications');
            }

            // Cache the workspace
            this.cache.set(hash, workspaceDir);

            return workspaceDir;
        } catch (error) {
            // Clean up temporary workspace on failure
            try {
                if (fs.existsSync(tempWorkspaceDir)) {
                    fs.rmSync(tempWorkspaceDir, {recursive: true, force: true});
                }
            } catch {
                // Ignore cleanup errors
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
     * Also removes all temporary directories (*.tmp-*) regardless of age,
     * as these indicate incomplete/crashed operations.
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

            // Always clean up temporary directories (incomplete operations)
            if (entry.name.includes('.tmp-')) {
                try {
                    fs.rmSync(workspaceDir, {recursive: true, force: true});
                } catch (error) {
                    // Ignore errors, might be in use by another process
                }
                continue;
            }

            // Clean up old regular workspaces
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
