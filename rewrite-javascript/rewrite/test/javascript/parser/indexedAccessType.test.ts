import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('indexed access type mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple type access', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              type Person = { age: number; name: string; alive: boolean };
              type Age = Person["age"];
          `),
          //language=typescript
          typeScript(`
              /*1*/type/*2*/ Person/*3*/ =/*4*/ {/*5*/ age/*6*/: /*7*/number/*8*/; /*9*/name/*10*/:/*11*/ string/*12*/; /*13*/alive/*14*/: /*15*/boolean /*16*/}/*17*/;/*18*/
              /*19*/type/*20*/ Age/*21*/ =/*22*/ Person/*23*/[/*24*/"age"/*25*/]/*26*/;/*27*/
          `),
        );
    });

    test('advanced indexed access type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Person = { age: number; name: string; alive: boolean };
                type I1 = Person["age" | "name"];
                type I2 = Person[keyof Person];
                type AliveOrName = "alive" | "name";
                type I3 = Person[AliveOrName];
            `)
        );
    });

    test('multy-dimension indexed access type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const MyArray = [
                    { name: "Alice", age: 15 },
                    { name: "Bob", age: 23 },
                    { name: "Eve", age: 38 },
                ];

                type Person = typeof MyArray[number];
                type Age = typeof MyArray[number]["age"];
                // Or
                type Age2 = Person["age"];
            `),
            //language=typescript
            typeScript(`
                const MyArray = [
                    { name: "Alice", age: 15 },
                    { name: "Bob", age: 23 },
                    { name: "Eve", age: 38 },
                ];

                /*1*/type/*2*/ Person/*3*/ =/*4*/ typeof/*5*/ MyArray/*6*/[/*7*/number/*8*/]/*9*/;/*10*/
                type Age = typeof MyArray/*11*/[number]/*12*/[/*13*/"age"/*14*/]/*15*/;/*16*/
                // Or
                type Age2 = Person["age"];
            `),
        );
    });

});
