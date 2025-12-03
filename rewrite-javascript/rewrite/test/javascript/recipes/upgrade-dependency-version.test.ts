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

describe("UpgradeDependencyVersion", () => {

    test("upgrades dependency version in package.json", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeDependencyVersion({
            packageName: "is-number",
            newVersion: "^8.0.0"
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
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "is-number": "^8.0.0"
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
            packageName: "is-number",
            newVersion: "^8.0.0"
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
                                "is-number": "^7.0.0"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "devDependencies": {
                                "is-number": "^8.0.0"
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
            packageName: "is-number",
            newVersion: "^8.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "is-number": "^7.0.0"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "is-number": "^8.0.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                        const marker = findNodeResolutionResult(doc);
                        expect(marker).toBeDefined();
                        expect(marker!.dependencies).toHaveLength(1);
                        expect(marker!.dependencies[0].name).toBe("is-number");
                        expect(marker!.dependencies[0].versionConstraint).toBe("^8.0.0");
                    }}
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

});
