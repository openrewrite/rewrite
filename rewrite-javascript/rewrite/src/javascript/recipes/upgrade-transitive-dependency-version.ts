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

import {Option, ScanningRecipe} from "../../recipe";
import {ExecutionContext} from "../../execution";
import {TreeVisitor} from "../../visitor";
import {Json, JsonParser, JsonVisitor} from "../../json";
import {
    createNodeResolutionResultMarker,
    findNodeResolutionResult,
    NodeResolutionResultQueries,
    PackageJsonContent,
    PackageLockContent,
    PackageManager,
    readNpmrcConfigs
} from "../node-resolution-result";
import * as path from "path";
import * as semver from "semver";
import {markupWarn, replaceMarkerByKind} from "../../markers";
import {TreePrinters} from "../../print";
import {getAllLockFileNames, getLockFileName, runInstallInTempDir} from "../package-manager";
import {applyOverrideToPackageJson, DependencyPathSegment, parseDependencyPath} from "../dependency-manager";

/**
 * Information about a project that needs updating
 */
interface ProjectUpdateInfo {
    /** Absolute path to the project directory */
    projectDir: string;
    /** Relative path to package.json (from source root) */
    packageJsonPath: string;
    /** Original package.json content */
    originalPackageJson: string;
    /** New version constraint to apply */
    newVersion: string;
    /** The package manager used by this project */
    packageManager: PackageManager;
    /**
     * If true, skip running the package manager because the resolved version
     * already satisfies the new constraint. Only package.json needs updating.
     */
    skipInstall: boolean;
    /** Parsed dependency path for scoped overrides (if specified) */
    dependencyPathSegments?: DependencyPathSegment[];
}

/**
 * Accumulator for tracking state across scanning and editing phases
 */
interface Accumulator {
    /** Projects that need updating: packageJsonPath -> update info */
    projectsToUpdate: Map<string, ProjectUpdateInfo>;

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
 * Upgrades the version of a transitive dependency by adding override entries to package.json.
 *
 * This recipe is used when you need to upgrade a dependency that is not directly declared
 * in your package.json, but is pulled in transitively by one of your direct dependencies.
 * This is commonly needed for security vulnerability remediation.
 *
 * The recipe adds entries to:
 * - `overrides` for npm and Bun
 * - `resolutions` for Yarn (Classic and Berry)
 * - `pnpm.overrides` for pnpm
 *
 * @see UpgradeDependencyVersion for upgrading direct dependencies
 */
export class UpgradeTransitiveDependencyVersion extends ScanningRecipe<Accumulator> {
    readonly name = "org.openrewrite.javascript.dependencies.upgrade-transitive-dependency-version";
    readonly displayName = "Upgrade transitive npm dependency version";
    readonly description = "Upgrades the version of a transitive dependency by adding override/resolution entries to `package.json` and updates the lock file by running the package manager.";

    @Option({
        displayName: "Package name",
        description: "The name of the npm package to upgrade (e.g., 'lodash', '@types/node')",
        example: "lodash"
    })
    packageName!: string;

    @Option({
        displayName: "New version",
        description: "The new version constraint to set (e.g., '^5.0.0', '~2.1.0', '3.0.0')",
        example: "^5.0.0"
    })
    newVersion!: string;

    @Option({
        displayName: "Dependency path",
        description: "Optional path to scope the override to a specific dependency chain. Use '>' as separator (e.g., 'express>accepts'). When not specified, applies globally to all transitive occurrences.",
        required: false,
        example: "express>accepts"
    })
    dependencyPath?: string;

    initialValue(_ctx: ExecutionContext): Accumulator {
        return {
            projectsToUpdate: new Map(),
            updatedLockFiles: new Map(),
            updatedPackageJsons: new Map(),
            processedProjects: new Set(),
            failedProjects: new Map()
        };
    }

