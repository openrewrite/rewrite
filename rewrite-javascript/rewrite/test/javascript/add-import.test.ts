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
import {describe, test} from "@jest/globals";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {
    AddImport,
    ImportStyle,
    javascript,
    JavaScriptVisitor,
    maybeAddImport,
    RemoveImport,
    Template,
    template,
    typescript
} from "../../src/javascript";
import {J} from "../../src/java";

/**
 * Helper function to create a visitor that:
 * 1. Removes an import statement for a specific module/member
 * 2. Then adds the import back if it's referenced (to test onlyIfReferenced behavior)
 *
 * This is useful for testing onlyIfReferenced behavior with type attribution.
 * The source code starts with the import (for type attribution during parse),
 * then the visitor removes the import, and AddImport re-adds it based on usage detection.
 *
 * @param target The module to import from (e.g., "fs")
 * @param member Optional member to import (e.g., "readFile")
 * @param alias Optional alias for the import
 */
function createRemoveThenAddImportVisitor(
    target: string,
    member?: string,
    alias?: string
): JavaScriptVisitor<any> {
    const removeImport = new RemoveImport(target, member);
    const addImport = new AddImport({ target, member, alias });

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
 * 1. Replaces placeholder() calls with template-generated code that has proper type attribution
 * 2. Registers AddImport to add the import statement based on the detected usage
 *
 * @param templateCode The code to insert (e.g., "readFile('test.txt', (err, data) => {})")
 * @param target The module to import from (e.g., "fs")
 * @param member Optional member to import (e.g., "readFile")
 * @param alias Optional alias for the import
 */
function createAddImportWithTemplateVisitor(
    templateCode: string,
    target: string,
    member?: string,
    alias?: string
): JavaScriptVisitor<any> {
    // Construct the import statement for type attribution
    let importStatement: string;
    if (member) {
        if (alias) {
            importStatement = `import {${member} as ${alias}} from '${target}'`;
        } else {
            importStatement = `import {${member}} from '${target}'`;
        }
    } else {
        importStatement = `import ${alias || target} from '${target}'`;
    }

    return new class extends JavaScriptVisitor<any> {
        constructor() {
            super();
            // Register AddImport in afterVisit so it runs after template changes
            maybeAddImport(this, { target, member, alias });
        }

        override async visitMethodInvocation(methodInvocation: J.MethodInvocation, p: any): Promise<J | undefined> {
            if (methodInvocation.name?.kind === J.Kind.Identifier &&
                (methodInvocation.name as J.Identifier).simpleName === 'placeholder') {
                // Use builder API for string code with import context so TypeScript can type-attribute the call
                return Template.builder()
                    .code(templateCode)
                    .build()
                    .configure({ context: [importStatement] })
                    .apply(this.cursor, methodInvocation);
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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFile", onlyIfReferenced: true }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFile", onlyIfReferenced: false }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFile" }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFile", alias: "read" }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs" }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs" }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", alias: "fileSystem" }));

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
    });

    describe('import positioning', () => {
        test('should add import after existing imports', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ target: "path", member: "join" }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFile" }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFile" }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFileSync", onlyIfReferenced: false }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFileSync", alias: "readSync", onlyIfReferenced: false }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFile" }));

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
    });

    describe('CommonJS require detection', () => {
        test('should not add import if require statement already exists', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({ target: "fs" }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFile" }));

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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFile", alias: "read" }));

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
                target: "fs",
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
                target: "fs",
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
                target: "fs",
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
                target: "fs",
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
            spec.recipe = fromVisitor(new AddImport({ target: "fs" }));

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
    });

    describe('JavaScript (.js) file support', () => {
        test('should work with .js files using ES6 imports', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AddImport({
                target: "fs",
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
            spec.recipe = fromVisitor(new AddImport({ target: "fs", member: "readFile" }));

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
});
