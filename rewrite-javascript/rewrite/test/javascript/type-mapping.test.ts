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
            spec.recipe = markTypes((_, type) => {
                // Mark any node that has a lodash-related type
                return Type.isClass(type) && type.fullyQualifiedName.includes('lodash') ?
                    type.fullyQualifiedName : null;
            });

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
                                import /*~~(@types/lodash.LoDashStatic)~~>*/_ from 'lodash';

                                const numbers = [1, 2, 3, 4, 5];
                                const doubled = /*~~(@types/lodash.LoDashStatic)~~>*/_.map(numbers, n => n * 2);
                                const sum = /*~~(@types/lodash.LoDashStatic)~~>*/_.reduce(doubled, (a, b) => a + b, 0);
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
                    //@formatter:off
                    `
                        class /*~~(Person)~~>*/Person {
                            name: string;
                            age: number;
                        }
                    `
                    //@formatter:on
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
                    //@formatter:off
                    `
                        interface /*~~(User)~~>*/User {
                            id: number;
                            email: string;
                        }
                    `
                    //@formatter:on
                )
            );
        });

        test('should map array types', async () => {
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

        test.skip('should map tuple types', async () => {
            // TODO: Tuples are not recognized as arrays by isArrayType()
            // They need special handling as they are a different TypeScript type
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark tuple literals
                if (node?.kind === J.Kind.NewArray) {
                    if (Type.isArray(type)) {
                        const elemTypeName = Type.isPrimitive(type.elemType)
                            ? type.elemType.keyword || 'unknown'
                            : Type.isClass(type.elemType)
                            ? 'union'  // Tuples often have union element types
                            : 'unknown';
                        return `Array<${elemTypeName}>`;
                    }
                    // Debug what we get for tuples
                    if (type) {
                        return `NotArray:${type.kind}`;
                    }
                    return 'NO_TYPE';
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const tuple: [string, number] = ["hello", 42];
                        const triple: [boolean, string, number] = [true, "test", 123];
                    `,
                    `
                        const tuple: [string, number] = /*~~(Array<union>)~~>*/["hello", 42];
                        const triple: [boolean, string, number] = /*~~(Array<union>)~~>*/[true, "test", 123];
                    `
                )
            );
        });

        test('should map readonly array types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark array literals assigned to readonly arrays
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
                        const readonlyNumbers: readonly number[] = [1, 2, 3];
                        const readonlyStrings: ReadonlyArray<string> = ["a", "b"];
                        const frozenArray = Object.freeze([1, 2, 3]);
                    `,
                    `
                        const readonlyNumbers: readonly number[] = /*~~(Array<double>)~~>*/[1, 2, 3];
                        const readonlyStrings: ReadonlyArray<string> = /*~~(Array<String>)~~>*/["a", "b"];
                        const frozenArray = Object.freeze(/*~~(Array<double>)~~>*/[1, 2, 3]);
                    `
                )
            );
        });

        test.skip('should map function types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark function declarations with their method type
                if (node?.kind === J.Kind.MethodDeclaration) {
                    const methodDecl = node as J.MethodDeclaration;
                    if (methodDecl.name.simpleName === 'add') {
                        // Pass the methodType as the type parameter instead
                        // since the predicate function expects it as the second parameter
                        return null; // We'll use the passed-in type parameter
                    }
                }
                // The type parameter contains methodType for MethodDeclaration nodes
                if (type && type.kind === Type.Kind.Method) {
                    const methodType = type as Type.Method;
                    if (methodType.name === 'add') {
                        const params = methodType.parameterNames.join(', ');
                        const returnTypeName = Type.isPrimitive(methodType.returnType) 
                            ? methodType.returnType.keyword || 'unknown'
                            : 'unknown';
                        return `(${params}) => ${returnTypeName}`;
                    }
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
                        /*~~((a, b) => double)~~>*/function add(a: number, b: number): number {
                            return a + b;
                        }
                    `
                )
            );
        });

        test.skip('should map arrow function types', async () => {
            // TODO: Arrow functions need special handling - methodType might not be attached to Lambda
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark arrow functions with their method type
                if (node?.kind === J.Kind.Lambda) {
                    const lambda = node as J.Lambda;
                    // Arrow functions might have methodType attached
                    const methodType = (lambda as any).methodType;
                    if (methodType && methodType.kind === Type.Kind.Method) {
                        const params = methodType.parameterNames.join(', ');
                        const returnTypeName = Type.isPrimitive(methodType.returnType)
                            ? methodType.returnType.keyword || 'unknown'
                            : 'unknown';
                        return `(${params}) => ${returnTypeName}`;
                    }
                    return null;
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const multiply = (x: number, y: number): number => x * y;
                    `,
                    `
                        const multiply = /*~~((x, y) => double)~~>*/(x: number, y: number): number => x * y;
                    `
                )
            );
        });

        test('should map method invocation types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark method invocations with their method type
                if (node?.kind === J.Kind.MethodInvocation) {
                    const invocation = node as J.MethodInvocation;
                    const methodType = invocation.methodType;
                    if (methodType && methodType.kind === Type.Kind.Method) {
                        return `${methodType.name}() returns ${
                            Type.isPrimitive(methodType.returnType)
                                ? methodType.returnType.keyword || 'unknown'
                                : 'unknown'
                        }`;
                    }
                    return null;
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const result = Math.sqrt(16);
                    `,
                    `
                        const result = /*~~(sqrt() returns double)~~>*/Math.sqrt(16);
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

                async visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): Promise<J.MethodDeclaration> {
                    const visited = await super.visitMethodDeclaration(method, p) as J.MethodDeclaration;
                    const description = predicate(visited, visited.methodType);
                    if (description) {
                        return foundSearchResult(visited, description);
                    }
                    return visited;
                }

                async visitNewArray(newArray: J.NewArray, p: ExecutionContext): Promise<J.NewArray> {
                    const visited = await super.visitNewArray(newArray, p) as J.NewArray;
                    const description = predicate(visited, visited.type);
                    if (description) {
                        return foundSearchResult(visited, description);
                    }
                    return visited;
                }

                async visitLambda(lambda: J.Lambda, p: ExecutionContext): Promise<J.Lambda> {
                    const visited = await super.visitLambda(lambda, p) as J.Lambda;
                    const description = predicate(visited, (visited as any).methodType);
                    if (description) {
                        return foundSearchResult(visited, description);
                    }
                    return visited;
                }

                async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J.MethodInvocation> {
                    const visited = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                    const description = predicate(visited, visited.methodType);
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
