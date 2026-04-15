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
import {
    findNodeResolutionResult,
    npm,
    packageJson,
    packageLockJson,
    RemoveDependency,
    typescript
} from "../../../src/javascript";
import {Json} from "../../../src/json";
import {RecipeSpec} from "../../../src/test";
import {withDir} from "tmp-promise";

describe("RemoveDependency", () => {

    test("removes dependency from package.json", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new RemoveDependency({
            packageName: "lodash"
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
                                "uuid": "^9.0.0",
                                "lodash": "^4.17.21"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^9.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("does not modify when dependency does not exist", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new RemoveDependency({
            packageName: "lodash"
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
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("removes devDependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new RemoveDependency({
            packageName: "@types/node"
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
                            },
                            "devDependencies": {
                                "@types/node": "^20.0.0",
                                "typescript": "^5.0.0"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^9.0.0"
                            },
                            "devDependencies": {
                                "typescript": "^5.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("removes only from specified scope", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new RemoveDependency({
            packageName: "uuid",
            scope: "devDependencies"
        });

        await withDir(async (repo) => {
            // uuid exists in dependencies, not devDependencies — no change
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
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("removes last dependency and removes empty scope section", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new RemoveDependency({
            packageName: "uuid"
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
                            "version": "1.0.0"
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("updates NodeResolutionResult marker after removing dependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new RemoveDependency({
            packageName: "lodash"
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
                                "uuid": "^9.0.0",
                                "lodash": "^4.17.21"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^9.0.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                            const marker = findNodeResolutionResult(doc);
                            expect(marker).toBeDefined();

                            const lodashDep = marker!.dependencies.find(d => d.name === "lodash");
                            expect(lodashDep).toBeUndefined();

                            const uuidDep = marker!.dependencies.find(d => d.name === "uuid");
                            expect(uuidDep).toBeDefined();
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test("removes from all scopes when no scope specified", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new RemoveDependency({
            packageName: "uuid"
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
                                "uuid": "^9.0.0",
                                "lodash": "^4.17.21"
                            },
                            "devDependencies": {
                                "uuid": "^9.0.0"
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
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("updates package-lock.json when removing dependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new RemoveDependency({
            packageName: "lodash"
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
                                "uuid": "^9.0.0",
                                "lodash": "^4.17.21"
                            }
                        }
                    `, `
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "uuid": "^9.0.0"
                            }
                        }
                    `),
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
                                    "uuid": "^9.0.0",
                                    "lodash": "^4.17.21"
                                }
                            },
                            "node_modules/uuid": {
                                "version": "9.0.0",
                                "resolved": "https://registry.npmjs.org/uuid/-/uuid-9.0.0.tgz",
                                "integrity": "sha512-MXcSTerfPa4uqyzStbRoTgt5XIe3x5+42+q1sDuy3R5MDk66URdLMOZe5aPX/SQd+kuYAh0FdP/pO28IkQyTeg=="
                            },
                            "node_modules/lodash": {
                                "version": "4.17.21",
                                "resolved": "https://registry.npmjs.org/lodash/-/lodash-4.17.21.tgz",
                                "integrity": "sha512-v2kDEe57lecTulaDIuNTPy3Ry4gLGJ6Z1O3vE1krgXZNrsQ+LFTGHVxVjcXPs17LhbZVGedAJv8XZ1tvj5FvSg=="
                            }
                        }
                    }
                    `, (actual: string) => {
                        const lockData = JSON.parse(actual);

                        if (!lockData.packages) {
                            throw new Error("Expected packages in lock file");
                        }

                        const rootPkg = lockData.packages[""];
                        expect(rootPkg?.dependencies?.["lodash"]).toBeUndefined();
                        expect(rootPkg?.dependencies?.["uuid"]).toBe("^9.0.0");

                        expect(lockData.packages["node_modules/lodash"]).toBeUndefined();

                        return actual;
                    })
                )
            );
        }, {unsafeCleanup: true});
    });
});
