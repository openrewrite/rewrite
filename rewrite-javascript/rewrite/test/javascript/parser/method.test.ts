import {connect, disconnect, rewriteRun, rewriteRunWithOptions, typeScript} from '../testHarness';

describe('method mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler {
                  test() {
                      // hello world comment
                  }
              }
          `)
        );
    });


    test('single parameter', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler {
                  test(input) {
                      // hello world comment
                  }
              }
          `)
        );
    });

    test('single typed parameter', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler {
                  test(input: string) {
                      // hello world comment
                  }
              }
          `)
        );
    });

    test('single typed parameter with initializer', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler {
                  test(input  /*asda*/:  string    =    /*8asdas */ "hello world"   ) {
                      // hello world comment
                  }
              }
          `)
        );
    });

    test('single parameter with initializer', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler {
                  test(input =    1) {
                      // hello world comment
                  }
              }
          `)
        );
    });

    test('multi parameters', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler {
                  test(input: string, a = 1, test: number) {
                      // hello world comment
                  }
              }
          `)
        );
    });

    test('parameter with trailing comma', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler {
                  test(input: string    , ) {
                      // hello world comment
                  }
              }
          `)
        );
    });

    test('optional parameter with trailing comma', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                class Handler {
                    test(      input   ?    :  string    , ) {
                        // hello world comment
                    }
                }
            `)
        );
    });

    test('type parameters', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler {
                  test<T>(input: T    , ) {
                      // hello world comment
                  }
              }
          `)
        );
    });

    test('type parameters with bounds', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler {
                  test<T extends string>(input: T    , ) {
                      // hello world comment
                  }
              }
          `)
        );
    });

    test('return type', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler {
                  test(input: string    , ) /*1*/ : /*asda*/ string {
                      // hello world comment
                      return input;
                  }
              }
          `)
        );
    });

    test('method with generics', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class Handler< T1  , T2> {
                  test   <  T3 >    ( input: string    , t3: T3 ) /*1*/ : /*asda*/ string {
                      // hello world comment
                      return input;
                  }
              }
          `)
        );
    });

    test('method with ComputedPropertyName', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const asyncIterable = {
                    [ Symbol .  asyncIterator ] () {
                        return {
                            async next() {
                                return {value: undefined, done: true};
                            },
                        };
                    },
                };
          `)
        );
    });

    test('method signature with ComputedPropertyName', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const greetSymbol = Symbol("greet");

                interface Greeter {
                    /*a*/[/*b*/greetSymbol/*c*/]/*d*/(message: string): void; // Computed method name
                }

                const greeter: Greeter = {
                    [greetSymbol](message: string): void {
                        console.log(message);
                    },
                };
          `)
        );
    });

    test('extends as a call expression', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export class ResultLengthMismatch extends TypeIdError(SqlErrorTypeId, "ResultLengthMismatch")<{
                }> {
                }
          `)
        );
    });

    test('method name as string literal', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export interface IEnumerable<T> extends Iterable<T> {
                    /*a*/"System.Collections.IEnumerable.GetEnumerator"/*b*/(/*c*/): IEnumerator<any>;
                }
          `)
        );
    });

});
