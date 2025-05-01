import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('type literal mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('indexed type literal', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
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
            typeScript(`
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
        );
    });

    test('type literal', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Animal = { kind: "dog"; bark(): void };
            `)
        );
    });

    test('type literal in params', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type ascii = {
                    " ": 32; "!": 33;
                }
            `)
        );
    });

    test('with index signature and mapped type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
              export const struct: <R extends {/*a*/ readonly /*b*/[x: string]: Semigroup<any> }>(
                  fields: R
              ) => Semigroup<{ readonly [K in keyof R]: [R[K]] extends [Semigroup<infer A>] ? A : never }> = product_.struct(Product)
          `)
        );
    });
});
