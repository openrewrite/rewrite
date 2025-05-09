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

describe('switch-case mapping', () => {
    const spec = new RecipeSpec();

    test('empty switch', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               let txt: string;
               switch /*a*/(/*b*/txt/*c*/)/*d*/ {
                   /*e*/
               }
           `)
        ));

    test('simple switch-case', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 let txt: string;
                 switch (txt) {
                     case 'a':
                         console.log('A');
                         break;
                 }
             `)
        ));

    test('simple switch-case with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               let  txt: string;
               switch (txt) {
                   /*a*/ case /*b*/'a'/*c*/:/*d*/
                       console.log('A');
                       /*e*/break /*f*/;/*g*/
               }
           `)
        ));

    test('switch-case with several different cases', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 let txt: string;
                 switch (txt) {
                     // first
                     case 'a':
                         console.log('A');
                         break;
                     //second
                     case 'b':
                         console.log('B');
                         break;
                 }
             `)
        ));

    test('switch-case with several cases with one body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 let txt: string;
                 switch (txt) {
                     // first
                     case 'a':
                     //second
                     case 'b':
                         console.log('B');
                         break;
                 }
             `)
        ));

    test('switch-case with cases and default', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 let txt: string;
                 switch (txt) {
                     // first
                     case 'a':
                         console.log('A');
                         break
                     //second
                     case 'b':
                         console.log('B')
                         break;
                     //default
                     default:
                         console.log('C, ...')
                         break
                 }
             `)
        ));

    test('switch-case with default and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                let txt: string;
                switch (txt) {
                    //default
                    /*a*/
                    default/*b*/
                    :/*c*/
                        console.log('C, ...');
                        break;
                }
            `)
        ));
});
