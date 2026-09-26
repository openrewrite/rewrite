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
import { RecipeSpec } from "../../../src/test";
import { ChangeImport, JavaScriptVisitor, npm, packageJson, tsx, typescript } from "../../../src/javascript";
import { J, Type } from "../../../src/java";
import { withDir } from "tmp-promise";

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
            }, { unsafeCleanup: true });
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
            }, { unsafeCleanup: true });
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
            }, { unsafeCleanup: true });
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
            }, { unsafeCleanup: true });
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
            }, { unsafeCleanup: true });
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
            }, { unsafeCleanup: true });
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
            }, { unsafeCleanup: true });
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
            }, { unsafeCleanup: true });
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
            }, { unsafeCleanup: true });
        });
    });

    describe("member renaming", () => {
        test("renames aliased member when changing import", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "lodash",
                oldMember: "extend",
                newModule: "lodash",
                newMember: "assign"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { extend as myExtend } from 'lodash';
                            `,
                            `
                            import { assign as myExtend } from 'lodash';
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
            }, { unsafeCleanup: true });
        });

        test("renames an unaliased member and the references that resolve to it", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "primeng/sidebar",
                oldMember: "SidebarModule",
                newModule: "primeng/drawer",
                newMember: "DrawerModule"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { SidebarModule } from 'primeng/sidebar';

                            const m = SidebarModule;
                            `,
                            `
                            import { DrawerModule } from 'primeng/drawer';

                            const m = DrawerModule;
                            `
                        ),
                        packageJson(`{
                            "name": "test",
                            "dependencies": {}
                        }`)
                    )
                );
            }, { unsafeCleanup: true });
        });

        test("renames the member but keeps the local name", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "lodash",
                oldMember: "extend",
                newModule: "lodash",
                newMember: "assign"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { extend } from 'lodash';

                            function assign() {}

                            extend({}, {});
                            assign();
                            `,
                            `
                            import { assign as extend } from 'lodash';

                            function assign() {}

                            extend({}, {});
                            assign();
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
            }, { unsafeCleanup: true });
        });

        test("a pinned alias keeps a type-only specifier's type keyword separate", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "lodash",
                oldMember: "extend",
                newModule: "lodash",
                newMember: "assign",
                newAlias: "extend"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { type extend } from 'lodash';

                            let v: extend;
                            `,
                            `
                            import { type assign as extend } from 'lodash';

                            let v: extend;
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
            }, { unsafeCleanup: true });
        });

        test("moving a member to another module renames it too, leaving its siblings behind", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "lodash",
                oldMember: "extend",
                newModule: "lodash-es",
                newMember: "assign"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                            import { extend, flatten } from 'lodash';

                            extend({}, {});
                            flatten([]);
                            `,
                            `
                            import { flatten } from 'lodash';
                            import { assign } from 'lodash-es';

                            assign({}, {});
                            flatten([]);
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
            }, { unsafeCleanup: true });
        });

        test("does not rename if member is local alias only", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new ChangeImport({
                oldModule: "lodash",
                oldMember: "extend",
                newModule: "lodash",
                newMember: "assign"
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                             import { flatten as extend } from 'lodash';
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
            }, { unsafeCleanup: true });
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
            }, { unsafeCleanup: true });
        });
    });

    test("the moved member's attribution follows it, and a sibling's stays behind", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeImport({
            oldModule: "lodash",
            oldMember: "extend",
            newModule: "lodash-es",
            newMember: "assign"
        });
        const attribution: string[] = [];

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(repo.path, {
                    ...typescript(
                        `import { extend, flatten } from 'lodash';\n\nextend({}, {});\nflatten([[1]]);\n`,
                        `import { flatten } from 'lodash';\nimport { assign } from 'lodash-es';\n\nassign({}, {});\nflatten([[1]]);\n`),
                    afterRecipe: async (cu: any) => {
                        await new class extends JavaScriptVisitor<any> {
                            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                                const declaringType = m.methodType?.declaringType;
                                attribution.push(`${declaringType && Type.isFullyQualified(declaringType)
                                    ? Type.FullyQualified.getFullyQualifiedName(declaringType as any)
                                    : undefined}.${m.methodType?.name}`);
                                return m;
                            }
                        }().visit(cu, undefined);
                    }
                } as any, packageJson(`{
                    "name": "test",
                    "dependencies": {
                        "lodash": "^4.17.21",
                        "lodash-es": "^4.17.21",
                        "@types/lodash": "^4.14.202",
                        "@types/lodash-es": "^4.17.12"
                    }
                }`))
            );
        }, { unsafeCleanup: true });

        // Printing does not show a method type's module, so this is what pins the sibling's.
        expect(attribution).toEqual(["lodash-es.assign", "lodash.flatten"]);
    });

    test("moving a reference typed by a large graph terminates", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeImport({
            oldModule: "./token",
            oldMember: "documentToken",
            newModule: "./core-token"
        });
        const referenced: { name?: string, members?: number } = {};

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {
                        ...typescript(
                            `import { documentToken } from './token';\n\nconst d = documentToken;\nconsole.log(d);\n`,
                            `import { documentToken } from './core-token';\n\nconst d = documentToken;\nconsole.log(d);\n`),
                        path: "main.ts",
                        afterRecipe: async (cu: any) => {
                            await new class extends JavaScriptVisitor<any> {
                                override async visitIdentifier(id: J.Identifier, p: any): Promise<J | undefined> {
                                    const type = id.type;
                                    if (id.simpleName === "documentToken" && referenced.name === undefined &&
                                        type !== undefined && Type.isFullyQualified(type)) {
                                        referenced.name = Type.FullyQualified.getFullyQualifiedName(type as any);
                                        referenced.members = (type as Type.Class).members.length;
                                    }
                                    return id;
                                }
                            }().visit(cu, undefined);
                        }
                    } as any,
                    {
                        ...typescript(`export const documentToken: Document = document;`),
                        path: "token.ts"
                    },
                    packageJson(`{"name": "test"}`)
                )
            );
        }, { unsafeCleanup: true });

        // `Document` reaches some 50k types, which is the only reason this case is large enough to
        // guard the walk; with a primitive here the recipe finishes instantly and pins nothing.
        expect(referenced.name).toBe("Document");
        expect(referenced.members).toBeGreaterThan(100);
    }, 30000);
});
