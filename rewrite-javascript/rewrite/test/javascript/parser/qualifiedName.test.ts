import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('qualified name mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('globalThis qualified name', () => {
        rewriteRun(
          //language=typescript
          typeScript('const value: globalThis.Number = 1')
        );
    });

    test('globalThis qualified name with generic', () => {
        rewriteRun(
          //language=typescript
          typeScript('const value: globalThis.Promise  <    string  > = null')
        );
    });

    test('globalThis qualified name with comments', () => {
        rewriteRun(
          //language=typescript
          typeScript('const value /*a123*/ : globalThis. globalThis . /*asda*/ globalThis.Promise<string> = null;')
        );
    });

    test.skip('nested class qualified name', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class OuterClass {
                public static InnerClass = class extends Number { };
              }
              const a: typeof OuterClass.InnerClass.prototype = 1;
          `)
        );
    });

    test('nested class qualified name', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                class OuterClass {
                    public static InnerClass = class extends Number { };
                }
                const a: typeof OuterClass.InnerClass.prototype = 1;
            `)
        );
    });

    test('namespace qualified name', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              namespace TestNamespace {
                export class Test {}
              };
              const value: TestNamespace.Test = null;
          `)
        );
    });

    test('enum qualified name', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              enum Test {
                  A, B
              };

              const val: Test.A = Test.A;
          `)
        );
    });
});
