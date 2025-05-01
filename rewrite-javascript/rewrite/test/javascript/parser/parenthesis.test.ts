import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('parenthesis mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          typeScript('(1)')
        );
    });

    test('space', () => {
        rewriteRun(
          typeScript('( 1 )')
        );
    });
});
