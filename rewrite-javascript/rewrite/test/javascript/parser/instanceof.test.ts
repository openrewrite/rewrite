import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('instanceof mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript('1 instanceof Object')
        );
    });
});
