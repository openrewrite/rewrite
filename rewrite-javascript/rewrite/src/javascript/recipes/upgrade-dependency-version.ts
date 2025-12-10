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
import * as fs from "fs";
import * as fsp from "fs/promises";
import * as path from "path";
import * as os from "os";
import * as semver from "semver";
import {markupWarn, replaceMarkerByKind} from "../../markers";
import {TreePrinters} from "../../print";
import {
    getAllLockFileNames,
    getLockFileName,
    runInstall
} from "../package-manager";

/**
 * Represents a dependency scope in package.json
 */
type DependencyScope = 'dependencies' | 'devDependencies' | 'peerDependencies' | 'optionalDependencies';

/**
 * Policy for handling which dependencies to upgrade.
 * - 'Direct': Only upgrade direct dependencies (default)
 * - 'Transitive': Only add overrides for transitive dependencies
 * - 'DirectAndTransitive': Upgrade direct AND add overrides for transitive
 */
type UpgradePolicy = 'Direct' | 'Transitive' | 'DirectAndTransitive';

/**
 * Parsed dependency path for scoped overrides.
 * Segments represent the chain of dependencies, e.g., "express>accepts" becomes
 * [{name: "express"}, {name: "accepts"}]
 */
interface DependencyPathSegment {
    name: string;
    version?: string;
}

/**
 * Parses a dependency path string into segments.
 * Accepts both '>' (pnpm style) and '/' (yarn style) as separators.
 * Examples:
 *   "express>accepts" -> [{name: "express"}, {name: "accepts"}]
 *   "express@4.0.0>accepts" -> [{name: "express", version: "4.0.0"}, {name: "accepts"}]
 *   "@scope/pkg>dep" -> [{name: "@scope/pkg"}, {name: "dep"}]
 * @internal Exported for testing
 */
export function parseDependencyPath(path: string): DependencyPathSegment[] {
    // We can't just replace all '/' with '>' because scoped packages contain '/'
    // Strategy: Split on '>' first, then for each part that contains '/' and doesn't
    // start with '@', treat it as a '/'-separated path (yarn style)
    const segments: DependencyPathSegment[] = [];

    // Split on '>' (pnpm style separator)
    const gtParts = path.split('>');

    for (const gtPart of gtParts) {
        // Check if this part needs further splitting by '/'
        // Only split if it contains '/' AND either:
        // - doesn't start with '@' (not a scoped package), OR
        // - contains multiple '/' (e.g., "@scope/pkg/dep" is yarn-style path)
        if (gtPart.includes('/')) {
            if (gtPart.startsWith('@')) {
                // Scoped package: @scope/pkg or @scope/pkg@version or @scope/pkg/dep (yarn path)
                // Find the first '/' which is part of the scope
                const firstSlash = gtPart.indexOf('/');
                const afterFirstSlash = gtPart.substring(firstSlash + 1);

                // Check if there's another '/' after the scope (yarn-style nesting)
                const secondSlash = afterFirstSlash.indexOf('/');
                if (secondSlash !== -1) {
                    // yarn-style: @scope/pkg/dep - split further
                    // First get the scoped package part
                    const scopedPart = gtPart.substring(0, firstSlash + 1 + secondSlash);
                    segments.push(parseSegment(scopedPart));

                    // Then handle the rest as separate segments
                    const rest = afterFirstSlash.substring(secondSlash + 1);
                    for (const subPart of rest.split('/')) {
                        if (subPart) {
                            segments.push(parseSegment(subPart));
                        }
                    }
                } else {
                    // Simple scoped package: @scope/pkg or @scope/pkg@version
                    segments.push(parseSegment(gtPart));
                }
            } else {
                // Non-scoped with '/': yarn-style path like "express/accepts"
                for (const slashPart of gtPart.split('/')) {
                    if (slashPart) {
                        segments.push(parseSegment(slashPart));
                    }
                }
            }
        } else {
            // No '/', just parse the segment directly
            segments.push(parseSegment(gtPart));
        }
    }

    return segments;
}

/**
 * Parses a single segment (package name, possibly with version).
 */
