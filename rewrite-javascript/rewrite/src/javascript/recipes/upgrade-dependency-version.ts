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
import {Tree} from "../../tree";
import {getMemberKeyName, isJson, isLiteral, Json, JsonVisitor} from "../../json";
import {isDocuments, isYaml, Yaml} from "../../yaml";
import {isPlainText, PlainText} from "../../text";
import {
    allDependencyScopes,
    DependencyScope,
    findNodeResolutionResult,
    NpmrcScope,
    PackageManager
} from "../node-resolution-result";
import * as path from "path";
import * as semver from "semver";
import {markupWarn, replaceMarkerByKind} from "../../markers";
import {TreePrinters} from "../../print";
import {
    createDependencyRecipeAccumulator,
    createLockFileEditor,
    DependencyRecipeAccumulator,
    getAllLockFileNames,
    getLockFileName,
    parseLockFileContent,
    runInstallIfNeeded,
    runInstallInTempDir,
    storeInstallResult,
    updateNodeResolutionMarker
} from "../package-manager";

/**
 * Information about a project that needs updating
 */
interface ProjectUpdateInfo {
    /** Relative path to package.json (from source root) */
    packageJsonPath: string;
    /** Original package.json content */
    originalPackageJson: string;
    /** The scope where the dependency was found */
    dependencyScope: DependencyScope;
    /** Current version constraint */
    currentVersion: string;
    /** New version constraint to apply */
    newVersion: string;
    /** The package manager used by this project */
    packageManager: PackageManager;
    /**
     * If true, skip running the package manager because the resolved version
     * already satisfies the new constraint. Only package.json needs updating.
     */
    skipInstall: boolean;
    /** Config file contents extracted from the project (e.g., .npmrc) */
    configFiles?: Record<string, string>;
}

interface Accumulator extends DependencyRecipeAccumulator<ProjectUpdateInfo> {
    /** Original lock file content, keyed by lock file path */
    originalLockFiles: Map<string, string>;
}

/**
 * Upgrades the version of a direct dependency in package.json and updates the lock file.
 *
 * This recipe:
 * 1. Finds package.json files containing the specified dependency
 * 2. Updates the version constraint to the new version
 * 3. Runs the package manager to update the lock file
 * 4. Updates the NodeResolutionResult marker with new dependency info
 *
 * For upgrading transitive dependencies (those pulled in indirectly by your direct
 * dependencies), use `UpgradeTransitiveDependencyVersion` instead.
 *
 * @see UpgradeTransitiveDependencyVersion for transitive dependencies
 */
