import {connect, disconnect, rewriteRunWithOptions, typeScript} from '../testHarness';

describe('this mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRunWithOptions(
          {normalizeIndent: false},
          typeScript('this')
        );
    });
});
