import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('array literal mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('access by index', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
                const numbers = [10, 20, 30, 40];
                const v = numbers[2];
          `)
        );
    });

    test('access by index with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const numbers = [10, 20, 30, 40];
                const v = /*a*/ numbers/*b*/[/*c*/2/*d*/]/*e*/;/*f*/
            `)
        );
    });

    test('access by index with !', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const numbers = [10, 20, 30, 40];
                const v = numbers[2]!;
            `)
        );
    });

    test('access by index with ! and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const numbers = [10, 20, 30, 40];
                const v = numbers[2]/*a*/!/*b*/;
            `)
        );
    });

    test('access by key', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const data = { a: 100, b: 200, c: 300 };
                const v = /*a*/data/*b*/[/*c*/'a'/*d*/]/*e*/;/*f*/
          `)
        );
    });

    test('access by key with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const data = { a: 100, b: 200, c: 300 };
                const v = data['a'];
            `)
        );
    });

    test('access by key with ?.', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const data = { a: 100, b: 200, c: 300 };
                const v = data['d'] ?. toString();
          `)
        );
    });


    test('with optional chaining operator', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const arr = [10, 20, 30];
                const value1 = arr/*a*/?./*b*/[1];
          `)
        );
    });

    test('with optional chaining operator and object access', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const obj = {
                    val: null
                };

                const result2 = obj.val?.[1];
            `)
        );
    });

});
