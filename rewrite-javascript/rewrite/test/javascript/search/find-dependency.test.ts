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
    FindDependency,
    npm,
    packageJson,
    packageLockJson,
    typescript
} from "../../../src/javascript";
import {RecipeSpec} from "../../../src/test";
import {withDir} from "tmp-promise";

describe("FindDependency", () => {

    test("should find direct dependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "lodash"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(
                        `{
                            "name": "test-project",
                            "dependencies": {
                                "lodash": "^4.17.21"
                            }
                        }`,
                        `{
                            "name": "test-project",
                            "dependencies": {
                                /*~~>*/"lodash": "^4.17.21"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should find dev dependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "typescript"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(
                        `{
                            "name": "test-project",
                            "devDependencies": {
                                "typescript": "^5.0.0"
                            }
                        }`,
                        `{
                            "name": "test-project",
                            "devDependencies": {
                                /*~~>*/"typescript": "^5.0.0"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should not match when dependency not present", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "react"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    // No "after" means no change expected
                    packageJson(
                        `{
                            "name": "test-project",
                            "dependencies": {
                                "lodash": "^4.17.21"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should find dependency with glob pattern", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "lod*"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(
                        `{
                            "name": "test-project",
                            "dependencies": {
                                "lodash": "^4.17.21"
                            }
                        }`,
                        `{
                            "name": "test-project",
                            "dependencies": {
                                /*~~>*/"lodash": "^4.17.21"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should find scoped package with glob pattern", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "@types/*"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(
                        `{
                            "name": "test-project",
                            "devDependencies": {
                                "@types/node": "^20.0.0"
                            }
                        }`,
                        `{
                            "name": "test-project",
                            "devDependencies": {
                                /*~~>*/"@types/node": "^20.0.0"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should match scoped package with pattern not containing slash", async () => {
        // *node* should match @types/node because we normalize * to ** for patterns without /
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "*node*"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(
                        `{
                            "name": "test-project",
                            "devDependencies": {
                                "@types/node": "^20.0.0"
                            }
                        }`,
                        `{
                            "name": "test-project",
                            "devDependencies": {
                                /*~~>*/"@types/node": "^20.0.0"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should match version constraint", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "lodash", version: ">=4.0.0"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(
                        `{
                            "name": "test-project",
                            "dependencies": {
                                "lodash": "^4.17.0"
                            }
                        }`,
                        `{
                            "name": "test-project",
                            "dependencies": {
                                /*~~>*/"lodash": "^4.17.0"
                            }
                        }`
                    ),
                    packageLockJson(`{
                        "name": "test-project",
                        "lockfileVersion": 3,
                        "packages": {
                            "": {
                                "name": "test-project",
                                "dependencies": {
                                    "lodash": "^4.17.0"
                                }
                            },
                            "node_modules/lodash": {
                                "version": "4.17.21"
                            }
                        }
                    }`)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should not match when version does not satisfy constraint", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "lodash", version: ">=5.0.0"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    // No "after" means no change expected
                    packageJson(
                        `{
                            "name": "test-project",
                            "dependencies": {
                                "lodash": "^4.17.0"
                            }
                        }`
                    ),
                    packageLockJson(`{
                        "name": "test-project",
                        "lockfileVersion": 3,
                        "packages": {
                            "": {
                                "name": "test-project",
                                "dependencies": {
                                    "lodash": "^4.17.0"
                                }
                            },
                            "node_modules/lodash": {
                                "version": "4.17.21"
                            }
                        }
                    }`)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should find peer dependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "react"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(
                        `{
                            "name": "test-project",
                            "peerDependencies": {
                                "react": "^18.0.0"
                            }
                        }`,
                        `{
                            "name": "test-project",
                            "peerDependencies": {
                                /*~~>*/"react": "^18.0.0"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should find optional dependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "fsevents"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(
                        `{
                            "name": "test-project",
                            "optionalDependencies": {
                                "fsevents": "^2.3.0"
                            }
                        }`,
                        `{
                            "name": "test-project",
                            "optionalDependencies": {
                                /*~~>*/"fsevents": "^2.3.0"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should find multiple matching dependencies", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindDependency({packageName: "@types/*"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(
                        `{
                            "name": "test-project",
                            "devDependencies": {
                                "@types/node": "^20.0.0",
                                "@types/jest": "^29.0.0"
                            }
                        }`,
                        `{
                            "name": "test-project",
                            "devDependencies": {
                                /*~~>*/"@types/node": "^20.0.0",
                                /*~~>*/"@types/jest": "^29.0.0"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should find transitive dependency when onlyDirect is false", async () => {
        const spec = new RecipeSpec();
        // Search for "is-number" which is a transitive dependency of "is-odd"
        spec.recipe = new FindDependency({packageName: "is-number", onlyDirect: false});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    // "is-odd" should be marked because it has "is-number" as a transitive dependency
                    packageJson(
                        `{
                            "name": "test-project",
                            "dependencies": {
                                "is-odd": "^3.0.0"
                            }
                        }`,
                        `{
                            "name": "test-project",
                            "dependencies": {
                                /*~~>*/"is-odd": "^3.0.0"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should not find transitive dependency when onlyDirect is true", async () => {
        const spec = new RecipeSpec();
        // Search for "is-number" with onlyDirect: true (default)
        spec.recipe = new FindDependency({packageName: "is-number"});
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    // No match expected - "is-number" is not a direct dependency
                    packageJson(
                        `{
                            "name": "test-project",
                            "dependencies": {
                                "is-odd": "^3.0.0"
                            }
                        }`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });
});
