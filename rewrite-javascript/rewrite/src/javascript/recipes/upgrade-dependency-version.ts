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

/**
 * Represents a dependency scope in package.json
 */
type DependencyScope = 'dependencies' | 'devDependencies' | 'peerDependencies' | 'optionalDependencies';

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

    initialValue(_ctx: ExecutionContext): Accumulator {
        return {
            projectsToUpdate: new Map(),
            updatedLockFiles: new Map(),
            updatedPackageJsons: new Map(),
            processedProjects: new Set(),
            failedProjects: new Map()
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

                // Check each dependency scope for the target package
                const scopes: DependencyScope[] = ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies'];
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

                acc.projectsToUpdate.set(doc.sourcePath, {
                    projectDir,
                    packageJsonPath: doc.sourcePath,
                    originalPackageJson: await this.printDocument(doc),
                    dependencyScope: foundScope,
                    currentVersion,
                    newVersion: recipe.newVersion,
                    packageManager: pm,
                    skipInstall
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
                    // Skip if the resolved version already satisfies the new constraint
                    if (!updateInfo.skipInstall && !acc.processedProjects.has(sourcePath)) {
                        await recipe.runPackageManagerInstall(acc, updateInfo, ctx);
                        acc.processedProjects.add(sourcePath);
                    }

                    // Check if the install failed - if so, don't update, just add warning
                    const failureMessage = acc.failedProjects.get(sourcePath);
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
             * Parses updated lock file content and creates a new document.
             */
            private async parseUpdatedLockFile(
                originalDoc: Json.Document,
                updatedContent: string
            ): Promise<Json.Document> {
                // Parse the updated content using JsonParser
                const parser = new JsonParser({});
                const parsed: Json.Document[] = [];

                for await (const sf of parser.parse({text: updatedContent, sourcePath: originalDoc.sourcePath})) {
                    parsed.push(sf as Json.Document);
                }

                if (parsed.length > 0) {
                    // Preserve the original source path and markers
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
     * Writes a modified package.json with the new version, then runs install to update the lock file.
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

        const result = await runInstallInTempDir(
            updateInfo.projectDir,
            updateInfo.packageManager,
            modifiedPackageJson
        );

        if (result.success) {
            // Store the modified package.json (we'll use our visitor for actual output)
            acc.updatedPackageJsons.set(updateInfo.packageJsonPath, modifiedPackageJson);

            // Store the updated lock file content
            if (result.lockFileContent) {
                const lockFileName = getLockFileName(updateInfo.packageManager);
                const lockFilePath = updateInfo.packageJsonPath.replace('package.json', lockFileName);
                acc.updatedLockFiles.set(lockFilePath, result.lockFileContent);
            }
        } else {
            // Track the failure - don't update package.json, the version likely doesn't exist
            acc.failedProjects.set(updateInfo.packageJsonPath, result.error || 'Unknown error');
        }
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

        // If we skipped install, just update the versionConstraint in the marker
        // The resolved version is already correct, we only changed the constraint
        if (updateInfo.skipInstall) {
            return this.updateMarkerVersionConstraint(doc, existingMarker, updateInfo);
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
            return doc; // Failed to parse, keep original marker
        }

        if (updatedLockFile) {
            try {
                lockContent = JSON.parse(updatedLockFile);
            } catch {
                // Continue without lock file content
            }
        }

        // Read npmrc configs from the project directory
        const npmrcConfigs = await readNpmrcConfigs(updateInfo.projectDir);

        // Create new marker
        const newMarker = createNodeResolutionResultMarker(
            existingMarker.path,
            packageJsonContent,
            lockContent,
            existingMarker.workspacePackagePaths,
            existingMarker.packageManager,
            npmrcConfigs.length > 0 ? npmrcConfigs : undefined
        );

        // Replace the marker in the document
        return {
            ...doc,
            markers: replaceMarkerByKind(doc.markers, newMarker)
        };
    }

    /**
     * Updates just the versionConstraint in the marker for the target dependency.
     * Used when skipInstall is true - the resolved version is unchanged.
     */
    private updateMarkerVersionConstraint(
        doc: Json.Document,
        existingMarker: any,
        updateInfo: ProjectUpdateInfo
    ): Json.Document {
        // Create updated dependency lists with the new versionConstraint
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
        const keyName = this.getMemberKeyName(member);

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

    private getMemberKeyName(member: Json.Member): string | undefined {
        const key = member.key.element;
        if (key.kind === Json.Kind.Literal) {
            // Remove quotes from string literal
            const source = (key as Json.Literal).source;
            if (source.startsWith('"') && source.endsWith('"')) {
                return source.slice(1, -1);
            }
            return source;
        } else if (key.kind === Json.Kind.Identifier) {
            return (key as Json.Identifier).name;
        }
        return undefined;
    }

    private updateVersion(member: Json.Member): Json.Member {
        const value = member.value;

        if (value.kind !== Json.Kind.Literal) {
            return member; // Not a literal value, can't update
        }

        const literal = value as Json.Literal;

        // Create new literal with updated version
        const newLiteral: Json.Literal = {
            ...literal,
            source: `"${this.newVersion}"`,
            value: this.newVersion
        };

        return {
            ...member,
            value: newLiteral
        };
    }
}
