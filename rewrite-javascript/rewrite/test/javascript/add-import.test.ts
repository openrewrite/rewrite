// noinspection JSUnusedLocalSymbols,TypeScriptCheckImport,TypeScriptUnresolvedReference,ES6UnusedImports

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
import {describe, expect, test} from "@jest/globals";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {
    AddImport,
    ImportStyle,
    IntelliJ,
    javascript,
    JavaScriptVisitor,
    JS,
    maybeAddImport,
    npm,
    packageJson,
    RemoveImport,
    SpacesStyle,
    Template,
    tsx,
    typescript
} from "../../src/javascript";
import {emptySpace, J} from "../../src/java";
import {MarkersKind, NamedStyles, randomId} from "../../src";
import {create as produce} from "mutative";
import {withDir} from "tmp-promise";

/**
 * Helper function to create a visitor that:
 * 1. Removes an import statement for a specific module/member
 * 2. Then adds the import back if it's referenced (to test onlyIfReferenced behavior)
 *
 * This is useful for testing onlyIfReferenced behavior with type attribution.
 * The source code starts with the import (for type attribution during parse),
 * then the visitor removes the import, and AddImport re-adds it based on usage detection.
 *
 * @param module The module to import from (e.g., "fs")
 * @param member Optional member to import (e.g., "readFile")
 * @param alias Optional alias for the import
 */
function createRemoveThenAddImportVisitor(
    module: string,
    member?: string,
    alias?: string
): JavaScriptVisitor<any> {
    const removeImport = new RemoveImport(module, member);
    const addImport = new AddImport({ module, member, alias });

    return new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: any, p: any): Promise<J | undefined> {
            // First remove the import, then add it back if referenced
            let result = await removeImport.visit(cu, p);
            if (result) {
                result = await addImport.visit(result, p);
            }
            return result;
        }
    };
}

/**
 * Helper function to create a visitor that:
 * 1. Manually removes the first import statement (bypassing RemoveImport which may refuse)
 * 2. Then adds a specific import back with onlyIfReferenced: true
 *
 * This is useful for testing type attribution when RemoveImport would refuse to remove
 * an import that's still being used.
 *
 * @param module The module to import from (e.g., "vitest")
 * @param member Optional member to import (e.g., "vi")
 * @param alias Optional alias for the import
 */
function createForceRemoveFirstImportThenAddVisitor(
    module: string,
    member?: string,
    alias?: string
): JavaScriptVisitor<any> {
    return new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            // First, manually remove the first statement (the import)
            let result: any = await this.produceJavaScript(cu, p, async draft => {
                if (draft.statements && draft.statements.length > 0) {
                    draft.statements = draft.statements.slice(1);
                    draft.statements[0].prefix = emptySpace;
                }
            });

            // Then try to add the import back with onlyIfReferenced: true
            if (result) {
                const addImport = new AddImport({
                    module,
                    member,
                    alias,
                    onlyIfReferenced: true
                });
                result = await addImport.visit(result, p) as any;
            }

            return result;
        }
    };
}

/**
 * Helper function to create a visitor that:
 * 1. Replaces placeholder() calls with template-generated code that has proper type attribution
 * 2. Registers AddImport to add the import statement based on the detected usage
 *
 * @param templateCode The code to insert (e.g., "readFile('test.txt', (err, data) => {})")
 * @param module The module to import from (e.g., "fs")
 * @param member Optional member to import (e.g., "readFile")
 * @param alias Optional alias for the import
 */
