import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('return mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript('function f() {return 1;}')
        );
    });
    test('empty', () => {
        rewriteRun(
          //language=typescript
          typeScript('function f() {return}')
        );
    });
});
