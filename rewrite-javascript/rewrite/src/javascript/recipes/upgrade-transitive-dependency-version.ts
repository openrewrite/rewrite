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
import {isDocuments, Yaml, YamlVisitor} from "../../yaml";
import {
    allDependencyScopes,
    findNodeResolutionResult,
    NodeResolutionResultQueries,
    PackageManager
} from "../node-resolution-result";
import * as path from "path";
import * as semver from "semver";
import {markupWarn} from "../../markers";
import {TreePrinters} from "../../print";
import {
    createDependencyRecipeAccumulator,
    DependencyRecipeAccumulator,
    getAllLockFileNames,
    getUpdatedLockFileContent,
    parseLockFileContent,
    runInstallIfNeeded,
    runInstallInTempDir,
    storeInstallResult,
    updateNodeResolutionMarker
} from "../package-manager";
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

type Accumulator = DependencyRecipeAccumulator<ProjectUpdateInfo>;

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
        description: "The name of the npm package to upgrade (e.g., `lodash`, `@types/node`)",
        example: "lodash"
    })
    packageName!: string;

    @Option({
        displayName: "Version",
        description: "The version constraint to set (e.g., `^5.0.0`, `~2.1.0`, `3.0.0`)",
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
        return createDependencyRecipeAccumulator();
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
                for (const scope of allDependencyScopes) {
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

        // Create visitors for each file type
        const jsonEditor = new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, ctx: ExecutionContext): Promise<Json | undefined> {
                const sourcePath = doc.sourcePath;

                // Handle package.json files
                if (sourcePath.endsWith('package.json')) {
                    const updateInfo = acc.projectsToUpdate.get(sourcePath);
                    if (!updateInfo) {
                        return doc; // This package.json doesn't need updating
                    }

                    // Run package manager install if needed, check for failure
                    const failureMessage = await runInstallIfNeeded(sourcePath, acc, () =>
                        recipe.runPackageManagerInstall(acc, updateInfo, ctx)
                    );
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
                    return updateNodeResolutionMarker(modifiedDoc, updateInfo, acc);
                }

                // Handle JSON lock files (package-lock.json, bun.lock)
                const lockFileName = path.basename(sourcePath);
                const updatedLockContent = getUpdatedLockFileContent(sourcePath, acc);
                if (updatedLockContent && getAllLockFileNames().includes(lockFileName)) {
                    return await parseLockFileContent(updatedLockContent, sourcePath, lockFileName) as Json.Document;
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
                const parsed = await new JsonParser({}).parseOne({
                    text: newContent,
                    sourcePath: doc.sourcePath
                }) as Json.Document;

                return {
                    ...parsed,
                    markers: doc.markers
                };
            }
        };

        const yamlEditor = new class extends YamlVisitor<ExecutionContext> {
            protected async visitDocuments(docs: Yaml.Documents, _ctx: ExecutionContext): Promise<Yaml | undefined> {
                const sourcePath = docs.sourcePath;
                const lockFileName = path.basename(sourcePath);

                // Handle YAML lock files (pnpm-lock.yaml)
                const updatedLockContent = getUpdatedLockFileContent(sourcePath, acc);
                if (updatedLockContent && getAllLockFileNames().includes(lockFileName)) {
                    return await parseLockFileContent(updatedLockContent, sourcePath, lockFileName) as Yaml.Documents;
                }

                return docs;
            }
        };

        // Return a composite visitor that delegates based on tree type
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

        storeInstallResult(result, acc, updateInfo, modifiedPackageJson);
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

}
