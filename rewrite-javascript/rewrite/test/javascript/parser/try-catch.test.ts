// noinspection TypeScriptUnresolvedReference

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

describe('try-catch mapping', () => {
    const spec = new RecipeSpec();

    test('try-catch empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                try {
                } catch (error) {
                }
            `)
        ));

    test('try-finally empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               try {
               } finally {
               }
           `)
        ));

    test('try-catch-finally empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               try {
               } catch (error) {
               } finally {
               }
           `)
        ));

    test('try-catch-finally empty comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               /*a*/ try /*b*/ {
               } /*c*/ catch /*d*/ ( /*e*/ error /*f*/) { /*g*/
               } /*h*/ finally /*i*/ { /*j*/
               } /*k*/
           `)
        ));

    test('try-catch without error', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 try {
                     // Code that may throw an error
                 } /*a*/ catch /*b*/ {
                     // handel error
                 }
             `)
        ));

    test('try-catch with typed unknown error', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               try {
                   // error
               } catch (error: unknown) {
                   // handel error
               }
           `)
        ));

    test('try-catch with typed any error', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               try {
                   // error
               } catch (error: any) {
                   // handel error
               }
           `)
        ));

    test('try-catch with typed error and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               try {
                   // error
               } catch (/*a*/ error /*b*/: /*c*/ unknown /*d*/) {
                   // handel error
               }
           `)
        ));

    test('try-catch-finally with body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               try {
                   // Try to parse JSON string
                   const data = JSON.parse('{ "name": "Alice" }');
                   console.log(data.name); // Output: "Alice"
               } catch (error: unknown) {
                   if (error instanceof Error) {
                       console.error("Caught an error:", error.message);
                   } else {
                       console.error("An unknown error occurred");
                   }
               } finally {
                   console.log("Parsing attempt finished.");
               }
           `)
        ));

    test('try-catch-finally with throw', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                   try {
                       throw new Error("Failed to perform database operation.");
                   } catch (error) {
                       console.error("Database Error:", (error as Error).message);
                   } finally {
                       console.log("Clean.");
                   }
           `)
        ));

    test('catch with ObjectBindingPattern as a name', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 class A {
                     public async connect() {
                         try {
                             await new Promise(null);
                         } catch ({error} : any ) {
                             throw error;
                         }
                     }
                 }
             `)
        ));

    test('catch with ObjectBindingPattern as a name with finally', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 class A {
                     public async connect() {
                         try {
                             await new Promise(null);
                         } catch ({error} : any ) {
                             throw error;
                         } finally {
                             console.log("Log");
                         }
                     }
                 }
             `)
        ));
});
