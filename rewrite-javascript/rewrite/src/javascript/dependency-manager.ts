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

import {PackageManager} from "./node-resolution-result";

/**
 * Parsed dependency path for scoped overrides.
 * Segments represent the chain of dependencies, e.g., "express>accepts" becomes
 * [{name: "express"}, {name: "accepts"}]
 */
export interface DependencyPathSegment {
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
