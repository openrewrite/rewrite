/*
 * Copyright 2026 the original author or authors.
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
import {describe, expect, test} from "vitest";
import {
    createNodeResolutionResultMarker,
    PackageLockContent,
    ResolvedDependency
} from "../../src/javascript/node-resolution-result";

/**
 * Transitive dependency resolution works for both the modern `packages` map
 * (npm lockfileVersion 2/3) and the legacy `dependencies` tree (lockfileVersion 1,
 * npm 5/6). Both lock file shapes describe the same install, so DependencyInsight and
 * its TypeScript twin FindDependency can walk transitive dependencies from either one.
 */
describe("npm lock file transitive resolution", () => {

    const packageJsonContent = {
        name: "my-app",
        version: "1.0.0",
        dependencies: {
            express: "^4.18.2"
        }
    };

    // The same `express` -> `accepts` -> `mime-types` graph, expressed once as a legacy v1
    // lock file and once as a modern (v2/v3) lock file.
    const legacyV1Lock = {
        name: "my-app",
        version: "1.0.0",
        lockfileVersion: 1,
        requires: true,
        dependencies: {
            express: {
                version: "4.18.2",
                resolved: "https://registry.npmjs.org/express/-/express-4.18.2.tgz",
                integrity: "sha512-5/PsL6iGPdfQ/lKM1UuielYgv3BUoJfz1aUwU9vHZ+J7gyvwdQXFEBIEIaxeGf0GIcreATNyBExtalisDbuMqQ==",
                requires: {accepts: "~1.3.8"}
            },
            accepts: {
                version: "1.3.8",
                resolved: "https://registry.npmjs.org/accepts/-/accepts-1.3.8.tgz",
                integrity: "sha512-PYAthTa2m2VKxuvSD3DPC/Gy+U+sOA1LAuT8mkmRuvw+NACSaeXEQ+NHcVF7rONl6qcaxV3Uuemwawk+7+SJLw==",
                requires: {"mime-types": "~2.1.34"}
            },
            "mime-types": {
                version: "2.1.35",
                resolved: "https://registry.npmjs.org/mime-types/-/mime-types-2.1.35.tgz",
                integrity: "sha512-ZDY+bPm5zTTF+YpCrAU9nK0UgICYPT0QtT1NZWFv4s++TNkcgVaT0g6+4R2uI4MjQjzysHB1zxuWL50hzaeXiw=="
            }
        }
    } as unknown as PackageLockContent;

    const modernLock: PackageLockContent = {
        name: "my-app",
        version: "1.0.0",
        lockfileVersion: 3,
        packages: {
            "": {
                version: "1.0.0",
                dependencies: {express: "^4.18.2"}
            },
            "node_modules/express": {
                version: "4.18.2",
                dependencies: {accepts: "~1.3.8"}
            },
            "node_modules/accepts": {
                version: "1.3.8",
                dependencies: {"mime-types": "~2.1.34"}
            },
            "node_modules/mime-types": {
                version: "2.1.35"
            }
        }
    };

    function resolveTransitive(resolved: ResolvedDependency | undefined, target: string,
                               visited: Set<string> = new Set()): ResolvedDependency | undefined {
        if (!resolved) return undefined;
        const key = `${resolved.name}@${resolved.version}`;
        if (visited.has(key)) return undefined;
        visited.add(key);
        for (const dep of resolved.dependencies || []) {
            if (dep.name === target) return dep.resolved;
            const found = resolveTransitive(dep.resolved, target, visited);
            if (found) return found;
        }
        return undefined;
    }

    // The same express -> accepts -> mime-types graph resolves identically from the legacy
    // v1 `dependencies` tree and the modern v2/v3 `packages` map.
    test.each([
        {label: "legacy lock file (lockfileVersion 1)", lock: legacyV1Lock},
        {label: "modern lock file (lockfileVersion 2/3)", lock: modernLock},
    ])("resolves the express -> accepts -> mime-types chain from the $label", ({lock}) => {
        const marker = createNodeResolutionResultMarker("package.json", packageJsonContent, lock);

        expect(marker.resolvedDependencies.length).toBeGreaterThan(0);
        const express = marker.dependencies.find(d => d.name === "express");
        expect(express?.resolved?.version).toBe("4.18.2");
        expect(resolveTransitive(express?.resolved, "mime-types")?.version).toBe("2.1.35");
    });

    test("legacy v1 lock file resolves nested conflicting versions by node_modules path", () => {
        // `a` and `b` both require `shared`, but at incompatible versions. npm 5/6 keeps
        // `shared@1.0.0` at the top level (for `a`) and nests `shared@2.0.0` under `b`.
        const nestedV1Lock = {
            lockfileVersion: 1,
            requires: true,
            dependencies: {
                a: {version: "1.0.0", requires: {shared: "^1.0.0"}},
                b: {
                    version: "1.0.0",
                    requires: {shared: "^2.0.0"},
                    dependencies: {
                        shared: {version: "2.0.0"}
                    }
                },
                shared: {version: "1.0.0"}
            }
        } as unknown as PackageLockContent;

        const marker = createNodeResolutionResultMarker(
            "package.json",
            {name: "my-app", version: "1.0.0", dependencies: {a: "^1.0.0", b: "^1.0.0"}},
            nestedV1Lock
        );

        const a = marker.dependencies.find(d => d.name === "a");
        const b = marker.dependencies.find(d => d.name === "b");
        expect(a?.resolved?.dependencies?.find(d => d.name === "shared")?.resolved?.version).toBe("1.0.0");
        expect(b?.resolved?.dependencies?.find(d => d.name === "shared")?.resolved?.version).toBe("2.0.0");
    });

    test("legacy v1 lock file resolves scoped packages, including a nested scoped version", () => {
        // A scoped key like `@scope/util` must keep its full name (the leading `@` is not a
        // version separator), at both the top level and nested under another package.
        const scopedV1Lock = {
            lockfileVersion: 1,
            requires: true,
            dependencies: {
                "@scope/app": {
                    version: "1.0.0",
                    requires: {"@scope/util": "^2.0.0"},
                    dependencies: {
                        "@scope/util": {version: "2.9.9"}
                    }
                },
                "@scope/util": {version: "2.0.0"}
            }
        } as unknown as PackageLockContent;

        const marker = createNodeResolutionResultMarker(
            "package.json",
            {name: "my-app", version: "1.0.0", dependencies: {"@scope/app": "^1.0.0", "@scope/util": "^2.0.0"}},
            scopedV1Lock
        );

        const app = marker.dependencies.find(d => d.name === "@scope/app");
        expect(app?.resolved?.version).toBe("1.0.0");
        // The direct scoped dep resolves top-level (2.0.0); the one required by @scope/app
        // resolves to the version nested under it (2.9.9).
        expect(marker.dependencies.find(d => d.name === "@scope/util")?.resolved?.version).toBe("2.0.0");
        expect(app?.resolved?.dependencies?.find(d => d.name === "@scope/util")?.resolved?.version).toBe("2.9.9");
        const utilVersions = marker.resolvedDependencies
            .filter(d => d.name === "@scope/util")
            .map(d => d.version)
            .sort();
        expect(utilVersions).toEqual(["2.0.0", "2.9.9"]);
    });

    test("modern v2 lock file prefers the `packages` map over a legacy `dependencies` tree", () => {
        // A v2 lock file carries both maps. The `packages` map wins; the stale versions in
        // the legacy tree must be ignored.
        const v2Lock = {
            ...modernLock,
            lockfileVersion: 2,
            dependencies: {
                express: {version: "4.0.0", requires: {accepts: "~1.0.0"}},
                accepts: {version: "1.0.0"}
            }
        } as unknown as PackageLockContent;

        const marker = createNodeResolutionResultMarker("package.json", packageJsonContent, v2Lock);

        const express = marker.dependencies.find(d => d.name === "express");
        expect(express?.resolved?.version).toBe("4.18.2");
        expect(resolveTransitive(express?.resolved, "mime-types")?.version).toBe("2.1.35");
    });

    test("an empty dependency tree yields no resolved dependencies", () => {
        const emptyV1Lock = {lockfileVersion: 1, requires: true, dependencies: {}} as unknown as PackageLockContent;
        const marker = createNodeResolutionResultMarker("package.json", packageJsonContent, emptyV1Lock);
        expect(marker.resolvedDependencies).toHaveLength(0);
    });

    test("falls back to the legacy `dependencies` tree when the `packages` map is empty", () => {
        const emptyPackagesLock = {
            lockfileVersion: 2,
            packages: {},
            dependencies: {
                express: {version: "4.18.2"}
            }
        } as unknown as PackageLockContent;
        const marker = createNodeResolutionResultMarker("package.json", packageJsonContent, emptyPackagesLock);
        expect(marker.dependencies.find(d => d.name === "express")?.resolved?.version).toBe("4.18.2");
    });
});
