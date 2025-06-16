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

describe('type-query operator mapping', () => {
    const spec = new RecipeSpec();

    test('typeof', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               type UserType = typeof Number;
           `)
        ));

    test('typeof with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type UserType = /*a*/ typeof /*b*/ Number /*c*/;
             `)
        ));

    test('typeof as a type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const createUser: typeof Number = Number;
             `)
        ));

    test('typeof as a type with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const createUser: /*a*/ typeof /*b*/ Number /*c*/ = /*d*/ Number /*e*/;
             `)
        ));

    test('typeof as a type as function', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function greet(name: string) {
                 }
 
                 function hello() {
                 }
 
                 const sayHello: typeof greet = (name) => name | hello;
             `)
        ));

    test('typeof as a type as array', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const numbers = [];
 
                 const moreNumbers: typeof numbers = [4, 5, 6];
             `)
        ));

    test('typeof as a type as a union', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const obj1 = { type: "type1", value: 42 };
                 const obj2 = { type: "type2", description: "TypeScript is awesome" };
 
                 const un: typeof obj1 | typeof obj2;
             `)
        ));

    test('index access type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type DatabaseConfig = typeof config["database"];
           `)
        ));

    test('index access type with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type DatabaseConfig = /*a*/typeof /*b*/ config/*c*/[/*d*/"database"/*e*/]/*f*/;
           `)
        ));

    test('index access type nested', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 interface Company {
                     employees: {
                         name: string;
                         age: number;
                     }[];
                 }
 
                 type EmployeeNameType = Company["employees"][number]["name"]; // Result: string
             `)
        ));

    test('typeof with generics', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                type MyStructReturnType<X extends S.Schema.All> = S.Schema.Type<ReturnType<typeof MyStruct/*a*/</*c*/X/*d*/>/*b*/>>
            `)
        ));
});
