/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderate-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Option, Recipe} from "../../recipe";
import {ExecutionContext} from "../../execution";
import {TreeVisitor} from "../../visitor";
import {Json, JsonVisitor} from "../../json";
import {foundSearchResult} from "../../markers";
import {
    Dependency,
    findNodeResolutionResult,
    NodeResolutionResult,
    ResolvedDependency
} from "../node-resolution-result";
import * as semver from "semver";
import * as picomatch from "picomatch";

/** Dependency section names in package.json */
const DEPENDENCY_SECTIONS = new Set([
    'dependencies',
    'devDependencies',
    'peerDependencies',
    'optionalDependencies'
]);

/**
 * Finds npm/Node.js dependencies declared in package.json.
 * This recipe is commonly used as a precondition to limit the scope of other recipes
 * to projects that use a specific dependency.
 *
 * The search result marker is placed on the specific dependency entry in package.json,
 * allowing users to see exactly where the dependency is declared.
 *
 * When `onlyDirect` is false, this recipe also marks direct dependencies that
 * transitively depend on the target package, helping answer "which of my dependencies
 * brings in package X?".
 */
export interface FindDependencyOptions {
    packageName: string;
    version?: string;
    onlyDirect?: boolean;
}

export class FindDependency extends Recipe {
    readonly name = "org.openrewrite.javascript.dependencies.find-dependency";
    readonly displayName = "Find Node.js dependency";
    readonly description = "Finds dependencies in a project's `package.json`. " +
        "Can find both direct dependencies and dependencies that transitively include the target package. " +
        "This recipe is commonly used as a precondition for other recipes.";

    @Option({
        displayName: "Package name",
        description: "The name of the npm package to find. Supports glob patterns.",
        example: "lodash"
    })
    packageName!: string;

    @Option({
        displayName: "Version",
        description: "An exact version number or semver selector used to select the version number. " +
            "Leave empty to match any version.",
        example: "^18.0.0",
        required: false
    })
    version?: string;

    @Option({
        displayName: "Only direct dependencies",
        description: "If true (default), only matches dependencies that directly match the package name. " +
            "If false, also marks direct dependencies that have the target package as a transitive dependency.",
        example: "true",
        required: false
    })
    onlyDirect?: boolean;

    constructor(options: FindDependencyOptions) {
        super(options);
        // Handle string values from RPC serialization (e.g., "false" instead of false)
        // and default to true if not specified
        this.onlyDirect = !(this.onlyDirect === false || (this.onlyDirect as any === "false"));
    }

