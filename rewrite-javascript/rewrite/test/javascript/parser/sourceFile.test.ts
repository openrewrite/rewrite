import {connect, disconnect, rewriteRunWithOptions, typeScript} from '../testHarness';

describe('source file mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('whitespace after last statement', () => {
        rewriteRunWithOptions(
          {normalizeIndent: false},
          typeScript(
            //language=typescript
            `
                1; /* comment 1 */
                // comment 2
            `
          )
        );
    });
});
