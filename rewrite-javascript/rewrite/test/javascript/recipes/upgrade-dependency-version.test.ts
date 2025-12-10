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
import {
    applyOverrideToPackageJson,
    findNodeResolutionResult,
    npm,
    packageJson,
    packageLockJson,
    PackageManager,
    parseDependencyPath,
    typescript,
    UpgradeDependencyVersion
} from "../../../src/javascript";
import {Json} from "../../../src/json";
import {RecipeSpec} from "../../../src/test";
import {withDir} from "tmp-promise";
import {findMarker, MarkersKind} from "../../../src/markers";

describe("UpgradeDependencyVersion", () => {

    test("upgrades dependency version in package.json", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "uuid",
            newVersion: "^10.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^9.0.0"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^10.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("does not modify when dependency not present", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "nonexistent-package",
            newVersion: "^2.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "is-number": "^7.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("does not modify when version already matches", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "is-number",
            newVersion: "^7.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "is-number": "^7.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("upgrades devDependency version", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "uuid",
            newVersion: "^10.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "devDependencies": {
                                "uuid": "^9.0.0"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "devDependencies": {
                                "uuid": "^10.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("upgrades scoped package version", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "@types/node",
            newVersion: "^22.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "devDependencies": {
                                "@types/node": "^20.0.0"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "devDependencies": {
                                "@types/node": "^22.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("updates NodeResolutionResult marker after upgrade", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "uuid",
            newVersion: "^10.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {
                        ...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^9.0.0"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^10.0.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                            const marker = findNodeResolutionResult(doc);
                            expect(marker).toBeDefined();
                            expect(marker!.dependencies).toHaveLength(1);
                            expect(marker!.dependencies[0].name).toBe("uuid");
                            expect(marker!.dependencies[0].versionConstraint).toBe("^10.0.0");
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test("handles peerDependencies", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "react",
            newVersion: "^19.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "peerDependencies": {
                                "react": "^18.0.0"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "peerDependencies": {
                                "react": "^19.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("handles optionalDependencies", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "fsevents",
            newVersion: "^3.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "optionalDependencies": {
                                "fsevents": "^2.3.0"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "optionalDependencies": {
                                "fsevents": "^3.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("adds warning marker when version does not exist", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "uuid",
            newVersion: "^999.0.0" // Non-existent version
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {
                        // Version doesn't change, but warning marker is added
                        ...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^9.0.0"
                            }
                        }
                    `, `
                        /*~~(Failed to upgrade uuid to ^999.0.0)~~>*/{
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^9.0.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                            // Should have a warning marker
                            const warnMarker = findMarker(doc, MarkersKind.MarkupWarn);
                            expect(warnMarker).toBeDefined();
                            expect((warnMarker as any).message).toContain("Failed to upgrade uuid");
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test("updates package-lock.json when upgrading dependency", async () => {
        const spec = new RecipeSpec();
        // Use uuid which has real version 9.x and 10.x available
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "uuid",
            newVersion: "^10.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^9.0.0"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^10.0.0"
                            }
                        }
                    `),
                    // Use validation function for lock file - returns actual if valid, error message if not
                    packageLockJson(`
                    {
                        "name": "test-project",
                        "version": "1.0.0",
                        "lockfileVersion": 3,
                        "requires": true,
                        "packages": {
                            "": {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                    "uuid": "^9.0.0"
                                }
                            },
                            "node_modules/uuid": {
                                "version": "9.0.0",
                                "resolved": "https://registry.npmjs.org/uuid/-/uuid-9.0.0.tgz",
                                "integrity": "sha512-MXcSTerfPa4uqyzStbRoTgt5XIe3x5+42+q1sDuy3R5MDk66URdLMOZe5aPX/SQd+kuYAh0FdP/pO28IkQyTeg=="
                            }
                        }
                    }
                    `, (actual: string) => {
                        const lockData = JSON.parse(actual);

                        // Verify lock file structure
                        if (!lockData.packages) {
                            throw new Error("Expected packages in lock file");
                        }

                        // The root package should now reference ^10.0.0
                        const rootPkg = lockData.packages[""];
                        if (rootPkg?.dependencies?.["uuid"] !== "^10.0.0") {
                            throw new Error(`Expected root dependency uuid to be ^10.0.0, got ${rootPkg?.dependencies?.["uuid"]}`);
                        }

                        // The resolved package should be version 10.x
                        const uuidPkg = lockData.packages["node_modules/uuid"];
                        if (!uuidPkg?.version?.startsWith("10.")) {
                            throw new Error(`Expected uuid version to start with 10., got ${uuidPkg?.version}`);
                        }

                        return actual;
                    })
                )
            );
        }, {unsafeCleanup: true});
    });

    test("preserves original formatting (4-space indentation, trailing newline)", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "uuid",
            newVersion: "^10.0.0"
        });

        await withDir(async (repo) => {
            // Note: 4-space indentation and trailing newline in the input
            const before = `{
    "name": "test-project",
    "version": "1.0.0",
    "dependencies": {
        "uuid": "^9.0.0"
    }
}
`;
            const after = `{
    "name": "test-project",
    "version": "1.0.0",
    "dependencies": {
        "uuid": "^10.0.0"
    }
}
`;
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(before, after)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("skips npm install when resolved version already satisfies new constraint", async () => {
        // Scenario: package.json has ^4.17.20, npm resolves to 4.17.21 (latest)
        // If we upgrade to ^4.17.21, the resolved version 4.17.21 already satisfies it,
        // so we should only update package.json, not run npm install
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "lodash",
            newVersion: "^4.17.21"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    // package.json should be updated from ^4.17.20 to ^4.17.21
                    {
                        ...packageJson(`
                            {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                    "lodash": "^4.17.20"
                                }
                            }
                        `, `
                            {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                    "lodash": "^4.17.21"
                                }
                            }
                        `),
                        afterRecipe: async (doc: Json.Document) => {
                            // Verify marker was updated with new versionConstraint
                            const marker = findNodeResolutionResult(doc);
                            expect(marker).toBeDefined();
                            expect(marker!.dependencies[0].versionConstraint).toBe("^4.17.21");
                            // The resolved version should still be 4.17.21 (unchanged)
                            // This proves we didn't run npm install - just updated the constraint
                            expect(marker!.resolvedDependencies?.[0]?.version).toBe("4.17.21");
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    describe("shouldUpgrade semver comparison", () => {

        test("should not upgrade when versions are identical", () => {
            const recipe = new UpgradeDependencyVersion({
                packageName: "test",
                newVersion: "^1.0.0"
            });
            expect(recipe.shouldUpgrade("^1.0.0", "^1.0.0")).toBe(false);
        });

        test("should upgrade when new version is higher", () => {
            const recipe = new UpgradeDependencyVersion({
                packageName: "test",
                newVersion: "^2.0.0"
            });
            expect(recipe.shouldUpgrade("^1.0.0", "^2.0.0")).toBe(true);
        });

        test("should not downgrade when new version is lower", () => {
            const recipe = new UpgradeDependencyVersion({
                packageName: "test",
                newVersion: "^1.0.0"
            });
            expect(recipe.shouldUpgrade("^2.0.0", "^1.0.0")).toBe(false);
        });

        test("should upgrade with tilde ranges", () => {
            const recipe = new UpgradeDependencyVersion({
                packageName: "test",
                newVersion: "~1.1.0"
            });
            expect(recipe.shouldUpgrade("~1.0.0", "~1.1.0")).toBe(true);
        });

        test("should upgrade with exact versions", () => {
            const recipe = new UpgradeDependencyVersion({
                packageName: "test",
                newVersion: "2.0.0"
            });
            expect(recipe.shouldUpgrade("1.0.0", "2.0.0")).toBe(true);
        });

    });

    describe("override policies", () => {

        test("Transitive policy adds override for transitive-only dependency (npm)", async () => {
            const spec = new RecipeSpec();
            // is-odd is a transitive dependency of is-even@1.0.0 (depends on is-odd@^0.1.2)
            // Upgrading to ^3.0.0 which exists
            spec.recipe = new UpgradeDependencyVersion({
                packageName: "is-odd",
                newVersion: "^3.0.0",
                upgradePolicy: "Transitive"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(`const x = 1;`),
                        packageJson(`
                            {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                    "is-even": "^1.0.0"
                                }
                            }
                        `, (actual: string) => {
                            const pkg = JSON.parse(actual);
                            // Should have added overrides section
                            expect(pkg.overrides).toBeDefined();
                            expect(pkg.overrides["is-odd"]).toBe("^3.0.0");
                            // Should NOT have modified dependencies
                            expect(pkg.dependencies["is-even"]).toBe("^1.0.0");
                            return actual;
                        })
                    )
                );
            }, {unsafeCleanup: true});
        });

        test("Transitive policy with dependencyPath adds scoped override (npm)", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new UpgradeDependencyVersion({
                packageName: "is-odd",
                newVersion: "^3.0.0",
                upgradePolicy: "Transitive",
                dependencyPath: "is-even"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(`const x = 1;`),
                        packageJson(`
                            {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                    "is-even": "^1.0.0"
                                }
                            }
                        `, (actual: string) => {
                            const pkg = JSON.parse(actual);
                            // Should have nested override structure for npm
                            expect(pkg.overrides).toBeDefined();
                            expect(pkg.overrides["is-even"]).toBeDefined();
                            expect(pkg.overrides["is-even"]["is-odd"]).toBe("^3.0.0");
                            return actual;
                        })
                    )
                );
            }, {unsafeCleanup: true});
        });

        test("Direct policy does not add override for transitive dependency", async () => {
            const spec = new RecipeSpec();
            // Default policy is Direct, which should NOT add overrides
            spec.recipe = new UpgradeDependencyVersion({
                packageName: "is-odd",
                newVersion: "^3.0.0"
                // upgradePolicy defaults to "Direct"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(`const x = 1;`),
                        // No changes expected - is-odd is transitive only and policy is Direct
                        packageJson(`
                            {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                    "is-even": "^1.0.0"
                                }
                            }
                        `)
                    )
                );
            }, {unsafeCleanup: true});
        });

        test("DirectAndTransitive policy upgrades direct dep AND adds override", async () => {
            const spec = new RecipeSpec();
            // is-number@7.0.0 is the latest version
            // is-odd@3.0.0 depends on is-number@^6.0.0
            // So if we have is-number@^6.0.0 as direct and is-odd@^3.0.0,
            // upgrading is-number to ^7.0.0 should update direct and add override
            spec.recipe = new UpgradeDependencyVersion({
                packageName: "is-number",
                newVersion: "^7.0.0",
                upgradePolicy: "DirectAndTransitive"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(`const x = 1;`),
                        // is-number is both direct AND transitive (via is-odd -> is-number)
                        packageJson(`
                            {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                    "is-number": "^6.0.0",
                                    "is-odd": "^3.0.0"
                                }
                            }
                        `, (actual: string) => {
                            const pkg = JSON.parse(actual);
                            // Should have upgraded direct dependency
                            expect(pkg.dependencies["is-number"]).toBe("^7.0.0");
                            // Should have added override for transitive usage
                            expect(pkg.overrides).toBeDefined();
                            expect(pkg.overrides["is-number"]).toBe("^7.0.0");
                            return actual;
                        })
                    )
                );
            }, {unsafeCleanup: true});
        });

    });

    describe("parseDependencyPath", () => {

        test("parses simple path with > separator", () => {
            const result = parseDependencyPath("express>accepts");
            expect(result).toEqual([
                {name: "express"},
                {name: "accepts"}
            ]);
        });

        test("parses simple path with / separator", () => {
            const result = parseDependencyPath("express/accepts");
            expect(result).toEqual([
                {name: "express"},
                {name: "accepts"}
            ]);
        });

        test("parses path with version constraint", () => {
            const result = parseDependencyPath("express@4.0.0>accepts");
            expect(result).toEqual([
                {name: "express", version: "4.0.0"},
                {name: "accepts"}
            ]);
        });

        test("parses scoped package", () => {
            const result = parseDependencyPath("@scope/pkg>dep");
            expect(result).toEqual([
                {name: "@scope/pkg"},
                {name: "dep"}
            ]);
        });

        test("parses scoped package with version", () => {
            const result = parseDependencyPath("@scope/pkg@1.0.0>dep");
            expect(result).toEqual([
                {name: "@scope/pkg", version: "1.0.0"},
                {name: "dep"}
            ]);
        });

        test("parses multi-level path", () => {
            const result = parseDependencyPath("a>b>c");
            expect(result).toEqual([
                {name: "a"},
                {name: "b"},
                {name: "c"}
            ]);
        });

    });

    describe("applyOverrideToPackageJson", () => {

        test("npm: global override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Npm,
                "lodash",
                "^4.17.21"
            );
            expect(result.overrides).toEqual({lodash: "^4.17.21"});
        });

        test("npm: scoped override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Npm,
                "accepts",
                "^2.0.0",
                [{name: "express"}]
            );
            expect(result.overrides).toEqual({
                express: {accepts: "^2.0.0"}
            });
        });

        test("npm: nested scoped override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Npm,
                "mime-types",
                "^3.0.0",
                [{name: "express"}, {name: "accepts"}]
            );
            expect(result.overrides).toEqual({
                express: {accepts: {"mime-types": "^3.0.0"}}
            });
        });

        test("npm: merges with existing overrides", () => {
            const result = applyOverrideToPackageJson(
                {name: "test", overrides: {lodash: "^4.17.20"}},
                PackageManager.Npm,
                "underscore",
                "^1.13.0"
            );
            expect(result.overrides).toEqual({
                lodash: "^4.17.20",
                underscore: "^1.13.0"
            });
        });

        test("yarn: global resolution", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.YarnClassic,
                "lodash",
                "^4.17.21"
            );
            expect(result.resolutions).toEqual({lodash: "^4.17.21"});
        });

        test("yarn: scoped resolution", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.YarnBerry,
                "accepts",
                "^2.0.0",
                [{name: "express"}]
            );
            expect(result.resolutions).toEqual({"express/accepts": "^2.0.0"});
        });

        test("pnpm: global override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Pnpm,
                "lodash",
                "^4.17.21"
            );
            expect(result.pnpm.overrides).toEqual({lodash: "^4.17.21"});
        });

        test("pnpm: scoped override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Pnpm,
                "accepts",
                "^2.0.0",
                [{name: "express"}]
            );
            expect(result.pnpm.overrides).toEqual({"express>accepts": "^2.0.0"});
        });

        test("pnpm: multi-level scoped override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Pnpm,
                "mime-types",
                "^3.0.0",
                [{name: "express"}, {name: "accepts"}]
            );
            expect(result.pnpm.overrides).toEqual({"express>accepts>mime-types": "^3.0.0"});
        });

        test("bun: uses npm-style overrides", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Bun,
                "lodash",
                "^4.17.21"
            );
            expect(result.overrides).toEqual({lodash: "^4.17.21"});
        });

    });

});
