import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('prefix operator mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('plus', () => {
        rewriteRun(
          //language=typescript
          typeScript('+1')
        );
    });
    test('minus', () => {
        rewriteRun(
          //language=typescript
          typeScript('-1')
        );
    });
    test('not', () => {
        rewriteRun(
          //language=typescript
          typeScript('!1')
        );
    });
    test('tilde', () => {
        rewriteRun(
          //language=typescript
          typeScript('~1')
        );
    });
    test('increment', () => {
        rewriteRun(
          //language=typescript
          typeScript('++1')
        );
    });
    test('decrement', () => {
        rewriteRun(
          //language=typescript
          typeScript('--a;')
        );
    });
    test('spread', () => {
        rewriteRun(
          //language=typescript
          typeScript('[ ...[] ]')
        );
    });
    test('spread in method param', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                class Foo {
                    constructor(@multiInject(BAR) /*a*/...args: Bar[][]) {}
                }
            `)
        );
    });
});

describe('postfix operator mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('increment', () => {
        rewriteRun(
          //language=typescript
          typeScript('a++;')
        );
    });
    test('decrement', () => {
        rewriteRun(
          //language=typescript
          typeScript('a--;')
        );
    });

    test('unary with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript('/*a*/a/*b*/++/*c*/;')
        );
    });
});
