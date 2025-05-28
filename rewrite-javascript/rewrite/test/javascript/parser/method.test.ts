// noinspection TypeScriptValidateTypes,TypeScriptUnresolvedReference,JSUnusedLocalSymbols

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
import {typescript} from "../../../src/javascript";

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
});