export class UpgradeDependencyVersion extends ScanningRecipe<Accumulator> {
    readonly name = "org.openrewrite.javascript.dependencies.upgrade-dependency-version";
    readonly displayName = "Upgrade npm dependency version";
    readonly description = "Upgrades the version of a direct dependency in `package.json` and updates the lock file by running the package manager.";

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
        displayName: "Npmrc scopes",
        description: "Which .npmrc configuration scopes to include when running the package manager. " +
            "By default, only 'Project' scope is used. Include 'User' or 'Global' to access private registries " +
            "configured in those scopes. Pass as JSON array, e.g., '[\"Project\",\"User\"]'.",
        required: false,
        example: '["Project"]',
        valid: ["Global", "User", "Project"]
    })
    npmrcScopes?: string[];

    initialValue(_ctx: ExecutionContext): Accumulator {
        return {
            ...createDependencyRecipeAccumulator<ProjectUpdateInfo>(),
            originalLockFiles: new Map()
        };
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

    async scanner(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;
        const LOCK_FILE_NAMES = getAllLockFileNames();

        return new class extends TreeVisitor<Tree, ExecutionContext> {
            protected async accept(tree: Tree, ctx: ExecutionContext): Promise<Tree | undefined> {
                // Handle JSON documents (package.json and JSON lock files)
                if (isJson(tree) && tree.kind === Json.Kind.Document) {
                    return this.handleJsonDocument(tree as Json.Document, ctx);
                }

                // Handle YAML documents (pnpm-lock.yaml)
                if (isYaml(tree) && isDocuments(tree)) {
                    return this.handleYamlDocument(tree, ctx);
                }

                // Handle PlainText files (yarn.lock for Yarn Classic)
                if (isPlainText(tree)) {
                    return this.handlePlainTextDocument(tree as PlainText, ctx);
                }

                return tree;
            }

            private async handleJsonDocument(doc: Json.Document, _ctx: ExecutionContext): Promise<Json | undefined> {
                const basename = path.basename(doc.sourcePath);

                // Capture JSON lock file content (package-lock.json, bun.lock)
                if (LOCK_FILE_NAMES.includes(basename)) {
                    acc.originalLockFiles.set(doc.sourcePath, await TreePrinters.print(doc));
                    return doc;
                }

                // Only process package.json files for dependency analysis
                if (!doc.sourcePath.endsWith('package.json')) {
                    return doc;
                }

                const marker = findNodeResolutionResult(doc);
                if (!marker) {
                    return doc;
                }

                const pm = marker.packageManager ?? PackageManager.Npm;

                // Check each dependency scope for the target package
                const scopes = allDependencyScopes;
                let foundScope: DependencyScope | undefined;
                let currentVersion: string | undefined;

                for (const scope of scopes) {
                    const deps = marker[scope];
                    const dep = deps?.find(d => d.name === recipe.packageName);

                    if (dep) {
                        foundScope = scope;
                        currentVersion = dep.versionConstraint;
                        break;
                    }
                }

                if (!foundScope || !currentVersion) {
                    return doc; // Dependency not found in any scope
                }

                // Check if upgrade is needed
                if (!recipe.shouldUpgrade(currentVersion, recipe.newVersion)) {
                    return doc; // Already at target version or newer
                }

                // Check if we can skip running the package manager
                // (resolved version already satisfies the new constraint)
                const resolvedDep = marker.resolvedDependencies?.find(
                    rd => rd.name === recipe.packageName
                );
                const skipInstall = resolvedDep !== undefined &&
                    semver.satisfies(resolvedDep.version, recipe.newVersion);

                // Extract project-level .npmrc config from marker
                const configFiles: Record<string, string> = {};
                const projectNpmrc = marker.npmrcConfigs?.find(c => c.scope === NpmrcScope.Project);
                if (projectNpmrc) {
                    const lines = Object.entries(projectNpmrc.properties)
                        .map(([key, value]) => `${key}=${value}`);
                    configFiles['.npmrc'] = lines.join('\n');
                }

                acc.projectsToUpdate.set(doc.sourcePath, {
                    packageJsonPath: doc.sourcePath,
                    originalPackageJson: await TreePrinters.print(doc),
                    dependencyScope: foundScope,
                    currentVersion,
                    newVersion: recipe.newVersion,
                    packageManager: pm,
                    skipInstall,
                    configFiles: Object.keys(configFiles).length > 0 ? configFiles : undefined
                });

                return doc;
            }

            private async handleYamlDocument(docs: Yaml.Documents, _ctx: ExecutionContext): Promise<Yaml.Documents | undefined> {
                const basename = path.basename(docs.sourcePath);
                if (LOCK_FILE_NAMES.includes(basename)) {
                    acc.originalLockFiles.set(docs.sourcePath, await TreePrinters.print(docs));
                }
                return docs;
            }

            private async handlePlainTextDocument(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                const basename = path.basename(text.sourcePath);
                if (LOCK_FILE_NAMES.includes(basename)) {
                    acc.originalLockFiles.set(text.sourcePath, await TreePrinters.print(text));
                }
                return text;
            }
        };
    }

    async editorWithData(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;

        // Create JSON visitor that handles both package.json and JSON lock files
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
                    // Skip if the resolved version already satisfies the new constraint
                    const failureMessage = updateInfo.skipInstall
                        ? undefined
                        : await runInstallIfNeeded(sourcePath, acc, () =>
                            recipe.runPackageManagerInstall(acc, updateInfo, ctx)
                        );
                    if (failureMessage) {
                        return markupWarn(
                            doc,
                            `Failed to upgrade ${recipe.packageName} to ${recipe.newVersion}`,
                            failureMessage
                        );
                    }

                    // Update the dependency version in the JSON AST (preserves formatting)
                    const visitor = new UpdateVersionVisitor(
                        recipe.packageName,
                        updateInfo.newVersion,
                        updateInfo.dependencyScope
                    );
                    const modifiedDoc = await visitor.visit(doc, undefined) as Json.Document;

                    // Update the NodeResolutionResult marker
                    if (updateInfo.skipInstall) {
                        // Just update the versionConstraint in the marker - resolved version is unchanged
                        return recipe.updateMarkerVersionConstraint(modifiedDoc, updateInfo);
                    }
                    return updateNodeResolutionMarker(modifiedDoc, updateInfo, acc);
                }

                // Handle JSON lock files (package-lock.json, bun.lock)
                const lockFileName = path.basename(sourcePath);
                if (getAllLockFileNames().includes(lockFileName)) {
                    const updatedLockContent = acc.updatedLockFiles.get(sourcePath);
                    if (updatedLockContent) {
                        const parsed = await parseLockFileContent(updatedLockContent, sourcePath, lockFileName) as Json.Document;
                        // Preserve original ID for RPC compatibility
                        return {
                            ...doc,
                            value: parsed.value,
                            eof: parsed.eof
                        } as Json.Document;
                    }
                }

                return doc;
            }
        };

        // Return composite visitor that handles both JSON and YAML lock files
        return createLockFileEditor(jsonEditor, acc);
    }

    /**
     * Runs the package manager in a temporary directory to update the lock file.
     * Writes a modified package.json with the new version, then runs install to update the lock file.
     * All file contents are provided from in-memory sources (SourceFiles), not read from disk.
     */
    private async runPackageManagerInstall(
        acc: Accumulator,
        updateInfo: ProjectUpdateInfo,
        _ctx: ExecutionContext
    ): Promise<void> {
        // Create modified package.json with the new version constraint
        const modifiedPackageJson = this.createModifiedPackageJson(
            updateInfo.originalPackageJson,
            updateInfo.dependencyScope,
            updateInfo.newVersion
        );

        // Get the lock file path based on package manager
        const lockFileName = getLockFileName(updateInfo.packageManager);
        const packageJsonDir = path.dirname(updateInfo.packageJsonPath);
        const lockFilePath = packageJsonDir === '.'
            ? lockFileName
            : path.join(packageJsonDir, lockFileName);

        // Look up the original lock file content from captured SourceFiles
        const originalLockFileContent = acc.originalLockFiles.get(lockFilePath);

        const result = await runInstallInTempDir(
            updateInfo.packageManager,
            modifiedPackageJson,
            {
                originalLockFileContent,
                configFiles: updateInfo.configFiles
            }
        );

        storeInstallResult(result, acc, updateInfo, modifiedPackageJson);
    }

    /**
     * Creates a modified package.json with the updated dependency version.
     * Used for the temp directory to validate the version exists.
     */
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

    /**
     * Updates just the versionConstraint in the marker for the target dependency.
     * Used when skipInstall is true - the resolved version is unchanged.
     */
    private updateMarkerVersionConstraint(
        doc: Json.Document,
        updateInfo: ProjectUpdateInfo
    ): Json.Document {
        const existingMarker = findNodeResolutionResult(doc);
        if (!existingMarker) {
            return doc;
        }

        // Update the versionConstraint for the target dependency
        const deps = existingMarker[updateInfo.dependencyScope];
        const updatedDeps = deps?.map(dep =>
            dep.name === this.packageName
                ? {...dep, versionConstraint: updateInfo.newVersion}
                : dep
        );

        const newMarker = {
            ...existingMarker,
            [updateInfo.dependencyScope]: updatedDeps
        };

        return {
            ...doc,
            markers: replaceMarkerByKind(doc.markers, newMarker)
        };
    }
}

