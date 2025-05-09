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

describe('template expression mapping', () => {
    const spec = new RecipeSpec();

    test('simple template', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const v = \`\${42}\`;
            `)
        ));

    test('simple template with literal', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const v = \`\${42}\`;
             `)
        ));

    test('simple template with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const v = /*a*/\`\/*b*/${/*c*/42/*d*/}/*e*/\`;
             `)
        ));

    test('simple template with text', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const a = 5;
                 const sum = \`The value of \${a}.\`;
             `)
        ));

    test('simple template with expression', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
             const a = 5;
             const b = 10;
             const sum = \`The sum of \${/*a*/a /*b*/} and \${/*c*/b/*d*/} is \${a /*e*/ + /*f*/b /*g*/}.  \`;
           `)
        ));

    test('simple template with ternary', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const isAdmin = true;
                 const roleMessage = \`User is \${ isAdmin ? "an Admin" : "a Guest" }.\`;
           `)
        ));

    test('simple template with function', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function greet(name: string): string {
                     return \`Hello, \${name}\`;
                 }
 
                 const username = "Alice";
                 const message = \`The greeting is: \${greet(username)}\`;
             `)
        ));

    test('template tag', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function tag(strings: TemplateStringsArray, ...values: any[]) {
                      return strings.reduce((result, str, i) => \`\${result}\${str}\${values[i] || ""}\`, "");
                 }
 
                 const name = "Alice";
                 const age = 25;
                 const result = tag\`My name is \${name} and I am \${age} years old.\`;
             `)
        ));

    test('template tag with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const result = /*a*/tag/*b*/\` My name is \${name} and I am \${age} years old.\`;
             `)
        ));

    test('template tag with type arguments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function genericTag<T>(
                     strings: TemplateStringsArray,
                     ...values: T[]
                 ): string {
                     return strings.reduce((result, str, i) => \`\${result}\${str}\${values[i] || ""}\`, "");
                 }
 
                 // Using generic types
                 const result = genericTag<number>\`The sum is \${42}\`;
           `)
        ));

    test('template tag with type arguments and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const result = /*a*/genericTag/*b*/</*c*/number/*d*/>/*e*/\`The sum is \${42}\`;
             `)
        ));

    test('template LiteralType ', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Name = "Alice";
                 type Greeting = \`Hello, \${Name}!\`;
             `)
        ));

    test('template yield LiteralType ', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 Effect.gen(function* () {
                     const sql = yield* SqlClient.SqlClient
 
                     const rows = yield* sql<{ table_name: string }>\`abc \`
 
                     expect(rows).toEqual([{table_name: "test_creation"}])
                 }).pipe(runTest({table: "test_creation"}))
             `)
        ));

    test('template LiteralType union', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Action = "create" | "update";
                 type Entity = "user" | "post";
                 type APIEndpoint = \`\${Action}_\${Entity}\`;
 
                 const endpoint: APIEndpoint = "create_user";
             `)
        ));

    test('template LiteralType with conditional', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Role = "admin" | "editor" | "viewer";
                 type Permissions<RoleType extends Role> = RoleType extends "admin"
                     ? \`can_manage_\${string}\`
                     : \`can_read_\${string}\`;
 
                 // Valid permissions:
                 const adminPermission: Permissions<"admin"> = "can_manage_users";
                 const viewerPermission: Permissions<"viewer"> = "can_read_posts";
 
                 // Invalid permission:
                 const invalidPermission: Permissions<"editor"> = "can_delete_posts"; // Error
             `)
        ));
});
