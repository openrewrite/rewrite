import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('try-catch mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('try-catch empty', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              try {
              } catch (error) {
              }
          `)
        );
    });

    test('try-finally empty', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              try {
              } finally {
              }
          `)
        );
    });

    test('try-catch-finally empty', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              try {
              } catch (error) {
              } finally {
              }
          `)
        );
    });

    test('try-catch-finally empty comments', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              /*a*/ try /*b*/ {
              } /*c*/ catch /*d*/ ( /*e*/ error /*f*/) { /*g*/
              } /*h*/ finally /*i*/ { /*j*/
              } /*k*/
          `)
        );
    });

    test('try-catch without error', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                try {
                    // Code that may throw an error
                } /*a*/ catch /*b*/ {
                    // handel error
                }
            `)
        );
    });

    test('try-catch with typed unknown error', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              try {
                  // error
              } catch (error: unknown) {
                  // handel error
              }
          `)
        );
    });

    test('try-catch with typed any error', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              try {
                  // error
              } catch (error: any) {
                  // handel error
              }
          `)
        );
    });

    test('try-catch with typed error and comments', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              try {
                  // error
              } catch (/*a*/ error /*b*/: /*c*/ unknown /*d*/) {
                  // handel error
              }
          `)
        );
    });

    test('try-catch-finally with body', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              try {
                  // Try to parse JSON string
                  const data = JSON.parse('{ "name": "Alice" }');
                  console.log(data.name); // Output: "Alice"
              } catch (error: unknown) {
                  if (error instanceof Error) {
                      console.error("Caught an error:", error.message);
                  } else {
                      console.error("An unknown error occurred");
                  }
              } finally {
                  console.log("Parsing attempt finished.");
              }
          `)
        );
    });

    test('try-catch-finally with throw', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
                  try {
                      throw new Error("Failed to perform database operation.");
                  } catch (error) {
                      console.error("Database Error:", (error as Error).message);
                  } finally {
                      console.log("Clean.");
                  }
          `)
        );
    });

    test('catch with ObjectBindingPattern as a name', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                class A {
                    public async connect() {
                        try {
                            await new Promise(null);
                        } catch ({error} : any ) {
                            throw error;
                        }
                    }
                }
            `)
        );
    });

    test('catch with ObjectBindingPattern as a name with finally', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                class A {
                    public async connect() {
                        try {
                            await new Promise(null);
                        } catch ({error} : any ) {
                            throw error;
                        } finally {
                            console.log("Log");
                        }
                    }
                }
            `)
        );
    });

});
