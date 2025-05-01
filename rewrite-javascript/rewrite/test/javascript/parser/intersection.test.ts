import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('intersection type mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript('let c: number/*1*/ &/*2*/undefined/*3*/&/*4*/null')
        );
    });
    test('literals', () => {
        rewriteRun(
          //language=typescript
          typeScript('let c: & true & 1 & "foo"')
        );
    });
    test('union which starts with & ', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              export type GeolocateResponse =
                  & GeolocateResponseSuccess
                  & GeolocateResponseError;
          `)
        );
    });
});
