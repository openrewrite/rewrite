// noinspection JSUnusedLocalSymbols,TypeScriptUnresolvedReference,TypeScriptValidateTypes,ThisExpressionReferencesGlobalObjectJS

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
import {JS, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('expression statement mapping', () => {
    const spec = new RecipeSpec();

    test('literal with semicolon', () => spec.rewriteRun(
        typescript('1 ;')
    ));

    test('multiple', () => spec.rewriteRun(
        typescript(
            //language=ts
            `
                1; // foo
                // bar
                /*baz*/
                2;`
        )
    ));

    test('simple non-null expression', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const length = user !.profile !.username !.length;
        `)
    ));

    test('simple non-null expression with comments', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const length = /*0*/user/*a*/!/*b*/./*c*/profile /*d*/!/*e*/./*f*/username /*g*/!/*h*/./*j*/length/*l*/;
        `)
    ));

    test('simple question-dot expression', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const length = user?.profile?.username?.length;
        `)
    ));

    test('simple question-dot expression with comments', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const length = /*0*/user/*a*/?./*b*/profile/*c*/?./*d*/username /*e*/?./*f*/length /*g*/;
        `)
    ));

    test('simple default expression', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const length = user ?? 'default';
        `)
    ));

    test('simple default expression with comments', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const length = user /*a*/ ??/*b*/ 'default' /*c*/;
        `)
    ));

    test('mixed expression with special tokens', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            class Profile {
                username?: string; // Optional property
            }

            class User {
                profile?: Profile; // Optional property
            }

            function getUser(id: number): User | null {
                if (id === 1) {
                    return new User();
                }
                return null;
            }

            const user = getUser(1);
            const length2 = user  !.profile?.username  !.length /*test*/;
            const username2 = getUser(1) !.profile?.username; // test;
            const username = user!.profile?.username ?? 'Guest';
        `)
    ));

    test('mixed expression with methods with special tokens', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            interface Profile {
                username?(): string; // Optional property

            }

            interface User {
                profile?(): Profile; // Optional property
            }

            function getUser(id: number): User | null {
                return null;
            }

            const user = getUser(1);
            const username1 = user  !.profile()?.username()  !.toLowerCase() /*test*/;
            const username2 = getUser(1) !.profile()?.username(); // test;
            const username3 = getUser(1)?.profile()?.username() ?? 'Guest';
        `)
    ));

    test('optional chaining operator with ?.', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const func1 = (msg: string) => {
                return {
                    func2: (greeting: string) => greeting + msg
                };
            };

            const result1 = func1?.("World")?.func2("Hello"); // Invokes func1 and then func2 if func1 is not null/undefined.
        `)
    ));

    test('optional chaining operator with ?. and custom type', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const func1: ((msg: string) => { func2: (greeting: string) => string }) | undefined = undefined;
            const result2 = func1?.("Test")?.func2("Hi"); // Does not invoke and returns \`undefined\`.
        `)
    ));

    test('satisfies expression', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            // noinspection JSAnnotator

            type Person = {
                name: string;
                age: number;
            };

            const user = /*o*/ {
                name: "Alice",
                age: 25,
                occupation: "Engineer"
            } /*a*/ satisfies /*b*/ Person /*c*/;
        `)
    ));

    test('satisfies expression with complex type ', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            // noinspection JSAnnotator

            type ApiResponse<T> = {
                data: T;
                status: "success" | "error";
            };

            const response = {
                data: {userId: 1},
                status: "success",
            } satisfies ApiResponse<{ userId: number }>;
        `)
    ));

    test('debugging statement', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            function calculate(value: number) {
                /*a*/
                debugger/*b*/;/*c*/ // Pauses execution when debugging
                return value * 2;
            }
        `)
    ));

    test('shorthand property assignment with initializer', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            ({
                initialState,
                resultSelector/*a*/ = /*b*/identity as ResultFunc<S, T>,
            } = initialStateOrOptions as GenerateOptions<T, S>);
        `)
    ));

    test('new expression with array access', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const results = new this.constructor[Symbol.species]<Key, Value>();
        `)
    ));

    test('method invocation', () => spec.rewriteRun({
        //language=typescript
        ...typescript(`console.log("Hello");`),
        afterRecipe : (cu: JS.CompilationUnit) => {
            const mi = cu.statements[0] as unknown as J.MethodInvocation;
            expect(mi.select!.kind).toEqual(J.Kind.Identifier);
            expect(mi.name.simpleName).toEqual("log");
        }
        }));
});
