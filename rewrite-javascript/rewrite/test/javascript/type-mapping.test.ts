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
import {javascript, JavaScriptVisitor, JS, npm, packageJson, tsx, typescript} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext, foundSearchResult, Recipe} from "../../src";
import {withDir} from "tmp-promise";
import FullyQualified = Type.FullyQualified;

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
                if (node?.kind === J.Kind.Literal && typeof (node as J.Literal).value === 'string') {
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
            spec.recipe = markTypes((_node, _type) => {
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
                        let element: /*~~(HTMLElement (246 members))~~>*/HTMLElement;
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
                return Type.isClass(type) && type.fullyQualifiedName.includes('LoDash') ?
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
                                import /*~~(_.LoDashStatic)~~>*/_ from 'lodash';

                                const numbers = [1, 2, 3, 4, 5];
                                const doubled = /*~~(_.LoDashStatic)~~>*/_.map(numbers, n => n * 2);
                                const sum = /*~~(_.LoDashStatic)~~>*/_.reduce(doubled, (a, b) => a + b, 0);
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
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

        test('deprecated node methods with CommonJS', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((_, type) => {
                return Type.isMethod(type) && type.name === 'isArray' ? FullyQualified.getFullyQualifiedName(type.declaringType) : null;
            });

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                const util = require('util');
                                util.isArray([])
                            `,
                            //@formatter:off
                            `
                                const util = require('util');
                                /*~~(util)~~>*/util.isArray([])
                            `
                            //@formatter:on
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/node": "^20"
                                }
                              }
                            `
                        )
                    )
                )
            }, {unsafeCleanup: true});
        });

        test('Promise not PromiseConstructor', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((_, type) => {
                return Type.isClass(type) ? type.fullyQualifiedName : null;
            });
            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `Promise.resolve("data")`,
                    //@formatter:off
                    `/*~~(Promise)~~>*/Promise.resolve("data")`
                    //@formatter:on
                )
            )
        })

        test('aliased destructured ES6 import has correct method name', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (Type.isMethod(type)) {
                    const method = type as Type.Method;
                    if (FullyQualified.getFullyQualifiedName(method.declaringType) === 'fs') {
                        return method.name;
                    }
                }
                return null;
            });

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import {readFile as rf} from 'fs';

                                rf('test.txt', (err, data) => {
                                    console.log(data);
                                });
                            `,
                            //@formatter:off
                            `
                                import {readFile as rf} from 'fs';

                                /*~~(readFile)~~>*/rf('test.txt', (err, data) => {
                                    console.log(data);
                                });
                            `
                            //@formatter:on
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/node": "^20"
                                }
                              }
                            `
                        )
                    )
                )
            }, {unsafeCleanup: true});
        })

        test('default export function call has <default> method name', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (Type.isMethod(type)) {
                    const method = type as Type.Method;
                    if (FullyQualified.getFullyQualifiedName(method.declaringType) === 'express') {
                        return `${FullyQualified.getFullyQualifiedName(method.declaringType)} ${method.name}`;
                    }
                }
                return null;
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import express from 'express';
                                const app = express();
                            `,
                            //@formatter:off
                            `
                                import express from 'express';
                                const app = /*~~(express <default>)~~>*/express();
                            `
                            //@formatter:on
                        ),
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "express": "^4.18.2"
                                },
                                "devDependencies": {
                                  "@types/express": "^4.17.21"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });

        test('deprecated node methods with ES6 imports', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((_, type) => {
                return Type.isMethod(type) && type.name === 'isArray' ? FullyQualified.getFullyQualifiedName(type.declaringType) : null;
            });

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import * as util from 'util';

                                util.isArray([])
                            `,
                            //@formatter:off
                            `
                                import * as util from 'util';

                                /*~~(util)~~>*/util.isArray([])
                            `
                            //@formatter:on
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/node": "^20"
                                }
                              }
                            `
                        )
                    )
                )
            }, {unsafeCleanup: true});
        });

        test('should map tsx types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((_, type) => {
                // Mark any node that has a react-spinners type
                if (Type.isClass(type) && type.fullyQualifiedName.includes('react-spinners')) {
                    // TODO: Supertype resolution for library types needs investigation
                    // The supertype should be React.Component but is currently undefined
                    // This appears to be a separate issue from parameterized type handling
                    return type.fullyQualifiedName;
                } else {
                    return null;
                }
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        //language=tsx
                        tsx(
                            `
                                import {ClipLoader} from 'react-spinners';

                                const App = () => {
                                    return <ClipLoader color="#36d7b7"/>;
                                };
                            `,
                            `
                                import {/*~~(react-spinners.ClipLoader)~~>*/ClipLoader} from 'react-spinners';

                                const App = () => {
                                    return </*~~(react-spinners.ClipLoader)~~>*/ClipLoader color="#36d7b7"/>;
                                };
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "react-spinners": "^0.5.0"
                                },
                                "devDependencies": {
                                  "@types/react": "^16.8.0"
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

        test('should map array types as class types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.NewArray) {
                    // Arrays can be either Class or Parameterized (if they have type arguments)
                    if (Type.isParameterized(type)) {
                        // For parameterized arrays, get the base type name
                        if (Type.isClass(type.type) && type.type.fullyQualifiedName === 'Array') {
                            return 'Array';
                        }
                    } else if (Type.isClass(type)) {
                        if (type.fullyQualifiedName === 'Array') {
                            return 'Array';
                        }
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
                        let numbers: number[] = /*~~(Array)~~>*/[1, 2, 3];
                        let strings: Array<string> = /*~~(Array)~~>*/["a", "b"];
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

        test('should map readonly array types as class types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                // Mark array literals assigned to readonly arrays - arrays are now class types
                if (node?.kind === J.Kind.NewArray) {
                    // Arrays can be either Class or Parameterized (if they have type arguments)
                    if (Type.isParameterized(type)) {
                        // For parameterized arrays (including ReadonlyArray<T>), get the base type name
                        if (Type.isClass(type.type) && (type.type.fullyQualifiedName === 'Array' || type.type.fullyQualifiedName === 'ReadonlyArray')) {
                            return 'Array';
                        }
                    } else if (Type.isClass(type)) {
                        // Both readonly and regular arrays should be mapped as Array
                        if (type.fullyQualifiedName === 'Array' || type.fullyQualifiedName === 'ReadonlyArray') {
                            return 'Array';
                        }
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
                        const readonlyNumbers: readonly number[] = /*~~(Array)~~>*/[1, 2, 3];
                        const readonlyStrings: ReadonlyArray<string> = /*~~(Array)~~>*/["a", "b"];
                        const frozenArray = Object.freeze(/*~~(Array)~~>*/[1, 2, 3]);
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
                        /*~~((a, b) => double)~~>*/
                        function add(a: number, b: number): number {
                            return a + b;
                        }
                    `
                )
            );
        });

        test.skip('should map arrow function types', async () => {
            // TODO: Arrow functions need special handling - methodType might not be attached to Lambda
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, _type) => {
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
            spec.recipe = markTypes((node, _type) => {
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

        test('should auto-box primitives when methods are called on them', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, _type) => {
                // Mark toString() invocations and verify the declaring type is boxed
                if (node?.kind === J.Kind.MethodInvocation) {
                    const invocation = node as J.MethodInvocation;
                    const methodType = invocation.methodType;
                    if (methodType && methodType.name === 'toString') {
                        // Verify that declaringType is NOT a primitive
                        const isPrimitive = Type.isPrimitive(methodType.declaringType);
                        const declaringTypeName = Type.isClass(methodType.declaringType) ?
                            methodType.declaringType.fullyQualifiedName :
                            'PRIMITIVE';

                        return `toString on ${declaringTypeName} (isPrimitive: ${isPrimitive})`;
                    }
                    return null;
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = "test";
                        const hex = a.charCodeAt(0).toString(16);
                    `,
                    `
                        const a = "test";
                        const hex = /*~~(toString on Number (isPrimitive: false))~~>*/a.charCodeAt(0).toString(16);
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
    });

    describe('generic types', () => {
        test('should create Parameterized type for Array<string>', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'arr') {
                    // Verify we get a Parameterized type, not just a Class
                    if (Type.isParameterized(type)) {
                        const parameterized = type as Type.Parameterized;
                        // Check that the base type is Array
                        const baseTypeName = Type.isClass(parameterized.type) ?
                            parameterized.type.fullyQualifiedName : 'NOT_CLASS';
                        // Check that we have one type parameter that is String
                        const typeArgCount = parameterized.typeParameters.length;
                        const firstArgType = Type.isPrimitive(parameterized.typeParameters[0]) ?
                            parameterized.typeParameters[0].keyword : 'NOT_PRIMITIVE';
                        return `Parameterized<${baseTypeName}, args=${typeArgCount}, first=${firstArgType}>`;
                    } else if (Type.isClass(type)) {
                        return `Class<${type.fullyQualifiedName}>`;
                    }
                    return 'OTHER_TYPE';
                }
                return null;
            });

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const arr: Array<string> = [];
                    `,
                    `
                        const /*~~(Parameterized<Array, args=1, first=String>)~~>*/arr: Array<string> = [];
                    `
                )
            );
        });

        test('should map plain generic type (Array)', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'arr') {
                    if (Type.isFullyQualified(type)) {
                        return FullyQualified.getFullyQualifiedName(type);
                    }
                    return formatPrimitiveType(type) || 'OTHER_TYPE';
                }
                return null;
            });

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const arr: Array<string> = [];
                    `,
                    `
                        const /*~~(Array)~~>*/arr: Array<string> = [];
                    `
                )
            );
        });

        test('should handle circular references through parameterized types', async () => {
            const spec = new RecipeSpec();
            let foundCircularRef = false;

            spec.recipe = markTypes((node, type) => {
                // Check Array<Node> usage in the children field
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'children') {
                    if (Type.isParameterized(type)) {
                        const paramType = type as Type.Parameterized;
                        // Verify it's Array with a type argument
                        if (Type.isClass(paramType.type) && paramType.type.fullyQualifiedName === 'Array') {
                            const typeArg = paramType.typeParameters[0];
                            if (Type.isClass(typeArg) && typeArg.fullyQualifiedName === 'Node') {
                                foundCircularRef = true;
                                // Don't mark - just track that we found it
                            }
                        }
                    }
                }
                return null;
            });

            // This should not cause an infinite loop despite the circular reference
            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        class Node {
                            value: string;
                            children: Array<Node>;
                        }
                    `
                )
            );

            // Verify we found the circular Array<Node> reference
            expect(foundCircularRef).toBe(true);
        });

        test('should share base class between different parameterizations', async () => {
            const spec = new RecipeSpec();
            let baseClassFromStringArray: Type.Class | undefined;
            let baseClassFromNumberArray: Type.Class | undefined;

            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier) {
                    const id = node as J.Identifier;
                    if (id.simpleName === 'arr1' && Type.isParameterized(type)) {
                        baseClassFromStringArray = type.type as Type.Class;
                        return `Parameterized<${(type.type as Type.Class).fullyQualifiedName}, ${type.typeParameters.length}>`;
                    }
                    if (id.simpleName === 'arr2' && Type.isParameterized(type)) {
                        baseClassFromNumberArray = type.type as Type.Class;
                        return `Parameterized<${(type.type as Type.Class).fullyQualifiedName}, ${type.typeParameters.length}>`;
                    }
                }
                return null;
            });

            await spec.rewriteRun(
                typescript(
                    `
                        const arr1: Array<string> = [];
                        const arr2: Array<number> = [];
                    `,
                    `
                        const /*~~(Parameterized<Array, 1>)~~>*/arr1: Array<string> = [];
                        const /*~~(Parameterized<Array, 1>)~~>*/arr2: Array<number> = [];
                    `
                )
            );

            // Verify that Array<string> and Array<number> share the same base class instance
            expect(baseClassFromStringArray).toBeDefined();
            expect(baseClassFromNumberArray).toBeDefined();
            expect(baseClassFromStringArray).toBe(baseClassFromNumberArray);
        });

        test('should map generic interface from library', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'promise') {
                    if (Type.isFullyQualified(type)) {
                        return FullyQualified.getFullyQualifiedName(type);
                    }
                    return formatPrimitiveType(type) || 'OTHER_TYPE';
                }
                return null;
            });

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const promise: Promise<number> = Promise.resolve(42);
                    `,
                    `
                        const /*~~(Promise)~~>*/promise: Promise<number> = Promise.resolve(42);
                    `
                )
            );
        });
    });

    describe('generic type variables', () => {
        test('should map simple type parameter', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'T') {
                    if (Type.isGenericTypeVariable(type)) {
                        return `GenericTypeVariable<${type.name}, variance=${type.variance}, bounds=${type.bounds?.length || 0}>`;
                    }
                    return 'NOT_GENERIC';
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
                        function identity</*~~(GenericTypeVariable<T, variance=Invariant, bounds=0>)~~>*/T>(value: /*~~(GenericTypeVariable<T, variance=Invariant, bounds=0>)~~>*/T): /*~~(GenericTypeVariable<T, variance=Invariant, bounds=0>)~~>*/T {
                            return value;
                        }
                    `
                )
            );
        });

        test('should map type parameter with constraint', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'T') {
                    if (Type.isGenericTypeVariable(type)) {
                        const boundsInfo = type.bounds && type.bounds.length > 0 ?
                            `bounds=${type.bounds.length}, first=${Type.isPrimitive(type.bounds[0]) ? type.bounds[0].keyword : Type.isClass(type.bounds[0]) ? (type.bounds[0] as Type.Class).fullyQualifiedName : 'OTHER'}` :
                            'bounds=0';
                        return `GenericTypeVariable<${type.name}, variance=${type.variance}, ${boundsInfo}>`;
                    }
                    return 'NOT_GENERIC';
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        function getValue<T extends string>(value: T): T {
                            return value;
                        }
                    `,
                    `
                        function getValue</*~~(GenericTypeVariable<T, variance=Covariant, bounds=1, first=String>)~~>*/T extends string>(value: /*~~(GenericTypeVariable<T, variance=Covariant, bounds=1, first=String>)~~>*/T): /*~~(GenericTypeVariable<T, variance=Covariant, bounds=1, first=String>)~~>*/T {
                            return value;
                        }
                    `
                )
            );
        });

        test('should map multiple type parameters', async () => {
            const spec = new RecipeSpec();
            const foundTypes: string[] = [];

            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier) {
                    const id = node as J.Identifier;
                    if ((id.simpleName === 'K' || id.simpleName === 'V') && Type.isGenericTypeVariable(type)) {
                        foundTypes.push(`${type.name}:variance=${type.variance}`);
                        // Don't mark - just track what we find
                    }
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        function createMap<K, V>(key: K, value: V): Map<K, V> {
                            return new Map([[key, value]]);
                        }
                    `
                )
            );

            // Verify we found both K and V (variance=Invariant means Invariant)
            expect(foundTypes).toContain('K:variance=Invariant');
            expect(foundTypes).toContain('V:variance=Invariant');
        });
    });

    describe('union and intersection types', () => {
        test('should map union type', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'value') {
                    if (Type.isUnion(type)) {
                        // Check bounds - should be string and number primitives
                        const boundTypes = type.bounds.map(b =>
                            Type.isPrimitive(b) ? b.keyword : Type.isClass(b) ? (b as Type.Class).fullyQualifiedName : 'OTHER'
                        );
                        return `Union[${boundTypes.join(', ')}]`;
                    }
                    return 'NOT_UNION';
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        let value: string | number = "hello";
                    `,
                    `
                        let /*~~(Union[String, double])~~>*/value: string | number = "hello";
                    `
                )
            );
        });

        test('should map intersection type', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'obj') {
                    if (Type.isIntersection(type)) {
                        // Check bounds count
                        return `Intersection[${type.bounds.length} types]`;
                    }
                    return 'NOT_INTERSECTION';
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        interface A { a: string; }
                        interface B { b: number; }
                        let obj: A & B = { a: "test", b: 42 };
                    `,
                    `
                        interface A { a: string; }
                        interface B { b: number; }
                        let /*~~(Intersection[2 types])~~>*/obj: A & B = { a: "test", b: 42 };
                    `
                )
            );
        });

        test('should map complex union with multiple types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'mixed') {
                    if (Type.isUnion(type)) {
                        return `Union[${type.bounds.length} types]`;
                    }
                    return 'NOT_UNION';
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        let mixed: string | number | boolean | null = "test";
                    `,
                    `
                        let /*~~(Union[4 types])~~>*/mixed: string | number | boolean | null = "test";
                    `
                )
            );
        });

        test('should handle union of primitives and class types', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'mixed') {
                    if (Type.isUnion(type)) {
                        // Check if we have both primitives and classes
                        const hasPrimitive = type.bounds.some(b => Type.isPrimitive(b));
                        const hasClass = type.bounds.some(b => Type.isClass(b));
                        if (hasPrimitive && hasClass) {
                            return 'Union[primitive+class]';
                        }
                    }
                    return 'OTHER';
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        class MyClass {}
                        let mixed: string | MyClass = "test";
                    `,
                    `
                        class MyClass {}
                        let /*~~(Union[primitive+class])~~>*/mixed: string | MyClass = "test";
                    `
                )
            );
        });

        test('should handle self-referential union types without infinite recursion', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'json') {
                    if (Type.isUnion(type)) {
                        // This should not cause infinite recursion when computing signature
                        try {
                            const sig = Type.signature(type);
                            // If we got here without stack overflow, the test passes
                            // The signature should contain "<cyclic union>" for the self-referential part
                            return sig.includes('<cyclic union>') ? 'SUCCESS: handled cyclic union' : `Signature: ${sig.substring(0, 50)}...`;
                        } catch (e) {
                            return `ERROR: ${e instanceof Error ? e.message : 'Unknown error'}`;
                        }
                    }
                    return 'NOT_UNION';
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        type Json = string | number | boolean | null | Json[] | { [key: string]: Json };
                        let json: Json = "test";
                    `,
                    `
                        type Json = string | number | boolean | null | Json[] | { [key: string]: Json };
                        let /*~~(SUCCESS: handled cyclic union)~~>*/json: Json = "test";
                    `
                )
            );
        });

        test('should handle self-referential intersection types without infinite recursion', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'obj') {
                    if (type) {
                        // This should not cause infinite recursion when computing signature
                        try {
                            const sig = Type.signature(type);
                            // If we got here without stack overflow, the test passes
                            return `SUCCESS: computed signature (${sig.length} chars)`;
                        } catch (e) {
                            return `ERROR: ${e instanceof Error ? e.message : 'Unknown error'}`;
                        }
                    }
                    return 'NO_TYPE';
                }
                return null;
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        interface A { prop: B & C }
                        interface B { prop: A }
                        interface C { other: string }
                        let obj: A = { prop: { prop: null as any, other: "test" } };
                    `,
                    // Signature computation should succeed without infinite recursion
                    `
                        interface A { prop: B & C }
                        interface B { prop: A }
                        interface C { other: string }
                        let /*~~(SUCCESS: computed signature (1 chars))~~>*/obj: A = { prop: { prop: null as any, other: "test" } };
                    `
                )
            );
        });
    });

    describe('type aliases', () => {
        test('should map plain type alias to underlying type', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'value') {
                    // When type has aliasSymbol, we should map it to the underlying type
                    if (Type.isFullyQualified(type)) {
                        return FullyQualified.getFullyQualifiedName(type);
                    }
                    return formatPrimitiveType(type);
                }
                return null;
            });

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        type StringAlias = string;
                        const value: StringAlias = "hello";
                    `,
                    `
                        type StringAlias = string;
                        const /*~~(String)~~>*/value: StringAlias = "hello";
                    `
                )
            );
        });

        test('should map namespace-qualified parameterized type (React.Ref)', async () => {
            const spec = new RecipeSpec();
            let reactRefType: Type | undefined;

            spec.recipe = new class extends Recipe {
                name = 'Type checker';
                displayName = 'Check React.Ref type';
                description = 'Verify React.Ref type attribution';

                async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
                    return new class extends JavaScriptVisitor<ExecutionContext> {
                        async visitParameterizedType(paramType: J.ParameterizedType, p: ExecutionContext): Promise<J.ParameterizedType> {
                            const visited = await super.visitParameterizedType(paramType, p) as J.ParameterizedType;

                            // Check if this is React.Ref
                            if (paramType.class.kind === J.Kind.FieldAccess) {
                                const fa = paramType.class as J.FieldAccess;
                                const targetName = fa.target.kind === J.Kind.Identifier ?
                                    (fa.target as J.Identifier).simpleName : '';
                                const fieldName = fa.name.simpleName;

                                if (targetName === 'React' && fieldName === 'Ref') {
                                    reactRefType = paramType.type;
                                }
                            }

                            return visited;
                        }
                    }
                }
            };

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        //language=tsx
                        tsx(
                            `
                                import React from "react";

                                interface ButtonProps {
                                    ref?: React.Ref<HTMLButtonElement>
                                }

                                const Button = (props: ButtonProps) => <button ref={props.ref} />;
                            `
                        ),
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/react": "^19.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, { unsafeCleanup: true });

            // Verify React.Ref<HTMLButtonElement> has correct type attribution
            expect(reactRefType).toBeDefined();

            // React.Ref<T> is a type alias that resolves to a Union type
            expect(Type.isUnion(reactRefType!)).toBe(true);

            const unionType = reactRefType as Type.Union;

            // Find the RefObject bound (the second constituent after the callback function)
            const refObjectBound = unionType.bounds.find(b =>
                Type.isParameterized(b) &&
                Type.isClass(b.type) &&
                b.type.fullyQualifiedName.includes('RefObject')
            );

            expect(refObjectBound).toBeDefined();

            // Verify the type parameter HTMLButtonElement is preserved
            const parameterizedBound = refObjectBound as Type.Parameterized;
            expect(parameterizedBound.typeParameters).toHaveLength(1);

            expect(Type.isClass(parameterizedBound.typeParameters[0])).toBe(true);
            expect((parameterizedBound.typeParameters[0] as Type.Class).fullyQualifiedName).toBe('HTMLButtonElement');
        });
    });

    describe('complex file parsing without stack overflow', () => {
        test('should handle recursive mapped types like DeepReadonly without stack overflow', async () => {
            // This test verifies that TypeScript's infinite type instantiation
            // is properly handled with recursion depth limits
            const spec = new RecipeSpec();

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        // Recursive mapped type - TypeScript creates new type objects at each instantiation level
                        type DeepReadonly<T> = { readonly [P in keyof T]: DeepReadonly<T[P]> };

                        interface NestedObject {
                            a: {
                                b: {
                                    c: {
                                        d: {
                                            e: string;
                                        }
                                    }
                                }
                            }
                        }

                        // This will trigger infinite type instantiation as TypeScript tries to resolve
                        // DeepReadonly<NestedObject> → DeepReadonly<{ a: ... }> → DeepReadonly<{ b: ... }> → ...
                        const obj: DeepReadonly<NestedObject> = {
                            a: {
                                b: {
                                    c: {
                                        d: {
                                            e: "test"
                                        }
                                    }
                                }
                            }
                        };
                    `
                )
            );

            // If we got here without stack overflow, the test passes
            expect(true).toBe(true);
        });

        test('minimal reproducer - just imports', async () => {
            // Start with just the imports that might cause the issue
            const spec = new RecipeSpec();

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        tsx(
                            `
                                import { render, act, fireEvent } from '@testing-library/react';
                                import React, { type JSX } from 'react';

                                // Empty file - just imports
                            `
                        ),
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/react": "^19.0.0",
                                  "@testing-library/react": "^16.1.0",
                                  "react": "^19.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, { unsafeCleanup: true });

            expect(true).toBe(true);
        });

        test('minimal reproducer - with render call', async () => {
            // Add a simple render call
            const spec = new RecipeSpec();

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        tsx(
                            `
                                import { render, act } from '@testing-library/react';
                                import React from 'react';

                                const App = () => <div>Test</div>;
                                render(<App />);
                            `
                        ),
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/react": "^19.0.0",
                                  "@testing-library/react": "^16.1.0",
                                  "react": "^19.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, { unsafeCleanup: true });

            expect(true).toBe(true);
        });

        test('minimal reproducer - with act', async () => {
            // Add act() which might trigger complex types
            const spec = new RecipeSpec();

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        tsx(
                            `
                                import { act } from '@testing-library/react';

                                act(() => {
                                    console.log("test");
                                });
                            `
                        ),
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/react": "^19.0.0",
                                  "@testing-library/react": "^16.1.0",
                                  "react": "^19.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, { unsafeCleanup: true });

            expect(true).toBe(true);
        });
    });

    describe('variable types (fieldType)', () => {
        test('should map imported variable reference with owner type', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'vi') {
                    // Check fieldType for variable attribution
                    const identifier = node as J.Identifier;
                    const fieldType = identifier.fieldType;
                    if (fieldType?.kind === Type.Kind.Variable) {
                        const varType = fieldType as Type.Variable;
                        const ownerName = varType.owner ?
                            Type.FullyQualified.getFullyQualifiedName(varType.owner) : 'no-owner';
                        return `Variable<name=${varType.name}, owner=${ownerName}>`;
                    }
                    return 'NOT_VARIABLE';
                }
                return null;
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import {vi} from 'vitest';

                                function example() {
                                    const mock = vi.fn();
                                }
                            `,
                            `
                                import {/*~~(Variable<name=vi, owner=vitest>)~~>*/vi} from 'vitest';

                                function example() {
                                    const mock = /*~~(Variable<name=vi, owner=vitest>)~~>*/vi.fn();
                                }
                            `
                        ),
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "vitest": "^2.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });

        test('should map function imports as Variable with owner', async () => {
            // Functions imported from modules are also represented as Variables.
            // The Variable's `owner` property contains the module they come from.
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, _type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'describe') {
                    const identifier = node as J.Identifier;
                    const fieldType = identifier.fieldType;
                    if (fieldType?.kind === Type.Kind.Variable) {
                        const varType = fieldType as Type.Variable;
                        const ownerName = varType.owner ?
                            Type.FullyQualified.getFullyQualifiedName(varType.owner) : 'no-owner';
                        return `Variable<name=${varType.name}, owner=${ownerName}>`;
                    }
                    return 'NOT_VARIABLE';
                }
                return null;
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import {describe} from 'vitest';

                                describe('test', () => {});
                            `,
                            // describe is a Variable with vitest as owner
                            `
                                import {/*~~(Variable<name=describe, owner=vitest>)~~>*/describe} from 'vitest';

                                /*~~(Variable<name=describe, owner=vitest>)~~>*/describe('test', () => {});
                            `
                        ),
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "vitest": "^2.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });

        test('should map variable used as standalone assignment', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markTypes((node, type) => {
                if (node?.kind === J.Kind.Identifier && (node as J.Identifier).simpleName === 'vi') {
                    const identifier = node as J.Identifier;
                    const fieldType = identifier.fieldType;
                    if (fieldType?.kind === Type.Kind.Variable) {
                        const varType = fieldType as Type.Variable;
                        const ownerName = varType.owner ?
                            Type.FullyQualified.getFullyQualifiedName(varType.owner) : 'no-owner';
                        return `Variable<owner=${ownerName}>`;
                    }
                    return 'NOT_VARIABLE';
                }
                return null;
            });

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import {vi} from 'vitest';

                                const mockUtils = vi;
                            `,
                            `
                                import {/*~~(Variable<owner=vitest>)~~>*/vi} from 'vitest';

                                const mockUtils = /*~~(Variable<owner=vitest>)~~>*/vi;
                            `
                        ),
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "vitest": "^2.0.0"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });
    });

    test('CommonJS require imports distinguish methods with identical signatures', async () => {
        const spec = new RecipeSpec();
        spec.recipe = markTypes((_, type) => {
            return Type.isMethod(type) && type.name === 'isArray' ? FullyQualified.getFullyQualifiedName(type.declaringType) : null;
        });

        //language=typescript
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(
                        `
                                const util = require('util');

                                util.isArray([]);
                                util.isString("not an array");
                            `,
                        //@formatter:off
                        `
                                const util = require('util');

                                /*~~(util)~~>*/util.isArray([]);
                                util.isString("not an array");
                            `
                        //@formatter:on
                    ),
                    //language=json
                    packageJson(
                        `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/node": "^20"
                                }
                              }
                            `
                    )
                )
            )
        }, {unsafeCleanup: true});
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
