import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('call mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('single', () => {
        rewriteRun(
          //language=typescript
          typeScript('parseInt("42")')
        );
    });

    test('multiple', () => {
        rewriteRun(
          //language=typescript
          typeScript('setTimeout(null, 2000, \'Hello\');')
        );
    });

    test('with array literal receiver', () => {
        rewriteRun(
          //language=typescript
          typeScript('[1] . splice(0)')
        );
    });

    test('with call receiver', () => {
        rewriteRun(
          //language=typescript
          typeScript('"1" . substring(0) . substring(0)')
        );
    });

    test('trailing comma', () => {
        rewriteRun(
          //language=typescript
          typeScript('parseInt("42" , )')
        );
    });

    test('with optional chaining operator', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const func = (message: string) => message;
                const result1 = func/*a*/?./*b*/("TS"); // Invokes the function
                const result2 = func/*a*/?./*b*/call("TS"); // Invokes the function
            `)
        );
    });

    test('call expression with type parameters', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function identity<T>(value: T): T {
                    return value;
                }

                const result = identity<string>("Hello TypeScript");
            `)
        );
    });

    test('call expression with type parameters and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function identity<T>(value: T): T {
                    return value;
                }

                const result = /*a*/identity/*b*/</*c*/string/*d*/>/*e*/("Hello TypeScript");
            `)
        );
    });

    test('call expression with type parameters and optional chaining operator', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function identity<T>(value: T): T {
                    return value;
                }

                const result1 = identity<string>?.("Hello TypeScript");
                const result2 = identity?.<string>("Hello TypeScript");
                const result3 = identity?.call("Hello TypeScript");
            `)
        );
    });

    test('call expression with type parameters and optional chaining operator with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function identity<T>(value: T): T {
                    return value;
                }

                const result1 = /*a*/identity/*b*/<string>/*c*/?./*d*/("Hello TypeScript");
                const result2 = /*a*/identity/*b*/?./*c*/<string>/*d*/("Hello TypeScript");
            `)
        );
    });

    test('call expression with mapping', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Operation = (a: number, b: number) => number;

                // Define an object with methods accessed by string keys
                const operations: { [key: string]: Operation } = {
                    add: (a, b) => a + b,
                    multiply: (a, b) => a * b,
                };

                // Access and call the "add" method using bracket notation
                const result1 = operations["add"](3, 4); // 3 + 4 = 7
            `)
        );
    });

    test('call expression with mapping and ?.', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Operation = (a: number, b: number) => number;

                // Define an object with methods accessed by string keys
                const operations: { [key: string]: Operation } = {
                    add: (a, b) => a + b,
                    multiply: (a, b) => a * b,
                };

                // Access and call the "add" method using bracket notation
                const result1 = operations["add"]?.(3, 4); // 3 + 4 = 7
            `)
        );
    });

    test('call expression with mapping adv', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const arr: { [key: string]: (x: number, y: number) => number }[] = [
                    {
                        abc: (x, y) => x - y,
                    },
                ];

                const result = arr[0]["abc"](10, 5); // Calls the function and subtracts 10 - 5 = 5
            `)
        );
    });

    // need a way to distinguish new class calls with empty braces and without braces
    test('call new expression without braces', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                var d = (new Date).getTime()
                const intType = new arrow.Uint32
            `)
        );
    });


    // perhaps a bug in a Node parser
    // node.getChildren() skips token '/*a*/<'
    test.skip('call expression with sequential <<', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                expectTypeOf(o.get).toMatchTypeOf/*a*/<<K extends keyof EmberObject>(key: K) => EmberObject[K]>();
            `)
        );
    });

});