/**
 * Visitor that updates the version of a specific dependency in a specific scope.
 */
class UpdateVersionVisitor extends JsonVisitor<void> {
    private readonly packageName: string;
    private readonly newVersion: string;
    private readonly targetScope: DependencyScope;
    private inTargetScope = false;

    constructor(packageName: string, newVersion: string, targetScope: DependencyScope) {
        super();
        this.packageName = packageName;
        this.newVersion = newVersion;
        this.targetScope = targetScope;
    }

    protected async visitMember(member: Json.Member, p: void): Promise<Json | undefined> {
        // Check if we're entering the target scope
        const keyName = getMemberKeyName(member);

        if (keyName === this.targetScope) {
            // We're entering the dependencies scope
            this.inTargetScope = true;
            const result = await super.visitMember(member, p);
            this.inTargetScope = false;
            return result;
        }

        // Check if this is the dependency we're looking for
        if (this.inTargetScope && keyName === this.packageName) {
            // Update the version value
            return this.updateVersion(member);
        }

        return super.visitMember(member, p);
    }

    private updateVersion(member: Json.Member): Json.Member {
        const value = member.value;

        if (!isLiteral(value)) {
            return member; // Not a literal value, can't update
        }

        // Create new literal with updated version
        const newLiteral: Json.Literal = {
            ...value,
            source: `"${this.newVersion}"`,
            value: this.newVersion
        };

        return {
            ...member,
            value: newLiteral
        };
    }
}
