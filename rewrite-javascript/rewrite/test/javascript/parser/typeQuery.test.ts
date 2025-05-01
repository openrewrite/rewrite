import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('type-query operator mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('typeof', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              type UserType = typeof Number;
          `)
        );
    });

    test('typeof with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type UserType = /*a*/ typeof /*b*/ Number /*c*/;
            `)
        );
    });

    test('typeof as a type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const createUser: typeof Number = Number;
            `)
        );
    });

    test('typeof as a type with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const createUser: /*a*/ typeof /*b*/ Number /*c*/ = /*d*/ Number /*e*/;
            `)
        );
    });

    test('typeof as a type as function', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function greet(name: string) {
                }

                function hello() {
                }

                const sayHello: typeof greet = (name) => name | hello;
            `)
        );
    });

    test('typeof as a type as array', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const numbers = [];

                const moreNumbers: typeof numbers = [4, 5, 6];
            `)
        );
    });

    test('typeof as a type as a union', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const obj1 = { type: "type1", value: 42 };
                const obj2 = { type: "type2", description: "TypeScript is awesome" };

                const un: typeof obj1 | typeof obj2;
            `)
        );
    });

    test('index access type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type DatabaseConfig = typeof config["database"];
          `)
        );
    });

    test('index access type with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type DatabaseConfig = /*a*/typeof /*b*/ config/*c*/[/*d*/"database"/*e*/]/*f*/;
          `)
        );
    });


    test('index access type nested', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                interface Company {
                    employees: {
                        name: string;
                        age: number;
                    }[];
                }

                type EmployeeNameType = Company["employees"][number]["name"]; // Result: string
            `)
        );
    });

    test('typeof with generics', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type MyStructReturnType<X extends S.Schema.All> = S.Schema.Type<ReturnType<typeof MyStruct/*a*/</*c*/X/*d*/>/*b*/>>
            `)
        );
    });

});
