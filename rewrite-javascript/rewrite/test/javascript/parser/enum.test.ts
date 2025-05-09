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

describe('enum mapping', () => {
    const spec = new RecipeSpec();

    test('enum declaration', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               enum Test {
               };
           `)
        ));

    test('enum empty declaration with modifiers', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               declare const enum Test {
               };
           `)
        ));

    test('enum member', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               enum Test {
                 A
               };
           `)
        ));

    test('enum member with coma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               enum Test {
                   A/*a*/,
               };
           `)
        ));

    test('enum members', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               enum Test {
                   A,
                   B,
                   C
               };
           `)
        ));

    test('enum with const modifier', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               const enum Test {
                   A,
                   B,
                   C,
               };
           `)
        ));

    test('enum with declare modifier', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               declare enum Test {
                   A,
                   B,
                   C,
               };
           `)
        ));

    test('enum with declare const modifier', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               declare const enum Test {
                   A,
                   B,
                   C,
               };
           `)
        ));

    test('enum with declare const modifier and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 /*a*/ declare /*b*/ const /*c*/ enum Test {
                     A,
                     B,
                     C,
                 };
             `)
        ));

    test('enum members with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                enum Test /*xx*/ {
                   A /*aa*/, /*ab*/
                   /*bb*/ B /*cc*/,
                   C/*de*/, /*dd*/
               };
           `)
        ));

    test('enum members with initializer', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               enum Test {
                   A  = "AA",
                   B = 10
               }
           `)
        ));

    test('enum mixed members with initializer', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               enum Test {
                   A  = "AA",
                   B = undefined,
                   C = 10,
                   D = globalThis.NaN,
                   E = (2 + 2),
                   F,
               }
           `)
        ));

    test('enum members with initializer and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               enum Test {
                   //A /*aaa*/ = /*bbb*/ "A"
                   A  /*aaa*/  = /*bbb*/ "AA"  ,
                   B = 10 /*ccc*/ + /*ddd*/ 5
               }
           `)
        ));

    test('enum complex members with initializer', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               const baseValue = 10;
 
               const enum MathConstants {
                   Pi = 3.14,
                   E = Math.E,
                   GoldenRatio = baseValue + 1.618,
               }
           `)
        ));

    test('enum with string literals', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               enum CustomizableCompilers {
                   /*a*/'typescript'/*b*/ = 'typescript'
               }
           `)
        ));

    test.skip('enum with non identifier name', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                enum A { ['baz'] }
            `)
        ));
});
