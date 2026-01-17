// noinspection TypeScriptValidateTypes,TypeScriptUnresolvedReference,JSUnusedLocalSymbols,TypeScriptCheckImport

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
import {RecipeSpec} from "../../../src/test";
import {JavaScriptVisitor, JS, npm, packageJson, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {withDir} from "tmp-promise";

describe('method mapping', () => {
    const spec = new RecipeSpec();

    test('simple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test() {
                        // hello world comment
                    }
                }
            `)
        ));

    test('string name', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript(`
                class Handler {
                    'foo bar'() {
                        // hello world comment
                    }
                }
            `),
            afterRecipe: (cu: JS.CompilationUnit) => {
                let method = (cu.statements[0] as unknown as J.ClassDeclaration).body.statements[0] as unknown as J.MethodDeclaration;
                expect(method.name.kind).toBe(J.Kind.Identifier);
            }
        }));

    test('single parameter', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test(input) {
                        // hello world comment
                    }
                }
            `)
        ));

    test('single typed parameter', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test(input: string) {
                        // hello world comment
                    }
                }
            `)
        ));

    test('single typed parameter with initializer', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test(input  /*asda*/: string =    /*asdas */ "hello world") {
                        // hello world comment
                    }
                }
            `)
        ));

    test('single parameter with initializer', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test(input = 1) {
                        // hello world comment
                    }
                }
            `)
        ));

    test('multi parameters', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test(input: string, a = 1, test: number) {
                        // hello world comment
                    }
                }
            `)
        ));

    test('parameter with trailing comma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test(input: string,) {
                        // hello world comment
                    }
                }
            `)
        ));

    test('optional parameter with trailing comma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test(input   ?: string,) {
                        // hello world comment
                    }
                }
            `)
        ));

    test('type parameters', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test<T>(input: T,) {
                        // hello world comment
                    }
                }
            `)
        ));

    test('type parameters with bounds', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test<T extends string>(input: T,) {
                        // hello world comment
                    }
                }
            `)
        ));

    test('return type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler {
                    test(input: string,) /*1*/: /*asda*/ string {
                        // hello world comment
                        return input;
                    }
                }
            `)
        ));

    test('method with generics', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Handler<T1, T2> {
                    test<T3>(input: string, t3: T3) /*1*/: /*asda*/ string {
                        // hello world comment
                        return input;
                    }
                }
            `)
        ));

    test('method with ComputedPropertyName', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const asyncIterable = {
                    [Symbol.asyncIterator]() {
                        return {
                            async next() {
                                return {value: undefined, done: true};
                            },
                        };
                    },
                };
            `)
        ));

    test('method signature with ComputedPropertyName', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const greetSymbol = Symbol("greet");

                interface Greeter {
                    /*a*/
                    [/*b*/greetSymbol/*c*/]/*d*/(message: string): void; // Computed method name
                }

                const greeter: Greeter = {
                    [greetSymbol](message: string): void {
                        console.log(message);
                    },
                };
            `)
        ));

    test('extends as a call expression', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export class ResultLengthMismatch extends TypeIdError(SqlErrorTypeId, "ResultLengthMismatch")<{}> {
                }
            `)
        ));

    test('method name as string literal', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export interface IEnumerable<T> extends Iterable<T> {
                    /*a*/
                    "System.Collections.IEnumerable.GetEnumerator"/*b*/(/*c*/): IEnumerator<any>;
                }
            `)
        ));

    test('generator with computed property name', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class R {
                    * [Symbol.iterator](): Generator<number> {
                        yield 1;
                    }
                }
            `)
        ));

    test("type attribution on third-party library", async () => {
        await withDir(async repo => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {
                        //language=typescript
                        ...typescript(
                            `
                                import _ from 'lodash';

                                const result = _.map([1, 2, 3], n => n * 2);
                            `
                        ),
                        afterRecipe: async cu => {
                            await (new class extends JavaScriptVisitor<any> {
                                protected async visitMethodInvocation(method: J.MethodInvocation, _: any): Promise<J | undefined> {
                                    expect(method.methodType?.name).toEqual('map')
                                    return method;
                                }
                            }).visit(cu, 0);
                        }
                    },
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
});