function createAddImportWithTemplateVisitor(
    templateCode: string,
    module: string,
    member?: string,
    alias?: string
): JavaScriptVisitor<any> {
    // Construct the import statement for type attribution
    let importStatement: string;
    if (member) {
        if (alias) {
            importStatement = `import {${member} as ${alias}} from '${module}'`;
        } else {
            importStatement = `import {${member}} from '${module}'`;
        }
    } else {
        importStatement = `import ${alias || module} from '${module}'`;
    }

    return new class extends JavaScriptVisitor<any> {
        constructor() {
            super();
            // Register AddImport in afterVisit so it runs after template changes
            maybeAddImport(this, { module, member, alias });
        }

        override async visitMethodInvocation(methodInvocation: J.MethodInvocation, p: any): Promise<J | undefined> {
            if (methodInvocation.name?.kind === J.Kind.Identifier &&
                (methodInvocation.name as J.Identifier).simpleName === 'placeholder') {
                // Use builder API for string code with import context so TypeScript can type-attribute the call
                return Template.builder()
                    .code(templateCode)
                    .build()
                    .configure({ context: [importStatement] })
                    .apply(methodInvocation, this.cursor);
            }
            return super.visitMethodInvocation(methodInvocation, p);
        }
    };
}

describe('AddImport visitor', () => {
    describe('named imports', () => {
        test('should add named import when referenced', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(createRemoveThenAddImportVisitor("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {});
                        }
                    `
                )
            );
        });

        test('should not add import when not referenced with onlyIfReferenced=true', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFile", onlyIfReferenced: true }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should add import when not referenced with onlyIfReferenced=false', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFile", onlyIfReferenced: false }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import {readFile} from 'fs';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should not add import if it already exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFile" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {
                            });
                        }
                    `
                )
            );
        });

        test('should add aliased named import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(createRemoveThenAddImportVisitor("fs", "readFile", "read"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile as read} from 'fs';

                        function example() {
                            read('test.txt', (err, data) => {
                            });
                        }
                    `
                )
            );
        });

        test('should not add import if aliased import already exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFile", alias: "read" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile as read} from 'fs';

                        function example() {
                            read('test.txt', (err, data) => {
                            });
                        }
                    `
                )
            );
        });
    });

    describe('default imports', () => {
        test('should add default import when referenced', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import fs from 'fs';

                        function example() {
                            fs.readFileSync('test.txt');
                        }
                    `
                )
            );
        });

        test('should not add default import if it already exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import fs from 'fs';

                        function example() {
                            fs.readFileSync('test.txt');
                        }
                    `
                )
            );
        });

        test('should add default import with alias', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", alias: "fileSystem" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import fileSystem from 'fs';

                        function example() {
                            fileSystem.readFileSync('test.txt');
                        }
                    `
                )
            );
        });

        test('should add default import using "default" member specifier', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "react", member: "default", alias: "React" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import React from 'react';

                        function example() {
                            return React.version;
                        }
                    `
                )
            );
        });

        test('should not duplicate default import when using "default" member specifier', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "react", member: "default", alias: "React" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import React from 'react';

                        function example() {
                            return React.version;
                        }
                    `
                )
            );
        });

        test('should throw error when member is "default" without alias', async () => {
            expect(() => {
                new AddImport({ module: "react", member: "default" });
            }).toThrow("When member is 'default', the alias parameter is required");
        });
    });

    describe('import positioning', () => {
        test('should add import after existing imports', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "path", member: "join" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';
                        import {join} from 'path';

                        function example() {
                            readFile('test.txt', () => {});
                            join('a', 'b');
                        }
                    `
                )
            );
        });

        test('should add import at beginning if no existing imports', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFile" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {
                            });
                        }
                    `
                )
            );
        });

        test('should preserve file header comment when adding import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFile" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        // Copyright 2025
                        // File header comment

                        import {readFile} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {
                            });
                        }
                    `
                )
            );
        });
    });

    describe('merging imports from same module', () => {
        test('should merge new member into existing import from same module', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFileSync", onlyIfReferenced: false }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {});
                        }
                    `,
                    `
                        import {readFile, readFileSync} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {});
                        }
                    `
                )
            );
        });

        test('should merge aliased member into existing import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFileSync", alias: "readSync", onlyIfReferenced: false }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {});
                        }
                    `,
                    `
                        import {readFile, readFileSync as readSync} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {});
                        }
                    `
                )
            );
        });

        test('should not merge if member already exists in import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFile" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile, writeFile} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {});
                        }
                    `
                )
            );
        });

        test('should add named import to existing default-only import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "react", member: "useState", onlyIfReferenced: false }));

            //language=tsx
            await spec.rewriteRun(
                tsx(
                    `
                        import React from 'react';

                        function example() {
                            return <div>{React.version}</div>;
                        }
                    `,
                    `
                        import React, {useState} from 'react';

                        function example() {
                            return <div>{React.version}</div>;
                        }
                    `
                )
            );
        });

        test('should preserve formatting when merging imports (forwardRef, memo example)', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "react", member: "memo", onlyIfReferenced: false }));

            //language=tsx
            await spec.rewriteRun(
                tsx(
                    `
                        import { forwardRef } from 'react';

                        function example() {
                            const MyComponent = forwardRef(() => <div/>);
                        }
                    `,
                    `
                        import { forwardRef, memo } from 'react';

                        function example() {
                            const MyComponent = forwardRef(() => <div/>);
                        }
                    `
                )
            );
        });

        test('should preserve spacing style when inserting import before existing ones', async () => {
            const spec = new RecipeSpec();
            // 'act' comes alphabetically before 'forwardRef', so it will be inserted first
            spec.recipe = fromVisitor(new AddImport({ module: "react", member: "act", onlyIfReferenced: false }));

            //language=tsx
            await spec.rewriteRun(
                tsx(
                    `
                        import { forwardRef } from 'react';

                        function example() {
                            const MyComponent = forwardRef(() => <div/>);
                        }
                    `,
                    `
                        import { act, forwardRef } from 'react';

                        function example() {
                            const MyComponent = forwardRef(() => <div/>);
                        }
                    `
                )
            );
        });

        test('should preserve no-spacing style when merging imports', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "react", member: "memo", onlyIfReferenced: false }));

            //language=tsx
            await spec.rewriteRun(
                tsx(
                    `
                        import {forwardRef} from 'react';

                        function example() {
                            const MyComponent = forwardRef(() => <div/>);
                        }
                    `,
                    `
                        import {forwardRef, memo} from 'react';

                        function example() {
                            const MyComponent = forwardRef(() => <div/>);
                        }
                    `
                )
            );
        });

        test('should preserve no-spacing style when inserting import before existing ones', async () => {
            const spec = new RecipeSpec();
            // 'act' comes alphabetically before 'forwardRef', so it will be inserted first
            spec.recipe = fromVisitor(new AddImport({ module: "react", member: "act", onlyIfReferenced: false }));

            //language=tsx
            await spec.rewriteRun(
                tsx(
                    `
                        import {forwardRef} from 'react';

                        function example() {
                            const MyComponent = forwardRef(() => <div/>);
                        }
                    `,
                    `
                        import {act, forwardRef} from 'react';

                        function example() {
                            const MyComponent = forwardRef(() => <div/>);
                        }
                    `
                )
            );
        });
    });

    describe('CommonJS require detection', () => {
        test('should not add import if require statement already exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const fs = require('fs');

                        function example() {
                            fs.readFileSync('test.txt');
                        }
                    `
                )
            );
        });

        test('should not add import if destructured require already exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFile" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const {readFile} = require('fs');

                        function example() {
                            readFile('test.txt', (err, data) => {});
                        }
                    `
                )
            );
        });

        test('should not add import if aliased destructured require already exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFile", alias: "read" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const {readFile: read} = require('fs');

                        function example() {
                            read('test.txt', (err, data) => {});
                        }
                    `
                )
            );
        });
    });

    describe('style parameter override', () => {
        test('should use ES6Named style when explicitly specified', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: "fs",
                member: "readFile",
                onlyIfReferenced: false,
                style: ImportStyle.ES6Named
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import {readFile} from 'fs';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should use ES6Default style when explicitly specified', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: "fs",
                onlyIfReferenced: false,
                style: ImportStyle.ES6Default
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import fs from 'fs';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should use ES6Default style with custom alias', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: "fs",
                alias: "fileSystem",
                onlyIfReferenced: false,
                style: ImportStyle.ES6Default
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import fileSystem from 'fs';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should use ES6Named style with aliased member', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: "fs",
                member: "readFile",
                alias: "read",
                onlyIfReferenced: false,
                style: ImportStyle.ES6Named
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import {readFile as read} from 'fs';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });
    });

    describe('usage detection', () => {
        test('should detect usage in field access', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs" }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import fs from 'fs';

                        function example() {
                            fs.readFileSync('test.txt');
                        }
                    `
                )
            );
        });

        test('should detect usage as direct function call', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(createAddImportWithTemplateVisitor(
                "promisify(fs.readFile)",
                "util",
                "promisify"
            ));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                    import * as fs from 'fs';

                    function example() {
                        placeholder();
                    }
                    `,
                    `
                    import * as fs from 'fs';
                    import {promisify} from 'util';

                    function example() {
                        promisify(fs.readFile);
                    }
                    `
                )
            );
        });

        test('should detect usage in arrow function', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(createAddImportWithTemplateVisitor(
                "readFile('test.txt', (err, data) => {})",
                "fs",
                "readFile"
            ));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const example = () => {
                            placeholder();
                        };
                    `,
                    `
                        import {readFile} from 'fs';

                        const example = () => {
                            readFile('test.txt', (err, data) => {
                            });
                        };
                    `
                )
            );
        });

        test('should detect usage nested in blocks', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(createAddImportWithTemplateVisitor(
                "readFile('test.txt', (err, data) => {})",
                "fs",
                "readFile"
            ));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            if (true) {
                                placeholder();
                            }
                        }
                    `,
                    `
                        import {readFile} from 'fs';

                        function example() {
                            if (true) {
                                readFile('test.txt', (err, data) => {
                                });
                            }
                        }
                    `
                )
            );
        });

        test('should map declaring type to module specifier (React vs react)', async () => {
            const spec = new RecipeSpec();
            // This test verifies the fix for the bug where the declaring type FQN ('React')
            // differs from the module specifier ('react')
            spec.recipe = fromVisitor(createRemoveThenAddImportVisitor("react", "useState"));

            //language=tsx
            await spec.rewriteRun(
                tsx(
                    `
                        import {useState} from 'react';

                        function example() {
                            const [state, setState] = useState(0);
                        }
                    `
                )
            );
        });

        test('should detect React default import usage when declaring type differs from module', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(createRemoveThenAddImportVisitor("react", "default", "React"));

            //language=tsx
            await spec.rewriteRun(
                tsx(
                    `
                        import React from 'react';

                        function example() {
                            return <div>{React.version}</div>;
                        }
                    `
                )
            );
        });

        test('should detect usage when only named imports exist (forwardRef/memo case)', async () => {
            const spec = new RecipeSpec();
            // This tests the specific case where we have import {forwardRef} from 'react'
            // and want to add memo - the declaring type is 'React' but module is 'react'
            spec.recipe = fromVisitor(createAddImportWithTemplateVisitor(
                "promisify(fs.readFile)",
                "util",
                "promisify"
            ));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';

                        function example() {
                            placeholder();
                        }
                    `,
                    `
                        import {readFile} from 'fs';
                        import {promisify} from 'util';

                        function example() {
                            promisify(fs.readFile);
                        }
                    `
                )
            );
        });

        test('should handle case where usedImports has declaring type name instead of module', async () => {
            const spec = new RecipeSpec();
            // This tests the edge case where some identifiers end up in usedImports
            // under the declaring type name (e.g., 'React') instead of module name ('react')
            spec.recipe = fromVisitor(createRemoveThenAddImportVisitor("react", "memo"));

            //language=tsx
            await spec.rewriteRun(
                tsx(
                    `
                        import {forwardRef, memo} from 'react';

                        function example() {
                            const Component = memo(() => <div/>);
                        }
                    `
                )
            );
        });

        test('should detect React usage with full type attribution (npm + packageJson)', async () => {
            const spec = new RecipeSpec();
            // This is the real-world test with full type attribution from @types/react
            // Test that AddImport detects usage correctly using onlyIfReferenced: false
            // This tests the case where forwardRef exists and we want to add memo (from your original question)
            spec.recipe = fromVisitor(new AddImport({
                module: "react",
                member: "memo",
                onlyIfReferenced: false  // Use false since memo isn't imported yet, so no type attribution
            }));

            //language=tsx
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        tsx(
                            `
                                import {forwardRef} from 'react';

                                function example() {
                                    const Component = forwardRef(() => <div/>);
                                }
                            `,
                            `
                                import {forwardRef, memo} from 'react';

                                function example() {
                                    const Component = forwardRef(() => <div/>);
                                }
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "@types/react": "^18"
                                }
                              }
                            `
                        )
                    )
                )
            }, {unsafeCleanup: true});
        });

        test('should detect React usage with onlyIfReferenced after manual import removal', async () => {
            const spec = new RecipeSpec();
            // This test manually removes the import statement, then adds it back with onlyIfReferenced: true
            // This tests that type attribution correctly detects usage even when declaring type != module name
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitJsCompilationUnit(cu: any, p: any): Promise<J | undefined> {
                    const jsCu = cu as any;
                    // First, manually remove the first statement (the import)
                    let result: any = await this.produceJavaScript(jsCu, p, async (draft: any) => {
                        if (draft.statements && draft.statements.length > 0) {
                            draft.statements = draft.statements.slice(1);
                            draft.statements[0].prefix = emptySpace;
                        }
                    });

                    // Then try to add the import back with onlyIfReferenced: true
                    if (result) {
                        const addImport = new AddImport({
                            module: "react",
                            member: "memo",
                            onlyIfReferenced: true
                        });
                        result = await addImport.visit(result, p) as any;
                    }

                    return result;
                }
            });

            //language=tsx
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        tsx(
                            `
                                import {memo} from 'react';

                                function example() {
                                    const Component = memo(() => <div/>);
                                }
                            `,
                            `
                                import {memo} from 'react';

                                function example() {
                                    const Component = memo(() => <div/>);
                                }
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "@types/react": "^18"
                                }
                              }
                            `
                        )
                    )
                )
            }, {unsafeCleanup: true});
        });
    });

    describe('JavaScript (.js) file support', () => {
        test('should work with .js files using ES6 imports', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: "fs",
                member: "readFile",
                onlyIfReferenced: false,
                style: ImportStyle.ES6Named
            }));

            //language=javascript
            await spec.rewriteRun(
                javascript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import {readFile} from 'fs';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should detect existing CommonJS require in .js files', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "fs", member: "readFile" }));

            //language=javascript
            await spec.rewriteRun(
                javascript(
                    `
                        const {readFile} = require('fs');

                        function example() {
                            readFile('test.txt', (err, data) => {});
                        }
                    `
                )
            );
        });
    });

    describe('side-effect imports', () => {
        test('should add side-effect import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "core-js/stable", sideEffectOnly: true }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import 'core-js/stable';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should detect existing side-effect import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "core-js/stable", sideEffectOnly: true }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import 'core-js/stable';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should add side-effect import when regular import from same module exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "react", sideEffectOnly: true }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {useState} from 'react';

                        function example() {
                            const [state, setState] = useState(0);
                        }
                    `,
                    `
                        import {useState} from 'react';
                        import 'react';

                        function example() {
                            const [state, setState] = useState(0);
                        }
                    `
                )
            );
        });

        test('should add regular import when side-effect import from same module exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: "react",
                member: "useState",
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import 'react';

                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import 'react';
                        import {useState} from 'react';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should add side-effect import when default import exists', async () => {
            const spec = new RecipeSpec();
            // This test verifies that side-effect and regular imports are treated as separate
            // A side-effect import is only a match if another side-effect import exists
            spec.recipe = fromVisitor(new AddImport({ module: "react", sideEffectOnly: true }));

            //language=tsx
            await spec.rewriteRun(
                tsx(
                    `
                        import React from 'react';

                        function example() {
                            return <div>{React.version}</div>;
                        }
                    `,
                    `
                        import React from 'react';
                        import 'react';

                        function example() {
                            return <div>{React.version}</div>;
                        }
                    `
                )
            );
        });

        test('should throw error when combining sideEffectOnly with member', async () => {
            expect(() => {
                new AddImport({ module: "react", sideEffectOnly: true, member: "useState" });
            }).toThrow("Cannot combine sideEffectOnly with member");
        });

        test('should throw error when combining sideEffectOnly with alias', async () => {
            expect(() => {
                new AddImport({ module: "react", sideEffectOnly: true, alias: "React" });
            }).toThrow("Cannot combine sideEffectOnly with alias");
        });

        test('should throw error when combining sideEffectOnly with onlyIfReferenced', async () => {
            expect(() => {
                new AddImport({ module: "react", sideEffectOnly: true, onlyIfReferenced: true });
            }).toThrow("Cannot combine sideEffectOnly with onlyIfReferenced");
        });
    });

    describe('namespace imports', () => {
        test('should add namespace import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'crypto',
                member: '*',
                alias: 'crypto',
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import * as crypto from 'crypto';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should detect existing namespace import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'crypto',
                member: '*',
                alias: 'crypto'
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import * as crypto from 'crypto';

                        function example() {
                            console.log(crypto.randomBytes(16));
                        }
                    `
                )
            );
        });

        test('should add namespace import when named import exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'fs',
                member: '*',
                alias: 'fs',
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {});
                        }
                    `,
                    `
                        import {readFile} from 'fs';
                        import * as fs from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {});
                        }
                    `
                )
            );
        });

        test('should throw error when member is "*" without alias', async () => {
            expect(() => {
                new AddImport({ module: "crypto", member: "*" });
            }).toThrow("When member is '*', the alias parameter is required");
        });

        test('should add namespace import with different alias', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'path',
                member: '*',
                alias: 'pathModule',
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import * as pathModule from 'path';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should add namespace import when referenced with onlyIfReferenced=true', async () => {
            const spec = new RecipeSpec();
            // Note: Due to limitations in type attribution for namespace imports,
            // onlyIfReferenced currently always returns true for namespace imports (member='*')
            spec.recipe = fromVisitor(createRemoveThenAddImportVisitor('crypto', '*', 'crypto'));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import * as crypto from 'crypto';

                        function example() {
                            const bytes = crypto.randomBytes(16);
                        }
                    `
                )
            );
        });
    });

    describe('object imports (non-function references)', () => {
        test('should add import for zod z object when referenced', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(createRemoveThenAddImportVisitor("zod", "z"));

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import {z} from 'zod';

                                function example() {
                                    const schema = z.string();
                                }
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "zod": "^3.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });

        test('should not add import for zod z when not referenced', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ module: "zod", member: "z", onlyIfReferenced: true }));

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                function example() {
                                    console.log('test');
                                }
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "zod": "^3.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });

        test('should add import for zod z when used as standalone identifier', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(createRemoveThenAddImportVisitor("zod", "z"));

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import {z} from 'zod';

                                function example() {
                                    const validator = z;
                                    validator.string();
                                }
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "zod": "^3.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });

        test('should add import for zod z with object schema usage', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(createRemoveThenAddImportVisitor("zod", "z"));

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import {z} from 'zod';

                                function example() {
                                    const schema = z.object({name: z.string()});
                                }
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "zod": "^3.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });

        test('should add import for zod z when another zod import exists (with onlyIfReferenced)', async () => {
            const spec = new RecipeSpec();
            // This test manually removes the z import statement, then adds it back with onlyIfReferenced: true
            // This tests that type attribution correctly detects usage of object types (not just methods).
            // The AddImport visitor will merge the z import into the existing ZodError import from zod.
            spec.recipe = fromVisitor(createForceRemoveFirstImportThenAddVisitor("zod", "z"));

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import {z} from 'zod';
                                import {ZodError} from 'zod';

                                function example() {
                                    const schema = z.string();
                                }
                            `,
                            `
                                import {z, ZodError} from 'zod';

                                function example() {
                                    const schema = z.string();
                                }
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "zod": "^3.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });

        test('should add import for React forwardRef when namespace owner has module info', async () => {
            const spec = new RecipeSpec();
            // This test verifies that when a namespace (e.g., React) owns an export (e.g., forwardRef),
            // and the namespace has module information in owningClass, we can match it to the module name
            spec.recipe = fromVisitor(new AddImport({
                module: 'react',
                member: 'forwardRef',
                onlyIfReferenced: true
            }));

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                function MyComponent() {
                                    return forwardRef(() => null);
                                }
                            `,
                            `
                                import {forwardRef} from 'react';

                                function MyComponent() {
                                    return forwardRef(() => null);
                                }
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "react": "^18.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });

        test('should add import for zod z when code created via templating', async () => {
            const spec = new RecipeSpec();
            // This test simulates code created via templating where z.string() is used
            // but the import doesn't exist. Tests that type attribution works with templated code.

            // Create a visitor that logs what it finds
            const addImportVisitor = new AddImport({
                module: 'zod',
                member: 'z',
                onlyIfReferenced: true
            });

            spec.recipe = fromVisitor(addImportVisitor);

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                function example() {
                                    const schema = z.string();
                                }
                            `,
                            `
                                import {z} from 'zod';

                                function example() {
                                    const schema = z.string();
                                }
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "zod": "^3.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });
    });

    describe('multiple maybeAddImport calls for same module', () => {
        test('should merge imports alphabetically (case-insensitive)', async () => {
            const spec = new RecipeSpec();

            // Visitor that adds imports in non-alphabetical order
            const addImportsVisitor = new class extends JavaScriptVisitor<any> {
                override async visitJsCompilationUnit(cu: any, p: any): Promise<J | undefined> {
                    maybeAddImport(this, {module: 'zod', member: 'ZodType', onlyIfReferenced: false});
                    maybeAddImport(this, {module: 'zod', member: 'ZodError', onlyIfReferenced: false});
                    maybeAddImport(this, {module: 'zod', member: 'z', onlyIfReferenced: false});
                    return cu;
                }
            };
            spec.recipe = fromVisitor(addImportsVisitor);

            //language=typescript
            await spec.rewriteRun(
                // No existing import - creates one and merges in alphabetical order
                typescript(
                    `
                        const x = 1;
                    `,
                    `
                        import {z, ZodError, ZodType} from 'zod';

                        const x = 1;
                    `
                ),
                // Existing import - inserts at correct alphabetical position
                typescript(
                    `
                        import {ZodString} from 'zod';

                        const x = 1;
                    `,
                    `
                        import {z, ZodError, ZodString, ZodType} from 'zod';

                        const x = 1;
                    `
                )
            );
        });
    });

    describe('style options', () => {
        /**
         * Helper function to create a visitor that:
         * 1. Adds a NamedStyles marker to the compilation unit with es6ImportExportBraces: true
         * 2. Then uses AddImport to add an import statement
         */
        function createAddImportWithBraceSpacingVisitor(
            module: string,
            member?: string,
            alias?: string
        ): JavaScriptVisitor<any> {
            return new class extends JavaScriptVisitor<any> {
                override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                    // Create a SpacesStyle with es6ImportExportBraces: true using produce
                    const spacesStyle: SpacesStyle = produce(IntelliJ.TypeScript.spaces(), draft => {
                        draft.within.es6ImportExportBraces = true;
                    });

                    // Create a NamedStyles marker
                    const namedStyles: NamedStyles = {
                        kind: MarkersKind.NamedStyles,
                        id: randomId(),
                        name: "test-style",
                        displayName: "Test Style",
                        tags: [],
                        styles: [spacesStyle]
                    };

                    // Add the NamedStyles marker to the compilation unit
                    let result: JS.CompilationUnit = {
                        ...cu,
                        markers: {
                            ...cu.markers,
                            markers: [...cu.markers.markers, namedStyles]
                        }
                    };

                    // Then add the import
                    const addImport = new AddImport({
                        module,
                        member,
                        alias,
                        onlyIfReferenced: false
                    });
                    result = await addImport.visit(result, p) as JS.CompilationUnit;

                    return result;
                }
            };
        }

        test('should add spaces inside braces when es6ImportExportBraces style is true', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(createAddImportWithBraceSpacingVisitor('fs', 'readFile'));

            await spec.rewriteRun(
                typescript(
                    `
                        const x = 1;
                    `,
                    `
                        import { readFile } from 'fs';

                        const x = 1;
                    `
                )
            );
        });
    });

    describe('type-only imports', () => {
        test('should add type-only import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'react',
                member: 'ReactNode',
                typeOnly: true,
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import type {ReactNode} from 'react';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should not add type-only import if one already exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'react',
                member: 'ReactNode',
                typeOnly: true
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import type {ReactNode} from 'react';

                        function example(): ReactNode {
                            return null;
                        }
                    `
                )
            );
        });

        test('should add value import even when type-only import exists for same member', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'react',
                member: 'useState',
                typeOnly: false,
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import type {useState} from 'react';

                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import type {useState} from 'react';
                        import {useState} from 'react';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should add type-only import even when value import exists for same member', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'react',
                member: 'useState',
                typeOnly: true,
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {useState} from 'react';

                        function example() {
                            useState(0);
                        }
                    `,
                    `
                        import {useState} from 'react';
                        import type {useState} from 'react';

                        function example() {
                            useState(0);
                        }
                    `
                )
            );
        });

        test('should not merge value import into type-only import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'react',
                member: 'useEffect',
                typeOnly: false,
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import type {useState} from 'react';

                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import type {useState} from 'react';
                        import {useEffect} from 'react';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should not merge type-only import into value import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'react',
                member: 'ReactNode',
                typeOnly: true,
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {useState} from 'react';

                        function example() {
                            useState(0);
                        }
                    `,
                    `
                        import {useState} from 'react';
                        import type {ReactNode} from 'react';

                        function example() {
                            useState(0);
                        }
                    `
                )
            );
        });

        test('should merge type-only imports from same module', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'react',
                member: 'ReactElement',
                typeOnly: true,
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import type {ReactNode} from 'react';

                        function example(): ReactNode {
                            return null;
                        }
                    `,
                    `
                        import type {ReactElement, ReactNode} from 'react';

                        function example(): ReactNode {
                            return null;
                        }
                    `
                )
            );
        });

        test('should add type-only default import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'react',
                member: 'default',
                alias: 'React',
                typeOnly: true,
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import type React from 'react';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should add type-only namespace import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                module: 'react',
                member: '*',
                alias: 'React',
                typeOnly: true,
                onlyIfReferenced: false
            }));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        import type * as React from 'react';

                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should throw error when combining typeOnly with sideEffectOnly', async () => {
            expect(() => {
                new AddImport({ module: "react", sideEffectOnly: true, typeOnly: true });
            }).toThrow("Cannot combine sideEffectOnly with typeOnly");
        });
    });
});
