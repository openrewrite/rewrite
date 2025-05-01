import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('namespace mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('namespace empty', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              namespace X {
              }
          `)
        );
    });

    test('namespace with statement body', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              namespace X {
                  const x = 10
              }
          `)
        );
    });

    test('namespace with several statements in body', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              namespace X {
                  const x = 10;
                  const y = 5
                  const z = 0;
              }
          `)
        );
    });

    test('namespace with several statements in body and comments', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              namespace X {
                  /*a*/ const x = 10/*b*/;/*c*/
                  /*d*/ const y = 5 /*e*/
                  const z = 0;/*f*/
              }
          `)
        );
    });

    test('namespace with statement body and comments', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              /*a*/ namespace /*b*/ X /*c*/{ /*d*/
                  /*e*/const x = 10 /*f*/
              } /*g*/
          `)
        );
    });

    test('namespace with statement body and modifier', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              declare namespace X {
                  const x = 10
              }
          `)
        );
    });

    test('namespace with statement body, modifier and comments', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              /*a*/ declare /*b*/ namespace /*c*/ X /*d*/{
                  const x = 10
              }
          `)
        );
    });

    test('namespace empty with sub-namespaces', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              namespace X.Y.Z {
              }
          `)
        );
    });

    test('namespace empty with sub-namespaces and comments', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              namespace /*a*/X/*b*/./*c*/Y/*d*/./*e*/Z/*f*/ {
              }
          `)
        );
    });

    test('namespace non-empty with sub-namespaces and body', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              namespace X.Y.Z {
                const x = 10
              }
          `)
        );
    });

    test('namespace non-empty with sub-namespaces and body and modifier', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              declare /*a*/ namespace X.Y.Z {
                  const x = 10
              }
          `)
        );
    });

    test('hierarchic namespaces with body with comments', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              /*0*/ namespace X {
              /*a*/    namespace Y {
              /*b*/        namespace Z {
              /*c*/            interface Person {
                                    name: string;
                          }
                      }
                  }
              }
          `)
        );
    });

    test('hierarchic namespaces with body', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              namespace X {
                  namespace Y {
                      namespace Z {
                          interface Person {
                              name: string;
                          }
                      }
                  }
              }
          `)
        );
    });

    test('namespace with expression', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              /*pref*/ declare namespace /*middle*/ TestNamespace /*after*/ {
                  /*bcd*/
                  /*1*/ const a = 10;
                  /*efg*/
                  /*2*/ function abc() {
                      return null
                  }
                  /*fgh*/
                  /*3*/ class X {
                            b: number;
                            c: string;
                 }
                  /*ghj*/
              }
          `)
        );
    });


    test('complex test with namaspaces and modules', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              export var asdf = 123;

              module porting {
                  var foo = {};
                  foo.bar = 123; // error : property 'bar' does not exist on \`{}\`
                  foo.bas = 'hello'; // error : property 'bas' does not exist on \`{}\`
              }

              module assert {
                  interface Foo {
                      bar: number;
                      bas: string;
                  }

                  var foo = {} as Foo;
                  foo.bar = 123;
                  foo.bas = 'hello';
              }

              module sdfsdfsdf {
                  var foo: any;
                  var bar = <string>foo; // bar is now of type "string"
              }


              namespace doubleAssertion {

                  function handler1(event: Event) {
                      let mouseEvent = event as MouseEvent;
                  }

                  function handler2(event: Event) {
                      let element = event as HTMLElement; // Error : Neither 'Event' not type 'HTMLElement' is assignable to the other
                  }

                  function handler(event: Event) {
                      let element = event as any as HTMLElement; // Okay!
                  }
              }

          `)
        );
    });

    test('empty body', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                declare module 'vue-count-to' /*a*/;/*b*/
          `)
        );
    });

    test('namespace export as', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export {}
                declare namespace MyLibrary {
                    function sayHello(name: string): void;
                }

                /*a*/ export /*b*/ as /*c*/ namespace /*d*/ MyLibrary/*e*/;
          `)
        );
    });

    test('complex namespace export as', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export {Fraction}

                export as namespace math /*a*/

                /*b*/ type NoLiteralType<T> = T extends number
                    ? number
                    : T extends string
                        ? string
                        : T extends boolean
                            ? boolean
                            : T
          `)
        );
    });


    test('extend global type definitions without namespace keyword', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                declare global {
                    interface Window {
                        myCustomGlobalFunction?: () => void; // Add a custom global function
                    }
                }
          `)
        );
    });

});
