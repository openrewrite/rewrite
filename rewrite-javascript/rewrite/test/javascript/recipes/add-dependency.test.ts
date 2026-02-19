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
    AddDependency,
    findNodeResolutionResult,
    npm,
    packageJson,
    packageLockJson,
    typescript
} from "../../../src/javascript";
import {Json} from "../../../src/json";
import {RecipeSpec} from "../../../src/test";
import {withDir} from "tmp-promise";
import {findMarker, MarkersKind} from "../../../src";

describe("AddDependency", () => {

    test("adds dependency to package.json", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AddDependency({
            packageName: "lodash",
            version: "^4.17.21"
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
                                "uuid": "^9.0.0",
                                "lodash": "^4.17.21"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    // This is the same behavior as org.openrewrite.maven.AddDependency
    test("does not modify when dependency already exists", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AddDependency({
            packageName: "uuid",
            version: "^10.0.0"
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

    test("adds devDependency when scope is specified", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AddDependency({
            packageName: "@types/lodash",
            version: "^4.17.0",
            scope: "devDependencies"
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
                                "uuid": "^9.0.0"
                            },
                            "devDependencies": {
                                "@types/lodash": "^4.17.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("adds to existing devDependencies section", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AddDependency({
            packageName: "@types/lodash",
            version: "^4.17.0",
            scope: "devDependencies"
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
                                "@types/node": "^20.0.0",
                                "@types/lodash": "^4.17.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("updates NodeResolutionResult marker after adding dependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AddDependency({
            packageName: "lodash",
            version: "^4.17.21"
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
                                "uuid": "^9.0.0",
                                "lodash": "^4.17.21"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                            const marker = findNodeResolutionResult(doc);
                            expect(marker).toBeDefined();
                            expect(marker!.dependencies).toHaveLength(2);

                            const lodashDep = marker!.dependencies.find(d => d.name === "lodash");
                            expect(lodashDep).toBeDefined();
                            expect(lodashDep!.versionConstraint).toBe("^4.17.21");
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test("adds peerDependency when scope is specified", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AddDependency({
            packageName: "react",
            version: "^18.0.0",
            scope: "peerDependencies"
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
                                "uuid": "^9.0.0"
                            },
                            "peerDependencies": {
                                "react": "^18.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("adds warning marker when package does not exist", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AddDependency({
            packageName: "this-package-does-not-exist-12345",
            version: "^1.0.0"
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
                    `, (actual: string) => {
                            expect(actual).toContain('/*~~(Failed to add this-package-does-not-exist-12345');
                            return actual;
                        }), afterRecipe: async (doc: Json.Document) => {
                            const warnMarker = findMarker(doc, MarkersKind.MarkupWarn);
                            expect(warnMarker).toBeDefined();
                            expect((warnMarker as any).message).toContain("Failed to add this-package-does-not-exist-12345");
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test("updates package-lock.json when adding dependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AddDependency({
            packageName: "lodash",
            version: "^4.17.21"
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
                                "uuid": "^9.0.0",
                                "lodash": "^4.17.21"
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

                        if (!lockData.packages) {
                            throw new Error("Expected packages in lock file");
                        }

                        // The root package should now include lodash
                        const rootPkg = lockData.packages[""];
                        if (rootPkg?.dependencies?.["lodash"] !== "^4.17.21") {
                            throw new Error(`Expected root dependency lodash to be ^4.17.21, got ${rootPkg?.dependencies?.["lodash"]}`);
                        }

                        // lodash should be in node_modules
                        const lodashPkg = lockData.packages["node_modules/lodash"];
                        if (!lodashPkg?.version?.startsWith("4.17.")) {
                            throw new Error(`Expected lodash version to start with 4.17., got ${lodashPkg?.version}`);
                        }

                        return actual;
                    })
                )
            );
        }, {unsafeCleanup: true});
    });

    // Note - to be honest, I am not sure if this is the desired behavior
    test("does not add if dependency exists in different scope", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AddDependency({
            packageName: "uuid",
            version: "^10.0.0",
            scope: "devDependencies"
        });

        await withDir(async (repo) => {
            // uuid exists in dependencies, so shouldn't add to devDependencies
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
});