    override instanceName(): string {
        return `${this.displayName} \`${this.packageName}${this.version ? '@' + this.version : ''}\``;
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const packageName = this.packageName;
        const version = this.version;
        const onlyDirect = this.onlyDirect;

        // Create a picomatch matcher for the package name pattern
        // For patterns without '/', use { contains: true } so that '*jest*' matches '@types/jest'
        // (by default, '*' doesn't match '/' in glob patterns, but for package names this is more intuitive)
        const matchOptions = packageName.includes('/') ? {} : { contains: true };
        const matcher: picomatch.Matcher = picomatch.default
            ? picomatch.default(packageName, matchOptions)
            : (picomatch as any)(packageName, matchOptions);

        return new class extends JsonVisitor<ExecutionContext> {
            private resolution: NodeResolutionResult | undefined;
            private isPackageJson: boolean = false;

            protected override async visitDocument(document: Json.Document, ctx: ExecutionContext): Promise<Json | undefined> {
                // Only process package.json files, not package-lock.json or other JSON files
                const sourcePath = document.sourcePath;
                this.isPackageJson = sourcePath.endsWith('package.json');

                if (this.isPackageJson) {
                    this.resolution = findNodeResolutionResult(document);
                }

                return this.isPackageJson && this.resolution ? super.visitDocument(document, ctx) : document;
            }

            protected override async visitMember(member: Json.Member, ctx: ExecutionContext): Promise<Json | undefined> {
                // Check if we're inside a dependency section
                const parentSection = this.getParentDependencySection();
                if (!parentSection) {
                    return super.visitMember(member, ctx);
                }

                // Get the package name from the member key
                const depName = this.getMemberKeyName(member);
                if (!depName) {
                    return super.visitMember(member, ctx);
                }

                // Find the dependency in the resolution result
                const dep = this.findDependencyByName(depName, parentSection);
                if (!dep) {
                    return super.visitMember(member, ctx);
                }

                // Check if this dependency matches directly
                if (matcher(depName) && versionMatches(dep, version)) {
                    return this.markDependency(member, ctx);
                }

                // If not only direct, check if this dependency has the target as a transitive dependency
                if (!onlyDirect && dep.resolved) {
                    if (hasTransitiveDependency(dep.resolved, matcher, version, new Set())) {
                        return this.markDependency(member, ctx);
                    }
                }

                return super.visitMember(member, ctx);
            }

            /**
             * Marks the dependency key with a search result marker.
             */
            private async markDependency(member: Json.Member, ctx: ExecutionContext): Promise<Json.Member> {
                const visitedMember = await super.visitMember(member, ctx) as Json.Member;
                const markedKey = foundSearchResult(visitedMember.key.element);
                return {
                    ...visitedMember,
                    key: {
                        ...visitedMember.key,
                        element: markedKey
                    }
                } as Json.Member;
            }

            /**
             * Checks if the current member's parent is a dependency section object.
             * Returns the section name if so, undefined otherwise.
             */
            private getParentDependencySection(): string | undefined {
                // Walk up the cursor to find the parent member that contains this dependency
                // Structure: Document > Object > Member("dependencies") > Object > Member("lodash")
                let cursor = this.cursor.parent;
                while (cursor) {
                    const tree = cursor.value;
                    if (tree && typeof tree === 'object' && 'kind' in tree) {
                        if (tree.kind === Json.Kind.Member) {
                            const memberKey = this.getMemberKeyName(tree as Json.Member);
                            if (memberKey && DEPENDENCY_SECTIONS.has(memberKey)) {
                                return memberKey;
                            }
                        }
                    }
                    cursor = cursor.parent;
                }
                return undefined;
            }

            /**
             * Extracts the key name from a Json.Member
             */
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

            /**
             * Finds a dependency by name in the appropriate section of the resolution result.
             */
            private findDependencyByName(name: string, section: string): Dependency | undefined {
                if (!this.resolution) return undefined;

                let deps: Dependency[] | undefined;
                switch (section) {
                    case 'dependencies':
                        deps = this.resolution.dependencies;
                        break;
                    case 'devDependencies':
                        deps = this.resolution.devDependencies;
                        break;
                    case 'peerDependencies':
                        deps = this.resolution.peerDependencies;
                        break;
                    case 'optionalDependencies':
                        deps = this.resolution.optionalDependencies;
                        break;
                }

                return deps?.find(d => d.name === name);
            }
        };
    }
}

/**
 * Recursively checks if a resolved dependency has the target package as a transitive dependency.
 */
function hasTransitiveDependency(
    resolved: ResolvedDependency,
    matcher: picomatch.Matcher,
    version: string | undefined,
    visited: Set<string>
): boolean {
    // Avoid cycles
    const key = `${resolved.name}@${resolved.version}`;
    if (visited.has(key)) {
        return false;
    }
    visited.add(key);

    // Check all dependency types
    const allDeps = [
        ...(resolved.dependencies || []),
        ...(resolved.devDependencies || []),
        ...(resolved.peerDependencies || []),
        ...(resolved.optionalDependencies || [])
    ];

    for (const dep of allDeps) {
        // Check if this dependency matches the target
        if (matcher(dep.name) && versionMatches(dep, version)) {
            return true;
        }

        // Recursively check transitive dependencies
        if (dep.resolved && hasTransitiveDependency(dep.resolved, matcher, version, visited)) {
            return true;
        }
    }

    return false;
}

function versionMatches(dep: Dependency, version: string | undefined): boolean {
    if (!version) {
        return true;
    }
    const resolved = dep.resolved;
    if (!resolved) {
        // If no resolved version available, we can't validate the version
        return false;
    }
    return versionMatchesResolved(resolved, version);
}

function versionMatchesResolved(resolved: ResolvedDependency, version: string | undefined): boolean {
    if (!version) {
        return true;
    }
    const actualVersion = resolved.version;
    // Use semver.satisfies to check if the actual version matches the constraint
    return semver.satisfies(actualVersion, version);
}
