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
import {describe} from "@jest/globals";
import {RecipeSpec} from "../../../src/test";
import {ChangeImport, npm, packageJson, tsx, typescript} from "../../../src/javascript";
import {withDir} from "tmp-promise";

describe("change-import", () => {
    describe("named imports", () => {
        test("changes named import to different module", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "react-dom/test-utils",
                oldMember: "act",
                newModule: "react"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { act } from 'react-dom/test-utils';

                            act(() => {});
                            `,
                            `
                            import { act } from 'react';

                            act(() => {});
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {
                                "react": "^19.0.0",
                                "react-dom": "^19.0.0"
                            }
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });

        test("changes named import with double quotes", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "react-dom/test-utils",
                oldMember: "act",
                newModule: "react"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { act } from "react-dom/test-utils";

                            act(() => {});
                            `,
                            `
                            import { act } from "react";

                            act(() => {});
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {
                                "react": "^19.0.0",
                                "react-dom": "^19.0.0"
                            }
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });

        test("preserves other imports from the same module", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "react-dom/test-utils",
                oldMember: "act",
                newModule: "react"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        tsx(
                            `
                            import { act, renderIntoDocument } from 'react-dom/test-utils';

                            act(() => {});
                            renderIntoDocument(<div />);
                            `,
                            `
                            import { renderIntoDocument } from 'react-dom/test-utils';
                            import { act } from 'react';

                            act(() => {});
                            renderIntoDocument(<div />);
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {
                                "react": "^19.0.0",
                                "react-dom": "^19.0.0"
                            }
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });

        test("does not change import from different module", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "react-dom/test-utils",
                oldMember: "act",
                newModule: "react"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { act } from 'react';

                            act(() => {});
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {
                                "react": "^19.0.0"
                            }
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });

        test("does not change unrelated imports from the same module", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "react-dom/test-utils",
                oldMember: "act",
                newModule: "react"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        tsx(
                            `
                            import { renderIntoDocument } from 'react-dom/test-utils';

                            renderIntoDocument(<div />);
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {
                                "react": "^19.0.0",
                                "react-dom": "^19.0.0"
                            }
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });

        test("adds import from target module", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "react-dom/test-utils",
                oldMember: "act",
                newModule: "react"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { useState } from 'react';
                            import { act } from 'react-dom/test-utils';

                            const [state, setState] = useState(0);
                            act(() => {});
                            `,
                            `
                            import { useState } from 'react';
                            import { act } from 'react';

                            const [state, setState] = useState(0);
                            act(() => {});
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {
                                "react": "^19.0.0",
                                "react-dom": "^19.0.0"
                            }
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });

        test("preserves aliased import", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "react-dom/test-utils",
                oldMember: "act",
                newModule: "react"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { act as actFromTestUtils } from 'react-dom/test-utils';

                            actFromTestUtils(() => {});
                            `,
                            `
                            import { act as actFromTestUtils } from 'react';

                            actFromTestUtils(() => {});
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {
                                "react": "^19.0.0",
                                "react-dom": "^19.0.0"
                            }
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });
    });

    describe("default imports", () => {
        test("changes default import to different module", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "old-module",
                oldMember: "default",
                newModule: "new-module"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import myDefault from 'old-module';

                            myDefault();
                            `,
                            `
                            import myDefault from 'new-module';

                            myDefault();
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {}
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });
    });

    describe("namespace imports", () => {
        test("changes namespace import to different module", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "old-module",
                oldMember: "*",
                newModule: "new-module"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import * as oldModule from 'old-module';

                            oldModule.doSomething();
                            `,
                            `
                            import * as oldModule from 'new-module';

                            oldModule.doSomething();
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {}
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });
    });

    describe("member renaming", () => {
        test("renames member when changing import", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "lodash",
                oldMember: "extend",
                newModule: "lodash",
                newMember: "assign"
            });

            // Note: This test only verifies the import statement change.
            // The identifier usage in the code would need a separate recipe to rename.
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { extend } from 'lodash';
                            `,
                            `
                            import { assign } from 'lodash';
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {
                                "lodash": "^4.17.21"
                            }
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });
    });

    describe("TSX files", () => {
        test("works with TSX files", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "react-dom/test-utils",
                oldMember: "act",
                newModule: "react"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        tsx(
                            `
                            import { act } from 'react-dom/test-utils';
                            import React from 'react';

                            const Component = () => <div>Hello</div>;

                            act(() => {});
                            `,
                            `
                            import { act } from 'react';
                            import React from 'react';

                            const Component = () => <div>Hello</div>;

                            act(() => {});
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {
                                "react": "^19.0.0",
                                "react-dom": "^19.0.0"
                            }
                        }`)
                    )
                );
            }, {unsafeCleanup: true});
        });
    });
});
