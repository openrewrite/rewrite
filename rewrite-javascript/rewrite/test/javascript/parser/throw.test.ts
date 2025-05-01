import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('throw mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple throw', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              throw new Error("Cannot divide by zero!");
          `)
        );
    });

    test('simple throw with comments', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              /*a*/ throw /*b*/ new /*c*/ Error/*d*/(/*e*/'Cannot divide by zero!'/*f*/)/*g*/;
          `)
        );
    });

    test('re-throwing', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              function riskyOperation() {
                  try {
                      throw new Error("An error occurred during risky operation.");
                  } catch (error) {
                      console.error("Logging Error:", (error as Error).message);
                      throw error;  // Re-throw the error to be handled at a higher level
                  }
              }
          `)
        );
    });
});
