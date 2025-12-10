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
    findNodeResolutionResult,
    npm,
    packageJson,
    packageLockJson,
    typescript,
    UpgradeDependencyVersion
} from "../../../src/javascript";
import {Json} from "../../../src/json";
import {RecipeSpec} from "../../../src/test";
import {withDir} from "tmp-promise";
import {findMarker, MarkersKind} from "../../../src";

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

    test("does not upgrade transitive-only dependencies", async () => {
        const spec = new RecipeSpec();
        // is-odd is a transitive dependency of is-even, not a direct dependency
        // UpgradeDependencyVersion should only upgrade direct dependencies
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "is-odd",
            newVersion: "^3.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    // No changes expected - is-odd is transitive only
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

});
