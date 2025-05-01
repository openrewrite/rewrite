import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('new mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript('new Uint8Array(1)')
        );
    });

    test('space', () => {
        rewriteRun(
          //language=typescript
          typeScript('new Uint8Array/*1*/(/*2*/1/*3*/)/*4*/')
        );
    });

    test('multiple', () => {
        rewriteRun(
          //language=typescript
          typeScript('new Date(2023, 9, 25, 10, 30, 15, 500)')
        );
    });

    test('trailing comma', () => {
        rewriteRun(
          //language=typescript
          typeScript('new Uint8Array(1 ,  )')
        );
    });
});