    async scanner(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, _ctx: ExecutionContext): Promise<Json | undefined> {
                // Only process package.json files
                if (!doc.sourcePath.endsWith('package.json')) {
                    return doc;
                }

                const marker = findNodeResolutionResult(doc);
                if (!marker) {
                    return doc;
                }

                // Get the project directory and package manager
                const projectDir = path.dirname(path.resolve(doc.sourcePath));
                const pm = marker.packageManager ?? PackageManager.Npm;

                // Check if package is a direct dependency - if so, skip (use UpgradeDependencyVersion instead)
                const scopes = ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies'] as const;
                for (const scope of scopes) {
                    const deps = marker[scope];
                    if (deps?.find(d => d.name === recipe.packageName)) {
                        // Package is a direct dependency, don't add override
                        return doc;
                    }
                }

                // Check if package exists as a transitive dependency (in resolvedDependencies)
                // Note: There may be multiple versions of the same package installed
                const resolvedVersions = NodeResolutionResultQueries.getAllResolvedVersions(
                    marker,
                    recipe.packageName
                );

                if (resolvedVersions.length === 0) {
                    // Package not found in resolved dependencies at all
                    return doc;
                }

                // Check if ANY resolved version needs upgrading
                // We need an override if at least one installed version doesn't satisfy the constraint
                const anyVersionNeedsUpgrade = resolvedVersions.some(
                    rd => !semver.satisfies(rd.version, recipe.newVersion)
                );

                if (!anyVersionNeedsUpgrade) {
                    // All installed versions already satisfy the constraint
                    return doc;
                }

                // Parse dependency path if specified
                const dependencyPathSegments = recipe.dependencyPath
                    ? parseDependencyPath(recipe.dependencyPath)
                    : undefined;

                acc.projectsToUpdate.set(doc.sourcePath, {
                    projectDir,
                    packageJsonPath: doc.sourcePath,
                    originalPackageJson: await this.printDocument(doc),
                    newVersion: recipe.newVersion,
                    packageManager: pm,
                    skipInstall: false, // Always need to run install for overrides
                    dependencyPathSegments
                });

                return doc;
            }

