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

describe('arrow mapping', () => {
    const spec = new RecipeSpec();

    test('function with simple body', () => spec.rewriteRun(
        //language=typescript
        typescript(`
                const multiply = (a: number, b: number): number => a * b;
            `
        )
    ));

    test('function with empty body', () => spec.rewriteRun(
        //language=typescript
        typescript(`
                const empty = (/*a*/) /*b*/ => {/*c*/
                };
            `
        )
    ));

    test('function with simple body and comments', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const multiply = /*0*/ (/*1*/a/*2*/: /*3*/number/*4*/,/*5*/ b: /*6*/ number/*7*/) /*a*/: /*b*/ number /*c*/ => a * b;
        `)
    ));

    test('function with body', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            let sum = (x: number, y: number): number => {
                return x + y;
            }
        `)
    ));

    test('function without params', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const greet = (/*no*/): string => 'Hello';
        `)
    ));

    test('function with implicit return', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            let sum = (x: number, y: number) => x + y;
        `)
    ));

    test('function with trailing comma', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            let sum = (x: number, y: number /*a*/, /*b*/) => x + y;
        `)
    ));

    test('function with async modifier', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            let sum = async (x: number, y: number) => x + y;
        `)
    ));

    test('basic async arrow function', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const fetchData = async (): Promise<string> => {
                return new Promise((resolve) => {
                    setTimeout(() => {
                        resolve("Data fetched successfully!");
                    }, 2000);
                });
            };

            // Using the function
            fetchData().then((message) => {
                console.log(message); // Outputs: Data fetched successfully! (after 2 seconds)
            });
        `)
    ));

    test('function with async modifier and comments', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            let sum = /*a*/async /*b*/(/*c*/x: number, y: number /*d*/) /*e*/ => x + y;
        `)
    ));

    test('function with implicit return and comments', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            let sum = (x: number, y: number) /*a*/ => /*b*/ x + y;
        `)
    ));

    test('function with default parameter', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const increment = (value: number, step: number = 1): number => value + step;
        `)
    ));

    test('function with default parameter and comments', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const increment = (value: number, step: /*a*/ number /*b*/ = /*c*/1 /*d*/): number => value + step;
        `)
    ));

    test('with generic type', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const echo = <T>(input: T): T => input;
        `)
    ));

    test('with generic type and comments', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const echo = /*a*/</*0*/T/*1*/>/*b*/(/*c*/input: T/*d*/)/*e*/: T => input;
        `)
    ));

    test('no paren', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const echo = input => input;
        `)
    ));

    test('no paren with comments', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const echo = /*a*/input/*b*/ => input;
        `)
    ));

    test('typed with dimond cast', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            export const addTodo3 = (text: string) => <AddTodoAction>({
                type: "ADD_TODO",
                text
            })
        `)
    ));

    test('arrow function with empty object binding', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            ({/*a*/}) => ({/*b*/})
        `)
    ));

    test('arrow function with const type param', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            class A {
                prop: </*a*/const /*b*/ S extends SchemaObj, A, E>(
                    name: string,
                    schemas: S,
                    self: TestFunction<
                        A,
                        E,
                        R,
                        [{ [K in keyof S]: Schema.Schema.Type<S[K]> }, V.TaskContext<V.RunnerTestCase<{}>> & V.TestContext]
                    >,
                    timeout?: number | V.TestOptions
                ) => void;
            }
        `)
    ));

});
