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

interface BaseWorkspaceOptions {
    /**
     * Optional target directory. If provided, creates workspace in this directory
     * instead of a hash-based temp directory. Caller is responsible for directory lifecycle.
     */
    targetDir?: string;
}

interface DependenciesWorkspaceOptions extends BaseWorkspaceOptions {
    /**
     * NPM dependencies (package name to version mapping).
     */
    dependencies: Record<string, string>;
    packageJsonContent?: never;
    packageLockContent?: never;
}

interface PackageJsonWorkspaceOptions extends BaseWorkspaceOptions {
    /**
     * package.json content as a string. Dependencies are extracted from it
     * and the content is written to the workspace.
     */
    packageJsonContent: string;
    dependencies?: never;
    /**
     * Optional package-lock.json content. If provided:
     * - The lock file content is used as the cache key (more precise than dependency hash)
     * - `npm ci` is used instead of `npm install` (faster, deterministic)
     */
    packageLockContent?: string;
    /**
     * Optional workspace member package.json files.
     * Keys are relative paths (e.g., "packages/foo/package.json"), values are content.
     */
    workspacePackages?: Record<string, string>;
}

/**
 * Options for creating a dependency workspace.
 * Provide either `dependencies` or `packageJsonContent`, but not both.
 */
export type WorkspaceOptions = DependenciesWorkspaceOptions | PackageJsonWorkspaceOptions;

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
     * Workspaces are cached by dependency hash (or lock file hash if provided) to avoid repeated npm installs.
     *
     * @param options Workspace options including dependencies or package.json content
     * @returns Path to the workspace directory
     */
    static async getOrCreateWorkspace(options: WorkspaceOptions): Promise<string> {
        // Extract dependencies from package.json content if provided
        let dependencies: Record<string, string> | undefined = options.dependencies;
        let parsedPackageJson: Record<string, any> | undefined;
        let workspacePackages: Record<string, string> | undefined;

        if (options.packageJsonContent) {
            parsedPackageJson = JSON.parse(options.packageJsonContent);
            dependencies = {
                ...parsedPackageJson?.dependencies,
                ...parsedPackageJson?.devDependencies
            };
            workspacePackages = options.workspacePackages;

            // For workspaces, also collect dependencies from workspace members
            if (workspacePackages) {
                for (const content of Object.values(workspacePackages)) {
                    const memberPkg = JSON.parse(content);
                    dependencies = {
                        ...dependencies,
                        ...memberPkg?.dependencies,
                        ...memberPkg?.devDependencies
                    };
                }
            }
        }

        // For workspaces without explicit dependencies in root, we still need to run install
        const hasWorkspaces = parsedPackageJson?.workspaces && Array.isArray(parsedPackageJson.workspaces);
        if ((!dependencies || Object.keys(dependencies).length === 0) && !hasWorkspaces) {
            throw new Error('No dependencies provided');
        }

        // Use the refactored internal method
        return this.createWorkspace(
            dependencies || {},
            parsedPackageJson,
            options.packageJsonContent,
            options.packageLockContent,
            options.targetDir,
            workspacePackages
        );
    }

    /**
     * Internal method that handles workspace creation.
     */
    private static async createWorkspace(
        dependencies: Record<string, string>,
        parsedPackageJson: Record<string, any> | undefined,
        packageJsonContent: string | undefined,
        packageLockContent: string | undefined,
        targetDir: string | undefined,
        workspacePackages?: Record<string, string>
    ): Promise<string> {
        // Determine hash based on lock file (most precise) or dependencies
        // Note: We always hash dependencies (not packageJsonContent) because whitespace/formatting
        // differences in package.json shouldn't create different workspaces
        // For workspaces, include workspace package paths in the hash
        let hash: string;
        if (packageLockContent) {
            hash = this.hashContent(packageLockContent);
        } else if (workspacePackages) {
            // Include workspace package paths in hash for workspace setups
            const workspacePaths = Object.keys(workspacePackages).sort().join(',');
            hash = this.hashContent(this.hashDependencies(dependencies) + ':' + workspacePaths);
        } else {
            hash = this.hashDependencies(dependencies);
        }

        // Determine npm command: use `npm ci` when lock file is provided (faster, deterministic)
        const npmCommand = packageLockContent ? 'npm ci --silent' : 'npm install --silent';

        // Helper to write package files to a directory
        const writePackageFiles = (dir: string) => {
            // Write package.json (use provided content or generate from parsed/dependencies)
            if (packageJsonContent) {
                fs.writeFileSync(path.join(dir, 'package.json'), packageJsonContent);
            } else if (parsedPackageJson) {
                fs.writeFileSync(path.join(dir, 'package.json'), JSON.stringify(parsedPackageJson, null, 2));
            } else {
                const packageJson = {
                    name: "openrewrite-template-workspace",
                    version: "1.0.0",
                    private: true,
                    dependencies: dependencies
                };
                fs.writeFileSync(path.join(dir, 'package.json'), JSON.stringify(packageJson, null, 2));
            }

            // Write package-lock.json if provided
            if (packageLockContent) {
                fs.writeFileSync(path.join(dir, 'package-lock.json'), packageLockContent);
            }

            // Write workspace member package.json files
            if (workspacePackages) {
                for (const [relativePath, content] of Object.entries(workspacePackages)) {
                    const fullPath = path.join(dir, relativePath);
                    const memberDir = path.dirname(fullPath);
                    if (!fs.existsSync(memberDir)) {
                        fs.mkdirSync(memberDir, {recursive: true});
                    }
                    fs.writeFileSync(fullPath, content);
                }
            }
        };

        // For workspaces, skip dependency validation (combined deps don't match root package.json)
        const depsForValidation = workspacePackages ? undefined : dependencies;

        if (targetDir) {
            // Use provided directory - check if it's already valid
            if (this.isWorkspaceValid(targetDir, depsForValidation)) {
                return targetDir;
            }

            // Create/update workspace in target directory
            fs.mkdirSync(targetDir, {recursive: true});

            // Check if we can reuse a cached workspace by symlinking node_modules
            const cachedWorkspaceDir = path.join(this.WORKSPACE_BASE, hash);
            const cachedNodeModules = path.join(cachedWorkspaceDir, 'node_modules');

            if (fs.existsSync(cachedNodeModules) && this.isWorkspaceValid(cachedWorkspaceDir, depsForValidation)) {
                // Symlink node_modules from cached workspace
                try {
                    const targetNodeModules = path.join(targetDir, 'node_modules');

                    // Remove existing node_modules if present (might be invalid)
                    if (fs.existsSync(targetNodeModules)) {
                        fs.rmSync(targetNodeModules, {recursive: true, force: true});
                    }

                    // Create symlink to cached node_modules
                    fs.symlinkSync(cachedNodeModules, targetNodeModules, 'dir');

                    // Write package files
                    writePackageFiles(targetDir);

                    return targetDir;
                } catch (symlinkError) {
                    // Symlink failed (e.g., cross-device, permissions) - fall through to npm install
                }
            }

            try {
                writePackageFiles(targetDir);

                // Run npm install or npm ci
                execSync(npmCommand, {
                    cwd: targetDir,
                    stdio: 'pipe' // Suppress output
                });

                return targetDir;
            } catch (error) {
                throw new Error(`Failed to create dependency workspace: ${error}`);
            }
        }

        // Use hash-based cached workspace

        // Check cache
        const cached = this.cache.get(hash);
        if (cached && fs.existsSync(cached) && this.isWorkspaceValid(cached, depsForValidation)) {
            return cached;
        }

        // Final workspace location
        const workspaceDir = path.join(this.WORKSPACE_BASE, hash);

        // Check if valid workspace already exists on disk (cross-VM reuse)
        if (fs.existsSync(workspaceDir) && this.isWorkspaceValid(workspaceDir, depsForValidation)) {
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

            // Write package files
            writePackageFiles(tempWorkspaceDir);

            // Run npm install or npm ci
            execSync(npmCommand, {
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
                        if (this.isWorkspaceValid(workspaceDir, depsForValidation)) {
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
                            if (this.isWorkspaceValid(workspaceDir, depsForValidation)) {
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
            if (!this.isWorkspaceValid(workspaceDir, depsForValidation)) {
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
        return this.hashContent(content);
    }

    /**
     * Generates a hash from arbitrary content for caching.
     */
    private static hashContent(content: string): string {
        return crypto.createHash('sha256').update(content).digest('hex').substring(0, 16);
    }

    /**
     * Checks if a workspace is valid (has node_modules and matching package.json).
     * Handles both real node_modules directories and symlinks to cached workspaces.
     *
     * @param workspaceDir Directory to check
     * @param expectedDependencies Optional dependencies to check against package.json
     */
    private static isWorkspaceValid(workspaceDir: string, expectedDependencies?: Record<string, string>): boolean {
        const nodeModules = path.join(workspaceDir, 'node_modules');
        const packageJsonPath = path.join(workspaceDir, 'package.json');

        // Check node_modules exists (as directory or symlink)
        if (!fs.existsSync(nodeModules) || !fs.existsSync(packageJsonPath)) {
            return false;
        }

        // If node_modules is a symlink, verify the target still exists
        try {
            const stats = fs.lstatSync(nodeModules);
            if (stats.isSymbolicLink()) {
                const target = fs.readlinkSync(nodeModules);
                const absoluteTarget = path.isAbsolute(target) ? target : path.resolve(path.dirname(nodeModules), target);
                if (!fs.existsSync(absoluteTarget)) {
                    return false;
                }
            }
        } catch {
            return false;
        }

        // If dependencies provided, check if they match
        if (expectedDependencies) {
            try {
                const packageJsonContent = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
                // Merge dependencies and devDependencies (same as getOrCreateWorkspace)
                const existingDeps = {
                    ...packageJsonContent.dependencies,
                    ...packageJsonContent.devDependencies
                };

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