            private async printDocument(doc: Json.Document): Promise<string> {
                return TreePrinters.print(doc);
            }
        };
    }

    async editorWithData(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, ctx: ExecutionContext): Promise<Json | undefined> {
                const sourcePath = doc.sourcePath;

                // Handle package.json files
                if (sourcePath.endsWith('package.json')) {
                    const updateInfo = acc.projectsToUpdate.get(sourcePath);
                    if (!updateInfo) {
                        return doc; // This package.json doesn't need updating
                    }

                    // Run package manager install if we haven't processed this project yet
                    if (!acc.processedProjects.has(sourcePath)) {
                        await recipe.runPackageManagerInstall(acc, updateInfo, ctx);
                        acc.processedProjects.add(sourcePath);
                    }

                    // Check if the install failed - if so, don't update, just add warning
                    const failureMessage = acc.failedProjects.get(sourcePath);
                    if (failureMessage) {
                        return markupWarn(
                            doc,
                            `Failed to add override for ${recipe.packageName} to ${recipe.newVersion}`,
                            failureMessage
                        );
                    }

                    // Add override entries
                    const modifiedDoc = await this.addOverrideEntry(doc, updateInfo);

                    // Update the NodeResolutionResult marker
                    return recipe.updateMarker(modifiedDoc, updateInfo, acc);
                }

                // Handle lock files for all package managers
                for (const lockFileName of getAllLockFileNames()) {
                    if (sourcePath.endsWith(lockFileName)) {
                        // Find the corresponding package.json path
                        const packageJsonPath = sourcePath.replace(lockFileName, 'package.json');
                        const updateInfo = acc.projectsToUpdate.get(packageJsonPath);

                        if (updateInfo && acc.updatedLockFiles.has(sourcePath)) {
                            // Parse the updated lock file content and return it
                            const updatedContent = acc.updatedLockFiles.get(sourcePath)!;
                            return this.parseUpdatedLockFile(doc, updatedContent);
                        }
                        break;
                    }
                }

                return doc;
            }

            /**
             * Adds override entry to package.json for transitive dependency upgrade.
             */
            private async addOverrideEntry(
                doc: Json.Document,
                updateInfo: ProjectUpdateInfo
            ): Promise<Json.Document> {
                // Parse current package.json content
                const currentContent = await TreePrinters.print(doc);
                let packageJson: Record<string, any>;
                try {
                    packageJson = JSON.parse(currentContent);
                } catch {
                    return doc; // Can't parse, return unchanged
                }

                // Apply override
                const modifiedPackageJson = applyOverrideToPackageJson(
                    packageJson,
                    updateInfo.packageManager,
                    recipe.packageName,
                    updateInfo.newVersion,
                    updateInfo.dependencyPathSegments
                );

                // Serialize back to JSON, preserving indentation
                const indentMatch = currentContent.match(/^(\s+)"/m);
                const indent = indentMatch ? indentMatch[1].length : 2;
                const newContent = JSON.stringify(modifiedPackageJson, null, indent);

                // Re-parse with JsonParser to get proper AST
                const parser = new JsonParser({});
                const parsed: Json.Document[] = [];
                for await (const sf of parser.parse({text: newContent, sourcePath: doc.sourcePath})) {
                    parsed.push(sf as Json.Document);
                }

                if (parsed.length > 0) {
                    return {
                        ...parsed[0],
                        sourcePath: doc.sourcePath,
                        markers: doc.markers
                    };
                }

                return doc;
            }

            /**
             * Parses updated lock file content and creates a new document.
             */
            private async parseUpdatedLockFile(
                originalDoc: Json.Document,
                updatedContent: string
            ): Promise<Json.Document> {
                const parser = new JsonParser({});
                const parsed: Json.Document[] = [];

                for await (const sf of parser.parse({text: updatedContent, sourcePath: originalDoc.sourcePath})) {
                    parsed.push(sf as Json.Document);
                }

                if (parsed.length > 0) {
                    return {
                        ...parsed[0],
                        sourcePath: originalDoc.sourcePath,
                        markers: originalDoc.markers
                    };
                }

                return originalDoc;
            }
        };
    }

    /**
     * Runs the package manager in a temporary directory to update the lock file.
     */
    private async runPackageManagerInstall(
        acc: Accumulator,
        updateInfo: ProjectUpdateInfo,
        _ctx: ExecutionContext
    ): Promise<void> {
        // Create modified package.json with the override
        const modifiedPackageJson = this.createModifiedPackageJson(
            updateInfo.originalPackageJson,
            updateInfo
        );

        const result = await runInstallInTempDir(
            updateInfo.projectDir,
            updateInfo.packageManager,
            modifiedPackageJson
        );

        if (result.success) {
            acc.updatedPackageJsons.set(updateInfo.packageJsonPath, modifiedPackageJson);

            // Store the updated lock file content
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
     * Creates a modified package.json with the override.
     */
    private createModifiedPackageJson(
        originalContent: string,
        updateInfo: ProjectUpdateInfo
    ): string {
        let packageJson = JSON.parse(originalContent);

        packageJson = applyOverrideToPackageJson(
            packageJson,
            updateInfo.packageManager,
            this.packageName,
            updateInfo.newVersion,
            updateInfo.dependencyPathSegments
        );

        return JSON.stringify(packageJson, null, 2);
    }

    /**
     * Updates the NodeResolutionResult marker with new dependency information.
     */
    private async updateMarker(
        doc: Json.Document,
        updateInfo: ProjectUpdateInfo,
        acc: Accumulator
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
            return doc;
        }

        if (updatedLockFile) {
            try {
                lockContent = JSON.parse(updatedLockFile);
            } catch {
                // Continue without lock file content
            }
        }

        const npmrcConfigs = await readNpmrcConfigs(updateInfo.projectDir);

        const newMarker = createNodeResolutionResultMarker(
            existingMarker.path,
            packageJsonContent,
            lockContent,
            existingMarker.workspacePackagePaths,
            existingMarker.packageManager,
            npmrcConfigs.length > 0 ? npmrcConfigs : undefined
        );

        return {
            ...doc,
            markers: replaceMarkerByKind(doc.markers, newMarker)
        };
    }
}
