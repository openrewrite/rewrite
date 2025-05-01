import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('property access mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('with array literal receiver', () => {
        rewriteRun(
          //language=typescript
          typeScript('[1] . length')
        );
    });

    test('with array literal receiver', () => {
        rewriteRun(
          //language=typescript
          typeScript('foo . bar . baz')
        );
    });

    test('optional property last', () => {
        rewriteRun(
          //language=typescript
          typeScript('options.onGroup?.(onGroupOptions)')
        );
    });

    test('optional property first', () => {
        rewriteRun(
            //language=typescript
            typeScript('options?.onGroup(onGroupOptions)')
        );
    });

    test('optional property all', () => {
        rewriteRun(
            //language=typescript
            typeScript('options?.onGroup?.(onGroupOptions)')
        );
    });

    test('no optional properties', () => {
        rewriteRun(
            //language=typescript
            typeScript('options.onGroup(onGroupOptions)')
        );
    });

});
