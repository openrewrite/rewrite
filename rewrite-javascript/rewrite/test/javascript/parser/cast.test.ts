import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('cast mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('primitive type', () => {
        rewriteRun(
          //language=typescript
          typeScript('< string  > "x"')
        );
    });
});
