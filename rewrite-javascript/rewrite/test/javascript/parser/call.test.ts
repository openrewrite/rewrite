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
import {J, TextComment} from "../../../src/java";

describe('call mapping', () => {
    const spec = new RecipeSpec();

    test('single', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('parseInt("42")')
        ));

    test('multiple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('setTimeout(null, 2000, \'Hello\');')
        ));

    test('with array literal receiver', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('[1] . splice(0)')
        ));

    test('with call receiver', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('"1" . substring(0) . substring(0)')
        ));

    test('trailing comma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('parseInt("42" , )')
        ));

    test('with optional chaining operator', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript(`
                const func = (message: string) => message;
                const result0 = func/*a*/?./*b*/("TS");
                const result1 = func/*a*/?./*b*/call("TS");
                const result2 = "hi"/*a*/./*b*/toUpperCase(); // usual call without optional chaining
            `),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const inits = [1, 2, 3].map(i => (cu.statements[i] as unknown as J.VariableDeclarations).variables[0].initializer!);
                expect(inits[0].kind).toEqual(JS.Kind.FunctionCall);
                expect(inits[1].kind).toEqual(J.Kind.MethodInvocation);
                expect(inits[2].kind).toEqual(J.Kind.MethodInvocation);

                for (let i = 0; i <= 2; i++) {
                    const select = i == 0 ? (inits[i] as unknown as JS.FunctionCall).function! : (inits[i] as unknown as J.MethodInvocation).select!;
                    expect(select.padding.after.whitespace).toEqual("");
                    expect(select.padding.after.comments.length).toEqual(1);
                    expect((select.padding.after.comments[0] as TextComment).text).toEqual("a");
                }

                expect(((inits[0] as unknown as JS.FunctionCall).arguments.before.comments[0] as TextComment).text).toEqual("b");
                expect(((inits[1] as unknown as J.MethodInvocation).name.prefix.comments[0] as TextComment).text).toEqual("b");
                expect(((inits[2] as unknown as J.MethodInvocation).name.prefix.comments[0] as TextComment).text).toEqual("b");
            }
        }));

    test('call expression with type parameters', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function identity<T>(value: T): T {
                     return value;
                 }
 
                 const result = identity<string>("Hello TypeScript");
             `)
        ));

    test('call expression with type parameters and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function identity<T>(value: T): T {
                     return value;
                 }
 
                 const result = /*a*/identity/*b*/</*c*/string/*d*/>/*e*/("Hello TypeScript");
             `)
        ));

    test('call expression with type parameters and optional chaining operator', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function identity<T>(value: T): T {
                     return value;
                 }
                 const result1 = identity<string>?.("Hello TypeScript");
                 const result2 = identity?.<string>("Hello TypeScript");
                 const result3 = identity?.call("Hello TypeScript");
             `)
        ));

    test('call expression with type parameters and optional chaining operator with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function identity<T>(value: T): T {
                     return value;
                 }
 
                 const result1 = /*a*/identity/*b*/<string>/*c*/?./*d*/("Hello TypeScript");
                 const result2 = /*a*/identity/*b*/?./*c*/<string>/*d*/("Hello TypeScript");
             `)
        ));

    test('call expression with mapping', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Operation = (a: number, b: number) => number;
 
                 // Define an object with methods accessed by string keys
                 const operations: { [key: string]: Operation } = {
                     add: (a, b) => a + b,
                     multiply: (a, b) => a * b,
                 };
 
                 // Access and call the "add" method using bracket notation
                 const result1 = operations["add"](3, 4); // 3 + 4 = 7
             `)
        ));

    test('call expression with mapping and ?.', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Operation = (a: number, b: number) => number;
 
                 // Define an object with methods accessed by string keys
                 const operations: { [key: string]: Operation } = {
                     add: (a, b) => a + b,
                     multiply: (a, b) => a * b,
                 };
 
                 // Access and call the "add" method using bracket notation
                 const result1 = operations["add"]?.(3, 4); // 3 + 4 = 7
             `)
        ));

    test('call expression with mapping adv', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const arr: { [key: string]: (x: number, y: number) => number }[] = [
                     {
                         abc: (x, y) => x - y,
                     },
                 ];
 
                 const result = arr[0]["abc"](10, 5); // Calls the function and subtracts 10 - 5 = 5
             `)
        ));

    // need a way to distinguish new class calls with empty braces and without braces
    test('call new expression without braces', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 var d = (new Date).getTime()
                 const intType = new arrow.Uint32
             `)
        ));

    // perhaps a bug in a Node parser
    // node.getChildren() skips token '/*a*/<'
    test.skip('call expression with sequential <<', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 expectTypeOf(o.get).toMatchTypeOf/*a*/<<K extends keyof EmberObject>(key: K) => EmberObject[K]>();
             `)
        ));
});
