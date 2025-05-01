import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('delete operator mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('delete', () => {
        rewriteRun(
          //language=typescript
          typeScript('delete 1')
        );
    });
});
