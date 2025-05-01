import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('literal mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          typeScript('foo')
        );
    });

    test('private', () => {
        rewriteRun(
          typeScript('#foo')
        );
    });
});