function parseSegment(part: string): DependencyPathSegment {
    // Handle scoped packages: @scope/name or @scope/name@version
    if (part.startsWith('@')) {
        // Find the version separator (last @ that's not the scope prefix)
        const slashIndex = part.indexOf('/');
        if (slashIndex === -1) {
            return {name: part};
        }
        const afterSlash = part.substring(slashIndex + 1);
        const atIndex = afterSlash.lastIndexOf('@');
        if (atIndex > 0) {
            return {
                name: part.substring(0, slashIndex + 1 + atIndex),
                version: afterSlash.substring(atIndex + 1)
            };
        }
        return {name: part};
    }

    // Non-scoped package: name or name@version
    const atIndex = part.lastIndexOf('@');
    if (atIndex > 0) {
        return {
            name: part.substring(0, atIndex),
            version: part.substring(atIndex + 1)
        };
    }
    return {name: part};
}

/**
 * Generates an npm-style override entry (nested objects).
 * npm uses nested objects for scoped overrides:
 *   { "express": { "accepts": "^2.0.0" } }
 * or for global overrides:
 *   { "lodash": "^4.17.21" }
 *
 * @param packageName The target package to override
 * @param newVersion The version to set
 * @param pathSegments Optional path segments for scoped override (excludes the target package)
 */
function generateNpmOverride(
    packageName: string,
    newVersion: string,
    pathSegments?: DependencyPathSegment[]
): Record<string, any> {
    if (!pathSegments || pathSegments.length === 0) {
        // Global override
        return {[packageName]: newVersion};
    }

    // Build nested structure from outside in
    let result: Record<string, any> = {[packageName]: newVersion};
    for (let i = pathSegments.length - 1; i >= 0; i--) {
        const segment = pathSegments[i];
        const key = segment.version ? `${segment.name}@${segment.version}` : segment.name;
        result = {[key]: result};
    }
    return result;
}

/**
 * Generates a Yarn-style resolution entry (path with / separator).
 * Yarn uses a flat object with path keys:
 *   { "express/accepts": "^2.0.0" }
 * or for global:
 *   { "lodash": "^4.17.21" }
 *
 * @param packageName The target package to override
 * @param newVersion The version to set
 * @param pathSegments Optional path segments for scoped override (excludes the target package)
 */
function generateYarnResolution(
    packageName: string,
    newVersion: string,
    pathSegments?: DependencyPathSegment[]
): Record<string, string> {
    if (!pathSegments || pathSegments.length === 0) {
        // Global resolution
        return {[packageName]: newVersion};
    }

    // Yarn uses / separator: "express/accepts"
    // Note: Yarn only supports one level of nesting, so we use the last segment
    const parentSegment = pathSegments[pathSegments.length - 1];
    const parentKey = parentSegment.version
        ? `${parentSegment.name}@${parentSegment.version}`
        : parentSegment.name;
    const key = `${parentKey}/${packageName}`;
    return {[key]: newVersion};
}

/**
 * Generates a pnpm-style override entry (path with > separator).
 * pnpm uses a flat object with > path keys:
 *   { "express>accepts": "^2.0.0" }
 * or for global:
 *   { "lodash": "^4.17.21" }
 *
 * @param packageName The target package to override
 * @param newVersion The version to set
 * @param pathSegments Optional path segments for scoped override (excludes the target package)
 */
function generatePnpmOverride(
    packageName: string,
    newVersion: string,
    pathSegments?: DependencyPathSegment[]
): Record<string, string> {
    if (!pathSegments || pathSegments.length === 0) {
        // Global override
        return {[packageName]: newVersion};
    }

    // pnpm uses > separator: "express@1>accepts"
    const pathParts = pathSegments.map(seg =>
        seg.version ? `${seg.name}@${seg.version}` : seg.name
    );
    const key = `${pathParts.join('>')}>${packageName}`;
    return {[key]: newVersion};
}

/**
 * Merges a new override entry into an existing overrides object.
 * Handles npm's nested structure by deep merging.
 */
