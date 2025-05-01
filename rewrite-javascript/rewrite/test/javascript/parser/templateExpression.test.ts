import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('template expression mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());


    test('simple template', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const v = \`\${42}\`;
            `)
        );
    });

    test('simple template with literal', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const v = \`\${42}\`;
            `)
        );
    });

    test('simple template with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const v = /*a*/\`\/*b*/${/*c*/42/*d*/}/*e*/\`;
            `)
        );
    });

    test('simple template with text', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const a = 5;
                const sum = \`The value of \${a}.\`;
            `)
        );
    });

    test('simple template with expression', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
            const a = 5;
            const b = 10;
            const sum = \`The sum of \${/*a*/a /*b*/} and \${/*c*/b/*d*/} is \${a /*e*/ + /*f*/b /*g*/}.  \`;
          `)
        );
    });

    test('simple template with ternary', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const isAdmin = true;
                const roleMessage = \`User is \${ isAdmin ? "an Admin" : "a Guest" }.\`;
          `)
        );
    });

    test('simple template with function', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function greet(name: string): string {
                    return \`Hello, \${name}\`;
                }

                const username = "Alice";
                const message = \`The greeting is: \${greet(username)}\`;
            `)
        );
    });

    test('template tag', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function tag(strings: TemplateStringsArray, ...values: any[]) {
                     return strings.reduce((result, str, i) => \`\${result}\${str}\${values[i] || ""}\`, "");
                }

                const name = "Alice";
                const age = 25;
                const result = tag\`My name is \${name} and I am \${age} years old.\`;
            `)
        );
    });

    test('template tag with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const result = /*a*/tag/*b*/\` My name is \${name} and I am \${age} years old.\`;
            `)
        );
    });

    test('template tag with type arguments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function genericTag<T>(
                    strings: TemplateStringsArray,
                    ...values: T[]
                ): string {
                    return strings.reduce((result, str, i) => \`\${result}\${str}\${values[i] || ""}\`, "");
                }

                // Using generic types
                const result = genericTag<number>\`The sum is \${42}\`;
          `)
        );
    });

    test('template tag with type arguments and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const result = /*a*/genericTag/*b*/</*c*/number/*d*/>/*e*/\`The sum is \${42}\`;
            `)
        );
    });

    test('template LiteralType ', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Name = "Alice";
                type Greeting = \`Hello, \${Name}!\`;
            `)
        );
    });

    test('template yield LiteralType ', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                Effect.gen(function* () {
                    const sql = yield* SqlClient.SqlClient

                    const rows = yield* sql<{ table_name: string }>\`abc \`

                    expect(rows).toEqual([{table_name: "test_creation"}])
                }).pipe(runTest({table: "test_creation"}))
            `)
        );
    });

    test('template LiteralType union', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Action = "create" | "update";
                type Entity = "user" | "post";
                type APIEndpoint = \`\${Action}_\${Entity}\`;

                const endpoint: APIEndpoint = "create_user";
            `)
        );
    });

    test('template LiteralType with conditional', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
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
        );
    });
});
