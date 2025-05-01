import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('empty mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript('if (true) {/*a*/};')
        );
    });
});