function mergeNpmOverride(
    existing: Record<string, any>,
    newOverride: Record<string, any>
): Record<string, any> {
    const result = {...existing};

    for (const [key, value] of Object.entries(newOverride)) {
        if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
            // Deep merge for nested objects
            if (typeof result[key] === 'object' && result[key] !== null) {
                result[key] = mergeNpmOverride(result[key], value);
            } else {
                result[key] = value;
            }
        } else {
            // Simple value, just overwrite
            result[key] = value;
        }
    }

    return result;
}

/**
 * Merges a new resolution/override entry into an existing flat object.
 * Used for Yarn resolutions and pnpm overrides.
 */
function mergeFlatOverride(
    existing: Record<string, string>,
    newOverride: Record<string, string>
): Record<string, string> {
    return {...existing, ...newOverride};
}

/**
 * Applies an override to a package.json object based on the package manager.
 *
 * @param packageJson The parsed package.json object
 * @param packageManager The package manager in use
 * @param packageName The target package to override
 * @param newVersion The version to set
 * @param pathSegments Optional path segments for scoped override
 * @returns The modified package.json object
 * @internal Exported for testing
 */
export function applyOverrideToPackageJson(
    packageJson: Record<string, any>,
    packageManager: PackageManager,
    packageName: string,
    newVersion: string,
    pathSegments?: DependencyPathSegment[]
): Record<string, any> {
    const result = {...packageJson};

    switch (packageManager) {
        case PackageManager.Npm:
        case PackageManager.Bun: {
            // npm and Bun use "overrides" with nested objects
            const newOverride = generateNpmOverride(packageName, newVersion, pathSegments);
            result.overrides = mergeNpmOverride(result.overrides || {}, newOverride);
            break;
        }

        case PackageManager.YarnClassic:
        case PackageManager.YarnBerry: {
            // Yarn uses "resolutions" with flat path keys
            const newResolution = generateYarnResolution(packageName, newVersion, pathSegments);
            result.resolutions = mergeFlatOverride(result.resolutions || {}, newResolution);
            break;
        }

        case PackageManager.Pnpm: {
            // pnpm uses "pnpm.overrides" with > path keys
            const newOverride = generatePnpmOverride(packageName, newVersion, pathSegments);
            if (!result.pnpm) {
                result.pnpm = {};
            }
            result.pnpm.overrides = mergeFlatOverride(result.pnpm?.overrides || {}, newOverride);
            break;
        }
    }

    return result;
}

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
    /** The scope where the dependency was found (for direct deps) */
    dependencyScope?: DependencyScope;
    /** Current version constraint (for direct deps) */
    currentVersion?: string;
    /** New version constraint to apply */
    newVersion: string;
    /** The package manager used by this project */
    packageManager: PackageManager;
    /**
     * If true, skip running the package manager because the resolved version
     * already satisfies the new constraint. Only package.json needs updating.
     */
    skipInstall: boolean;
    /** Whether to update a direct dependency */
    updateDirect: boolean;
    /** Whether to add/update override entries for transitive dependencies */
    addOverride: boolean;
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
export class UpgradeDependencyVersion extends ScanningRecipe<Accumulator> {
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

    @Option({
        displayName: "Upgrade policy",
        description: "Controls which dependencies are upgraded. 'Direct' (default) only upgrades direct dependencies. 'Transitive' adds override entries for transitive occurrences. 'DirectAndTransitive' does both.",
        required: false,
        example: "Transitive"
    })
    upgradePolicy?: string;

    @Option({
        displayName: "Dependency path",
        description: "Optional path to scope the override to a specific dependency chain. Use '>' as separator (e.g., 'express>accepts'). When not specified, applies globally to all transitive occurrences. Only used when policy includes transitive handling.",
        required: false,
        example: "express>accepts"
    })
    dependencyPath?: string;

    /**
     * Returns the effective upgrade policy, defaulting to 'Direct'.
     */
    private getUpgradePolicy(): UpgradePolicy {
        const policy = this.upgradePolicy as UpgradePolicy | undefined;
        if (policy === 'Transitive' || policy === 'DirectAndTransitive') {
            return policy;
        }
        return 'Direct';
    }

