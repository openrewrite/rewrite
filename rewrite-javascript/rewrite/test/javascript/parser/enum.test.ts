import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('enum mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('enum declaration', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              enum Test {
              };
          `)
        );
    });

    test('enum empty declaration with modifiers', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              declare const enum Test {
              };
          `)
        );
    });

    test('enum member', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              enum Test {
                A
              };
          `)
        );
    });

    test('enum member with coma', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              enum Test {
                  A/*a*/,
              };
          `)
        );
    });

    test('enum members', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              enum Test {
                  A,
                  B,
                  C
              };
          `)
        );
    });

    test('enum with const modifier', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              const enum Test {
                  A,
                  B,
                  C,
              };
          `)
        );
    });

    test('enum with declare modifier', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              declare enum Test {
                  A,
                  B,
                  C,
              };
          `)
        );
    });

    test('enum with declare const modifier', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              declare const enum Test {
                  A,
                  B,
                  C,
              };
          `)
        );
    });

    test('enum with declare const modifier and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                /*a*/ declare /*b*/ const /*c*/ enum Test {
                    A,
                    B,
                    C,
                };
            `)
        );
    });

    test('enum members with comments', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
               enum Test /*xx*/ {
                  A /*aa*/, /*ab*/
                  /*bb*/ B /*cc*/,
                  C/*de*/, /*dd*/
              };
          `)
        );
    });

    test('enum members with initializer', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              enum Test {
                  A  = "AA",
                  B = 10
              }
          `)
        );
    });

    test('enum mixed members with initializer', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              enum Test {
                  A  = "AA",
                  B = undefined,
                  C = 10,
                  D = globalThis.NaN,
                  E = (2 + 2),
                  F,
              }
          `)
        );
    });

    test('enum members with initializer and comments', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              enum Test {
                  //A /*aaa*/ = /*bbb*/ "A"
                  A  /*aaa*/  = /*bbb*/ "AA"  ,
                  B = 10 /*ccc*/ + /*ddd*/ 5
              }
          `)
        );
    });

    test('enum complex members with initializer', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              const baseValue = 10;

              const enum MathConstants {
                  Pi = 3.14,
                  E = Math.E,
                  GoldenRatio = baseValue + 1.618,
              }
          `)
        );
    });

    test('enum with string literals', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              enum CustomizableCompilers {
                  /*a*/'typescript'/*b*/ = 'typescript'
              }
          `)
        );
    });

    test.skip('enum with non identifier name', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              enum A { ['baz'] }
          `)
        );
    });
});
