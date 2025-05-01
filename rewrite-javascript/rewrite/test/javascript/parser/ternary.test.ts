import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('ternary mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript('true ? 1 : 2')
        );
    });
});
