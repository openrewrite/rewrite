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
 */import {RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";

describe('type literal mapping', () => {
    const spec = new RecipeSpec();

    test('indexed type literal', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 /*1*/
                 type/*2*/ OnlyBoolsAndHorses /*3*/ =/*4*/ { /*5*/
                     /*6*/
                     /*7*/[/*8*/key/*9*/:/*10*/ string/*11*/]/*12*/:/*13*/ boolean/*14*/ |/*15*/ Horse/*16*/; /*17*/
                     /*18*/
                 }/*19*/; /*20*/
 
                 const conforms: OnlyBoolsAndHorses = {
                     del: true,
                     rodney: false,
                 };
             `),
            //language=typescript
            typescript(`
               type test = {
                   (start: number): string;   // Call signature
                   interval: number;          // Property
                   reset(): void;             // Method
                   [index: number]: string    // Indexable
                   add(): (x: number, y: number) => number; //Function signature
                   add: <T> (x: number, y: number) => number; //Function type
                   add1: (x: number, y: number) => number; //Function type
                   ctroType: new < T > (x: number, y: number) => number; //Ctor type
                   ctroType1: new  (x: number, y: number) => number; //Ctor type
               }
           `)
        ));

    test('type literal', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Animal = { kind: "dog"; bark(): void };
             `)
        ));

    test('type literal in params', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type ascii = {
                     " ": 32; "!": 33;
                 }
             `)
        ));

    test('with index signature and mapped type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export const struct: <R extends {/*a*/ readonly /*b*/[x: string]: Semigroup<any> }>(
                    fields: R
                ) => Semigroup<{ readonly [K in keyof R]: [R[K]] extends [Semigroup<infer A>] ? A : never }> = product_.struct(Product)
            `)
        ));
});
