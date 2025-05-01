import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('typeof operator mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('typeof', () => {
        rewriteRun(
          //language=typescript
          typeScript('typeof 1')
        );
    });
});
