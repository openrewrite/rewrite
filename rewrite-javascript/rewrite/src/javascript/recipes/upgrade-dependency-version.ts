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
import {Json, JsonVisitor} from "../../json";
import {findNodeResolutionResult, PackageManager} from "../node-resolution-result";
import * as path from "path";
import * as semver from "semver";
import {replaceMarkerByKind} from "../../markers";
import {getAllLockFileNames} from "../package-manager";
import {
    createDependencyAccumulator,
    createWarningDocument,
    DependencyAccumulator,
    DependencyScope,
    parseUpdatedLockFile,
    printDocument,
    runPackageManagerInstall,
    updateNodeResolutionMarker,
    UpdateVersionVisitor
} from "./dependency-utils";

/**
 * Upgrades the version of a dependency in package.json and updates the lock file.
 *
 * This recipe:
 * 1. Finds package.json files containing the specified dependency
 * 2. Updates the version constraint to the new version
 * 3. Runs the package manager to update the lock file
 * 4. Updates the NodeResolutionResult marker with new dependency info
 *
 * TODO: Consider adding a `resolveToLatestMatching` option that would use `npm install <pkg>@<version>`
 * to let the package manager resolve the constraint to the latest matching version.
 * For example, `^22.0.0` would become `^22.19.1` (the latest version satisfying ^22.0.0).
 * This would be similar to how Maven's UpgradeDependencyVersion works with version selectors.
 */
export class UpgradeDependencyVersion extends ScanningRecipe<DependencyAccumulator> {
    readonly name = "org.openrewrite.javascript.dependencies.upgrade-dependency-version";
    readonly displayName = "Upgrade npm dependency version";
    readonly description = "Upgrades the version of a dependency in `package.json` and updates the lock file by running the package manager.";

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

    initialValue(_ctx: ExecutionContext): DependencyAccumulator {
        return createDependencyAccumulator();
    }

    /**
     * Determines if the dependency should be upgraded from currentVersion to newVersion.
     * Returns true only if the new version constraint represents a strictly newer version.
     *
     * This prevents:
     * - Re-applying the same version (idempotency)
     * - Downgrading to an older version
     *
     * @param currentVersion Current version constraint (e.g., "^4.17.20")
     * @param newVersion New version constraint to apply (e.g., "^4.17.21")
     * @returns true if upgrade should proceed, false otherwise
     */
    shouldUpgrade(currentVersion: string, newVersion: string): boolean {
        // If they're identical strings, no upgrade needed
        if (currentVersion === newVersion) {
            return false;
        }

        // Extract the minimum version from each constraint
        // semver.minVersion returns the lowest version that could match the range
        const currentMin = semver.minVersion(currentVersion);
        const newMin = semver.minVersion(newVersion);

        // If either constraint is invalid, fall back to string comparison
        // (will upgrade if strings differ)
        if (!currentMin || !newMin) {
            return currentVersion !== newVersion;
        }

        // Only upgrade if new minimum version is strictly greater than current
        return semver.gt(newMin, currentMin);
    }

