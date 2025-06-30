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

describe('indexed access type mapping', () => {
    const spec = new RecipeSpec();

    test('simple type access', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               type Person = { age: number; name: string; alive: boolean };
               type Age = Person["age"];
           `),
            //language=typescript
            typescript(`
               /*1*/type/*2*/ Person/*3*/ =/*4*/ {/*5*/ age/*6*/: /*7*/number/*8*/; /*9*/name/*10*/:/*11*/ string/*12*/; /*13*/alive/*14*/: /*15*/boolean /*16*/}/*17*/;/*18*/
               /*19*/type/*20*/ Age/*21*/ =/*22*/ Person/*23*/[/*24*/"age"/*25*/]/*26*/;/*27*/
           `),
        ));

    test('advanced indexed access type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Person = { age: number; name: string; alive: boolean };
                 type I1 = Person["age" | "name"];
                 type I2 = Person[keyof Person];
                 type AliveOrName = "alive" | "name";
                 type I3 = Person[AliveOrName];
             `)
        ));

    test('multy-dimension indexed access type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const MyArray = [
                    {name: "Alice", age: 15},
                    {name: "Bob", age: 23},
                    {name: "Eve", age: 38},
                ];

                type Person = typeof MyArray[number];
                type Age = typeof MyArray[number]["age"];
                // Or
                type Age2 = Person["age"];
            `),
            //language=typescript
            typescript(`
                const MyArray = [
                    {name: "Alice", age: 15},
                    {name: "Bob", age: 23},
                    {name: "Eve", age: 38},
                ];

                /*1*/
                type/*2*/ Person/*3*/ =/*4*/ typeof/*5*/ MyArray/*6*/[/*7*/number/*8*/]/*9*/;/*10*/
                type Age = typeof MyArray/*11*/[number]/*12*/[/*13*/"age"/*14*/]/*15*/;/*16*/
                // Or
                type Age2 = Person["age"];
            `),
        ));
});
