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

describe('for mapping', () => {
    const spec = new RecipeSpec();

    test('empty for', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('for (;;);')
        ));

    test('empty for with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('/*a*/for /*b*/ (/*c*/;/*d*/;/*e*/)/*f*/;/*g*/')
        ));

    test('for indexed', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('for (let i = 0; i < 10; i++) ;')
        ));

    test('for with assigment condition', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('for(var r,a=0;r=t[a++];);')
        ));

    test('for indexed multiple variables', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('for (let /*0*/ i /*1*/ = /*2*/ 0 /*3*/ , j = 0/*4*/; /*5*/ i /*6*/ < 10 /*7*/; /*8*/i++/*9*/)/*10*/ ;/*11*/')
        ));

    test('for indexed with empty body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('for (let i = 0; i < 10; i++) /*a*/ {/*b*/} /*c*/;')
        ));

    test('for indexed with body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('for (let i = 0; i < 10; i++) { console.log(i); };')
        ));

    test('for with continue', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 for (let i = 0; i < 5; i++) {
                     if (i === 2) {
                         continue /*a*/; // Skip the current iteration when i equals 2
                     }
                     console.log(i);
                 }
             `)
        ));

    test('for with break', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 for (let i = 0; i < 5; i++) {
                     if (i === 2) {
                         break /*a*/; // Exit the loop when i equals 5
                     }
                     console.log(i);
                 }
             `)
        ));

    test('for with labeled break', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 /*a*/labelName/*b*/:/*c*/ for (let i = 0; i < 5; i++) {
                     for (let j = 0; j < 5; j++) {
                         if (i === 2 && j === 2) {
                             /*d*/break /*e*/ labelName/*f*/; // Exits the outer loop when i and j are both 2
                         }
                     }
                 }
             `)
        ));

    test('for-of empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('for (let char of "text") ;')
        ));

    test('for-of empty with array', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('for (let i of [0, 1, 2, [3, 4, 5]]) ;')
        ));

    test('for-of with object binding pattern', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const list : any = [];
                 for (let {a, b} of list) {
                     console.log(a); // 4, 5, 6
                 }
             `)
        ));

    test('for-of with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('/*a*/for/*b*/ (/*c*/const /*d*/char /*e*/of /*f*/ "text"/*g*/)/*h*/ {/*j*/} /*k*/;/*l*/')
        ));

    test('for-of with await and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('export {};/*a*/for/*b*/ await/*bb*/(/*c*/const /*d*/char /*e*/of /*f*/ "text"/*g*/)/*h*/ {/*j*/} /*k*/;/*l*/')
        ));

    test('for-in empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('for (const index in []) ;')
        ));

    test('for-in with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('/*a*/for/*b*/ (/*c*/const /*d*/index /*e*/in /*f*/ []/*g*/)/*h*/ {/*j*/} /*k*/;/*l*/')
        ));

    test('for-in with keyof typeof TypeOperator', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const person = {
                     name: "Alice",
                     age: 25,
                     city: "New York",
                 };
 
                 for (const key in person) {
                     console.log(person[key as keyof typeof person]);
                 }
             `)
        ));

    test('for-in with dynamic object', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const dynamicObject: { [key: string]: any } = {
                     prop1: "Value1",
                     prop2: 42,
                     prop3: true,
                 };
 
                 for (const key in dynamicObject) {
                     console.log(dynamicObject[key]);
                 }
             `)
        ));

    test('for with expression instead of statement', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function foo () {
                     let b;
                     for (b in a) return !1;
                     for (b of a) return !1;
                     let i, str;
                     for (i = 0; i < 9; i++) {
                         str = str + i;
                     }
                 }
             `)
        ));

    test('for-of with await', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const asyncIterable = {
                     [Symbol.asyncIterator]() {
                         let count = 0;
                         return {
                             async next() {
                                 if (count < 3) {
                                     count++;
                                     return {value: count, done: false};
                                 }
                                 return {value: undefined, done: true};
                             },
                         };
                     },
                 };
 
                 async function iterateAsyncIterable() {
                     for await (const value of asyncIterable) {
                         console.log(value);
                     }
                 }
             `)
        ));

    test('for with bool condition', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 let bit = true
                 for (let i = 0;/*a*/ bit/*b*/; ++i) {
                    bit = false;
                 }
             `)
        ));
});