    async scanner(acc: DependencyAccumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, ctx: ExecutionContext): Promise<Json | undefined> {
                // Only process package.json files
                if (!doc.sourcePath.endsWith('package.json')) {
                    return doc;
                }

                const marker = findNodeResolutionResult(doc);
                if (!marker) {
                    return doc;
                }

                // Check each dependency scope for the target package
                const scopes: DependencyScope[] = ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies'];

                for (const scope of scopes) {
                    const deps = marker[scope];
                    const dep = deps?.find(d => d.name === recipe.packageName);

                    if (dep) {
                        const currentVersion = dep.versionConstraint;

                        // Check if version needs updating using semver comparison
                        // Only upgrade if the new version is strictly newer than current
                        if (recipe.shouldUpgrade(currentVersion, recipe.newVersion)) {
                            // Get the project directory from the marker path
                            const projectDir = path.dirname(path.resolve(doc.sourcePath));

                            // Use package manager from marker (set during parsing), default to npm
                            const pm = marker.packageManager ?? PackageManager.Npm;

                            // Check if the resolved version already satisfies the new constraint.
                            // If so, we can skip running the package manager entirely.
                            const resolvedDep = marker.resolvedDependencies?.find(
                                rd => rd.name === recipe.packageName
                            );
                            const skipInstall = resolvedDep !== undefined &&
                                semver.satisfies(resolvedDep.version, recipe.newVersion);

                            acc.projectsToUpdate.set(doc.sourcePath, {
                                projectDir,
                                packageJsonPath: doc.sourcePath,
                                originalPackageJson: await printDocument(doc),
                                dependencyScope: scope,
                                currentVersion,
                                newVersion: recipe.newVersion,
                                packageName: recipe.packageName,
                                packageManager: pm,
                                skipInstall
                            });
                        }
                        break;
                    }
                }

                return doc;
            }
        };
    }

    async editorWithData(acc: DependencyAccumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(doc: Json.Document, _ctx: ExecutionContext): Promise<Json | undefined> {
                const sourcePath = doc.sourcePath;

                if (sourcePath.endsWith('package.json')) {
                    const updateInfo = acc.projectsToUpdate.get(sourcePath);
                    if (!updateInfo) {
                        return doc;
                    }

                    if (!updateInfo.skipInstall && !acc.processedProjects.has(sourcePath)) {
                        await runPackageManagerInstall(acc, updateInfo, (original) =>
                            recipe.createModifiedPackageJson(original, updateInfo.dependencyScope, updateInfo.newVersion)
                        );
                        acc.processedProjects.add(sourcePath);
                    }

                    const failureMessage = acc.failedProjects.get(sourcePath);
                    if (failureMessage) {
                        return createWarningDocument(doc, 'upgrade', recipe.packageName, recipe.newVersion, failureMessage);
                    }

                    const visitor = new UpdateVersionVisitor(
                        recipe.packageName,
                        updateInfo.newVersion,
                        updateInfo.dependencyScope
                    );
                    const modifiedDoc = await visitor.visit(doc, undefined) as Json.Document;

                    if (updateInfo.skipInstall) {
                        return recipe.updateMarkerVersionConstraint(modifiedDoc, updateInfo);
                    }

                    return updateNodeResolutionMarker(modifiedDoc, updateInfo, acc, findNodeResolutionResult);
                }

                for (const lockFileName of getAllLockFileNames()) {
                    if (sourcePath.endsWith(lockFileName)) {
                        const packageJsonPath = sourcePath.replace(lockFileName, 'package.json');
                        const updateInfo = acc.projectsToUpdate.get(packageJsonPath);

                        if (updateInfo && acc.updatedLockFiles.has(sourcePath)) {
                            const updatedContent = acc.updatedLockFiles.get(sourcePath)!;
                            return parseUpdatedLockFile(doc, updatedContent);
                        }
                        break;
                    }
                }

                return doc;
            }
        };
    }

    private createModifiedPackageJson(
        originalContent: string,
        scope: DependencyScope,
        newVersion: string
    ): string {
        const packageJson = JSON.parse(originalContent);

        if (packageJson[scope] && packageJson[scope][this.packageName]) {
            packageJson[scope][this.packageName] = newVersion;
        }

        return JSON.stringify(packageJson, null, 2);
    }

    private updateMarkerVersionConstraint(
        doc: Json.Document,
        updateInfo: { dependencyScope: DependencyScope; newVersion: string }
    ): Json.Document {
        const existingMarker = findNodeResolutionResult(doc);
        if (!existingMarker) {
            return doc;
        }

        const updateDeps = (deps: any[] | undefined) => {
            if (!deps) return deps;
            return deps.map(dep => {
                if (dep.name === this.packageName) {
                    return {...dep, versionConstraint: updateInfo.newVersion};
                }
                return dep;
            });
        };

        const newMarker = {
            ...existingMarker,
            [updateInfo.dependencyScope]: updateDeps(existingMarker[updateInfo.dependencyScope])
        };

        return {
            ...doc,
            markers: replaceMarkerByKind(doc.markers, newMarker)
        };
    }
}