    /**
     * Returns true if the policy includes upgrading direct dependencies.
     */
    private shouldUpgradeDirect(): boolean {
        const policy = this.getUpgradePolicy();
        return policy === 'Direct' || policy === 'DirectAndTransitive';
    }

    /**
     * Returns true if the policy includes adding overrides for transitive dependencies.
     */
    private shouldAddOverrides(): boolean {
        const policy = this.getUpgradePolicy();
        return policy === 'Transitive' || policy === 'DirectAndTransitive';
    }

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
            protected async visitDocument(doc: Json.Document, ctx: ExecutionContext): Promise<Json | undefined> {
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

                // Check each dependency scope for the target package as a direct dependency
                const scopes: DependencyScope[] = ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies'];
                let directDep: { scope: DependencyScope; currentVersion: string } | undefined;

                for (const scope of scopes) {
                    const deps = marker[scope];
                    const dep = deps?.find(d => d.name === recipe.packageName);

                    if (dep) {
                        directDep = {scope, currentVersion: dep.versionConstraint};
                        break;
                    }
                }

                // Check if package exists as a transitive dependency (in resolvedDependencies)
                const resolvedDep = marker.resolvedDependencies?.find(
                    rd => rd.name === recipe.packageName
                );

                // Determine what actions to take based on upgrade policy
                const shouldUpdateDirect = recipe.shouldUpgradeDirect() && directDep !== undefined &&
                    recipe.shouldUpgrade(directDep.currentVersion, recipe.newVersion);

                // For transitive: package must exist in resolved deps and not be a direct dep
                // (or policy is DirectAndTransitive which handles both)
                const isTransitiveOnly = resolvedDep !== undefined && directDep === undefined;
                const shouldAddOverride = recipe.shouldAddOverrides() && (
                    isTransitiveOnly ||
                    // When DirectAndTransitive and package is both direct and transitive,
                    // we add override to ensure all transitive instances are also updated
                    (recipe.getUpgradePolicy() === 'DirectAndTransitive' && resolvedDep !== undefined)
                );

                // Check if the resolved version needs upgrading
                const resolvedVersionNeedsUpgrade = resolvedDep !== undefined &&
                    !semver.satisfies(resolvedDep.version, recipe.newVersion);

                // Skip if nothing to do
                if (!shouldUpdateDirect && !shouldAddOverride) {
                    return doc;
                }

                // For overrides, we only need to act if the resolved version doesn't satisfy the new constraint
                if (shouldAddOverride && !resolvedVersionNeedsUpgrade && !shouldUpdateDirect) {
                    return doc;
                }

                // For direct deps, check if already at target version
                if (shouldUpdateDirect && !shouldAddOverride && directDep &&
                    !recipe.shouldUpgrade(directDep.currentVersion, recipe.newVersion)) {
                    return doc;
                }

                // Check if we can skip running the package manager
                // (resolved version already satisfies the new constraint)
                const skipInstall = resolvedDep !== undefined &&
                    semver.satisfies(resolvedDep.version, recipe.newVersion);

                // Parse dependency path if specified
                const dependencyPathSegments = recipe.dependencyPath
                    ? parseDependencyPath(recipe.dependencyPath)
                    : undefined;

                acc.projectsToUpdate.set(doc.sourcePath, {
                    projectDir,
                    packageJsonPath: doc.sourcePath,
                    originalPackageJson: await this.printDocument(doc),
                    dependencyScope: shouldUpdateDirect ? directDep!.scope : undefined,
                    currentVersion: shouldUpdateDirect ? directDep!.currentVersion : undefined,
                    newVersion: recipe.newVersion,
                    packageManager: pm,
                    skipInstall,
                    updateDirect: shouldUpdateDirect,
                    addOverride: shouldAddOverride && resolvedVersionNeedsUpgrade,
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

                    let modifiedDoc = doc;

                    // Update the direct dependency version in the JSON AST (preserves formatting)
                    if (updateInfo.updateDirect && updateInfo.dependencyScope) {
                        const visitor = new UpdateVersionVisitor(
                            recipe.packageName,
                            updateInfo.newVersion,
                            updateInfo.dependencyScope
                        );
                        modifiedDoc = await visitor.visit(modifiedDoc, undefined) as Json.Document;
                    }

                    // Add override entries for transitive dependencies
                    if (updateInfo.addOverride) {
                        modifiedDoc = await this.addOverrideEntry(modifiedDoc, updateInfo);
                    }

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
             * Uses the applyOverrideToPackageJson function to generate the correct format
             * for the package manager in use.
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
                // Detect indentation from original content
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
        const pm = updateInfo.packageManager;
        const lockFileName = getLockFileName(pm);

        // Create temp directory
        const tempDir = await fsp.mkdtemp(path.join(os.tmpdir(), 'openrewrite-pm-'));

        try {
            // Create modified package.json with the new version constraint and/or overrides
            const modifiedPackageJson = this.createModifiedPackageJson(
                updateInfo.originalPackageJson,
                updateInfo
            );

            // Write modified package.json to temp directory
            await fsp.writeFile(path.join(tempDir, 'package.json'), modifiedPackageJson);

            // Copy existing lock file if present
            const originalLockPath = path.join(updateInfo.projectDir, lockFileName);
            if (fs.existsSync(originalLockPath)) {
                await fsp.copyFile(originalLockPath, path.join(tempDir, lockFileName));
            }

            // Copy config files if present (for registry configuration and workspace setup)
            const configFiles = ['.npmrc', '.yarnrc', '.yarnrc.yml', '.pnpmfile.cjs', 'pnpm-workspace.yaml'];
            for (const configFile of configFiles) {
                const configPath = path.join(updateInfo.projectDir, configFile);
                if (fs.existsSync(configPath)) {
                    await fsp.copyFile(configPath, path.join(tempDir, configFile));
                }
            }

            // Run package manager install to validate the version and update lock file
            const result = runInstall(pm, {
                cwd: tempDir,
                lockOnly: true,
                timeout: 120000 // 2 minute timeout
            });

            if (result.success) {
                // Store the modified package.json (we'll use our visitor for actual output)
                acc.updatedPackageJsons.set(updateInfo.packageJsonPath, modifiedPackageJson);

                // Read back the updated lock file
                const updatedLockPath = path.join(tempDir, lockFileName);
                if (fs.existsSync(updatedLockPath)) {
                    const updatedLockContent = await fsp.readFile(updatedLockPath, 'utf-8');
                    const lockFilePath = updateInfo.packageJsonPath.replace('package.json', lockFileName);
                    acc.updatedLockFiles.set(lockFilePath, updatedLockContent);
                }
            } else {
                // Track the failure - don't update package.json, the version likely doesn't exist
                const errorMessage = result.error || result.stderr || 'Unknown error';
                acc.failedProjects.set(updateInfo.packageJsonPath, errorMessage);
            }

        } finally {
            // Cleanup temp directory
            try {
                await fsp.rm(tempDir, {recursive: true, force: true});
            } catch {
                // Ignore cleanup errors
            }
        }
    }

    /**
     * Creates a modified package.json with the updated dependency version and/or overrides.
     * Used for the temp directory to validate the version exists.
     */
    private createModifiedPackageJson(
        originalContent: string,
        updateInfo: ProjectUpdateInfo
    ): string {
        let packageJson = JSON.parse(originalContent);

        // Update direct dependency if applicable
        if (updateInfo.updateDirect && updateInfo.dependencyScope) {
            if (packageJson[updateInfo.dependencyScope] &&
                packageJson[updateInfo.dependencyScope][this.packageName]) {
                packageJson[updateInfo.dependencyScope][this.packageName] = updateInfo.newVersion;
            }
        }

        // Add override if applicable
        if (updateInfo.addOverride) {
            packageJson = applyOverrideToPackageJson(
                packageJson,
                updateInfo.packageManager,
                this.packageName,
                updateInfo.newVersion,
                updateInfo.dependencyPathSegments
            );
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
        // If this is a transitive-only update (no direct dependency scope),
        // we don't need to update the marker's dependency lists since overrides
        // don't appear there. Just return the doc unchanged.
        if (!updateInfo.dependencyScope) {
            return doc;
        }

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
