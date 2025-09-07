// noinspection JSUnusedLocalSymbols,TypeScriptMissingConfigOption,TypeScriptCheckImport

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
import {RecipeSpec} from "../../src/test";
import {javascript, JavaScriptVisitor, npm, packageJson, tsx, typescript} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext, foundSearchResult, Recipe} from "../../src";
import {withDir} from "tmp-promise";

describe('JavaScript type mapping', () => {
    describe('primitive types', () => {
        test('should map number literals', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Literal && typeof (node as J.Literal).value === 'number') {
                    return formatPrimitiveType(type);
                }
                return null;
            });

            //language=javascript
            await spec.rewriteRun(
                javascript(
                    "let x = 1;",
                    "let x = /*~~(double)~~>*/1;"
                ),
                javascript(
                    "let y = 1.5;",
                    "let y = /*~~(double)~~>*/1.5;"
                )
            );
        });

        test('should map string literals', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Literal && typeof (node as J.Literal).value === 'string') {
                    return formatPrimitiveType(type);
                }
                return null;
            });

            //language=javascript
            await spec.rewriteRun(
                javascript(
                    'const x = "hello";',
                    'const x = /*~~(String)~~>*/"hello";'
                ),
                javascript(
                    "const y = `template`;",
                    "const y = /*~~(String)~~>*/`template`;"
                )
            );
        });

        test('should map boolean literals', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Literal && typeof (node as J.Literal).value === 'boolean') {
                    return formatPrimitiveType(type);
                }
                return null;
            });

            //language=javascript
            await spec.rewriteRun(
                javascript(
                    "let x = true;",
                    "let x = /*~~(boolean)~~>*/true;"
                ),
                javascript(
                    "let y = false;",
                    "let y = /*~~(boolean)~~>*/false;"
                )
            );
        });

        test('should map null literal', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Literal && (node as J.Literal).value === null) {
                    return formatPrimitiveType(type);
                }
                return null;
            });

            await spec.rewriteRun(
                //language=javascript
                javascript(
                    "let x = null;",
                    "let x = /*~~(null)~~>*/null;"
                )
            );
        });

        test('should map undefined identifier', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'undefined') {
                    return formatPrimitiveType(type);
                }
                return null;
            });

            await spec.rewriteRun(
                //language=javascript
                javascript(
                    "let x = undefined;",
                    "let x = /*~~(None)~~>*/undefined;"
                )
            );
        });

        test('should map bigint literal', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Literal && typeof (node as J.Literal).value === 'bigint') {
                    return formatPrimitiveType(type);
                }
                return null;
            });

            await spec.rewriteRun(
                //language=javascript
                javascript(
                    "let x = 123n;",
                    "let x = /*~~(long)~~>*/123n;"
                )
            );
        });

        test('should map regex literal', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Check if it's a regex literal by looking at the valueSource
                if (node?.kind === J.Kind.Literal && (node as J.Literal).valueSource?.startsWith('/')) {
                    return formatPrimitiveType(type);
                }
                return null;
            });

            await spec.rewriteRun(
                //language=javascript
                javascript(
                    "let x = /pattern/gi;",
                    "let x = /*~~(String)~~>*//pattern/gi;"
                )
            );
        });

        test('should map multiple literals with correct types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Literal) {
                    return formatPrimitiveType(type);
                }
                return null;
            });

            await spec.rewriteRun(
                //language=javascript
                javascript(
                    `
                        const num = 42;
                        const str = "test";
                        const bool = true;
                        const nil = null;
                    `,
                    `
                        const num = /*~~(double)~~>*/42;
                        const str = /*~~(String)~~>*/"test";
                        const bool = /*~~(boolean)~~>*/true;
                        const nil = /*~~(null)~~>*/null;
                    `
                )
            );
        });
    });

    describe('type annotations', () => {
        // TODO: These will be implemented in Phase 2/3
        test.skip('should map type annotations', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Will mark type reference nodes when implemented
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    "let x: number;",
                    "let x: /*~~(double)~~>*/number;"
                )
            );
        });
    });

    describe('cache behavior', () => {
        test('should use same type instance for same primitive types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Literal && typeof (node as J.Literal).value === 'number') {
                    // If types are cached properly, all should be "double"
                    return formatPrimitiveType(type);
                }
                return null;
            });

            await spec.rewriteRun(
                //language=javascript
                javascript(
                    `
                        let x = 1;
                        let y = 2;
                        let z = 3;
                    `,
                    `
                        let x = /*~~(double)~~>*/1;
                        let y = /*~~(double)~~>*/2;
                        let z = /*~~(double)~~>*/3;
                    `
                )
            );
        });
    });

    describe('union types', () => {
        test('should handle union type with concrete initializer', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Literal && (node as J.Literal).value === "hello") {
                    return formatPrimitiveType(type);
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    'let x: string | number = "hello";',
                    'let x: string | number = /*~~(String)~~>*/"hello";'
                )
            );
        });
    });

    describe('class types', () => {
        test('should map built-in DOM types with correct fully qualified names', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark HTMLElement type references
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'HTMLElement') {
                    if (Type.isClass(type)) {
                        // Verify we're getting members - HTMLElement has many properties
                        const memberNames = type.members.map(m => m.name);
                        // Check for some known HTMLElement properties
                        const hasExpectedProperties =
                            memberNames.includes('innerHTML') &&
                            memberNames.includes('style') &&
                            memberNames.includes('classList');

                        if (hasExpectedProperties) {
                            return `${type.fullyQualifiedName} (${type.members.length} members)`;
                        }
                        return type.fullyQualifiedName;
                    }
                    return null;
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        let element: HTMLElement;
                        const div = document.createElement('div');
                        element = div;
                    `,
                    `
                        let element: /*~~(lib.HTMLElement (235 members))~~>*/HTMLElement;
                        const div = document.createElement('div');
                        element = div;
                    `
                )
            );
        });

        test('should map types from libraries accessible from node_modules', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark any node that has a lodash-related type
                return Type.isClass(type) && type.fullyQualifiedName.includes('lodash') ?
                    type.fullyQualifiedName : null;
            });

            // Use tmp-promise's withDir for automatic cleanup
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        //language=typescript
                        typescript(
                            `
                                import _ from 'lodash';

                                const numbers = [1, 2, 3, 4, 5];
                                const doubled = _.map(numbers, n => n * 2);
                                const sum = _.reduce(doubled, (a, b) => a + b, 0);
                            `,
                            `
                                import /*~~(lodash)~~>*/_ from 'lodash';

                                const numbers = [1, 2, 3, 4, 5];
                                const doubled = /*~~(lodash)~~>*/_.map(numbers, n => n * 2);
                                const sum = /*~~(lodash)~~>*/_.reduce(doubled, (a, b) => a + b, 0);
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "lodash": "^4.17.21"
                                },
                                "devDependencies": {
                                  "@types/lodash": "^4.14.195"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });

        test('should map user-defined class types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark class identifiers
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'Person') {
                    return Type.isClass(type) ? type.fullyQualifiedName : null;
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        class Person {
                            name: string;
                            age: number;
                        }
                    `,
                    `
                        class /*~~(Person)~~>*/
                        Person {
                            name: string;
                            age: number;
                        }
                    `
                )
            );
        });

        test('should map interface types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark interface identifiers
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'User') {
                    return Type.isClass(type) ? type.fullyQualifiedName : null;
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        interface User {
                            id: number;
                            email: string;
                        }
                    `,
                    `
                        interface /*~~(User)~~>*/
                        User {
                            id: number;
                            email: string;
                        }
                    `
                )
            );
        });

        test.skip('should map array types', async () => {
            // TODO: Implement in Phase 4
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark array literals
                if (node?.kind === J.Kind.NewArray) {
                    if (Type.isArray(type)) {
                        const elemTypeName = Type.isPrimitive(type.elemType)
                            ? type.elemType.keyword || 'unknown'
                            : 'unknown';
                        return `Array<${elemTypeName}>`;
                    }
                    return null;
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        let numbers: number[] = [1, 2, 3];
                        let strings: Array<string> = ["a", "b"];
                    `,
                    `
                        let numbers: number[] = /*~~(Array<double>)~~>*/[1, 2, 3];
                        let strings: Array<string> = /*~~(Array<String>)~~>*/["a", "b"];
                    `
                )
            );
        });

        test.skip('should map function types', async () => {
            // TODO: Implement in Phase 3
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark function identifiers with their method type
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'add') {
                    // Check for Method type using kind property
                    return type?.kind === Type.Kind.Method ? `(number, number) => number` : null;
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        function add(a: number, b: number): number {
                            return a + b;
                        }
                    `,
                    `
                        function /*~~((number, number) => number)~~>*/add(a: number, b: number): number {
                            return a + b;
                        }
                    `
                )
            );
        });

        test.skip('should map generic types', async () => {
            // TODO: Implement in Phase 5
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark generic function with parameterized type
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'identity') {
                    return Type.isParameterized(type) ? `<T>(T) => T` : null;
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        function identity<T>(value: T): T {
                            return value;
                        }
                    `,
                    `
                        function /*~~(<T>(T) => T)~~>*/identity<T>(value: T): T {
                            return value;
                        }
                    `
                )
            );
        });

        test.skip('should map JSX element types', async () => {
            // TODO: Implement in Phase 7
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark JSX elements with their component type
                if (node?.kind === J.Kind.NewClass && type) {
                    // JSX elements are typically mapped as NewClass in the J model
                    return Type.isClass(type) ? type.fullyQualifiedName : null;
                }
                return null;
            });

            await spec.rewriteRun(
                //language=tsx
                tsx(
                    `
                        import React from 'react';

                        const element = <div className="test">Hello</div>;
                    `,
                    `
                        import React from 'react';

                        const element = /*~~(JSX.IntrinsicElements.div)~~>*/<div className="test">Hello</div>;
                    `
                )
            );
        });
    });
});

/**
 * Helper to create a recipe that marks types
 */
function markTypes(predicate: (node: any, type: Type | undefined) => string | null): Recipe {
    class TypeMarkerRecipe extends Recipe {
        name = 'Type marker';
        displayName = 'Mark types';
        description = 'Marks nodes with their types';

        async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
            return new class extends JavaScriptVisitor<ExecutionContext> {
                async visitLiteral(literal: J.Literal, p: ExecutionContext): Promise<J.Literal> {
                    const visited = await super.visitLiteral(literal, p) as J.Literal;
                    const description = predicate(literal, literal.type);
                    if (description) {
                        return foundSearchResult(visited, description);
                    }
                    return visited;
                }

                async visitIdentifier(ident: J.Identifier, p: ExecutionContext): Promise<J.Identifier> {
                    const visited = await super.visitIdentifier(ident, p) as J.Identifier;
                    const description = predicate(ident, ident.type);
                    if (description) {
                        return foundSearchResult(visited, description);
                    }
                    return visited;
                }
            }
        }
    }

    return new TypeMarkerRecipe();
}

/**
 * Helper to format primitive types for display
 */
function formatPrimitiveType(type: Type | undefined): string | null {
    return Type.isPrimitive(type) ? type.keyword || 'None' : null;
}
