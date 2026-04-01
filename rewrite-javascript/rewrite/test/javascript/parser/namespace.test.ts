/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";

describe('namespace mapping', () => {
    const spec = new RecipeSpec();

    test('namespace empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                namespace X {
                }
            `)
        ));

    test('namespace with statement body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                namespace X {
                    const x = 10
                }
            `)
        ));

    test('namespace with several statements in body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                namespace X {
                    const x = 10;
                    const y = 5
                    const z = 0;
                }
            `)
        ));

    test('namespace with several statements in body and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                namespace X {
                    /*a*/
                    const x = 10/*b*/;/*c*/
                    /*d*/
                    const y = 5 /*e*/
                    const z = 0;/*f*/
                }
            `)
        ));

    test('namespace with statement body and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                /*a*/
                namespace /*b*/
                X /*c*/
                { /*d*/
                    /*e*/
                    const x = 10 /*f*/
                } /*g*/
            `)
        ));

    test('namespace with statement body and modifier', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                declare namespace X {
                    const x = 10
                }
            `)
        ));

    test('namespace with statement body, modifier and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                /*a*/
                declare /*b*/
                namespace /*c*/
                X /*d*/
                {
                    const x = 10
                }
            `)
        ));

    test('namespace empty with sub-namespaces', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                namespace X.Y.Z {
                }
            `)
        ));

    test('namespace empty with sub-namespaces and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                namespace /*a*/
                X/*b*/./*c*/Y/*d*/
                ./*e*/
                Z/*f*/
                {
                }
            `)
        ));

    test('namespace non-empty with sub-namespaces and body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                namespace X.Y.Z {
                    const x = 10
                }
            `)
        ));

    test('namespace non-empty with sub-namespaces and body and modifier', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                declare /*a*/ namespace X.Y.Z {
                    const x = 10
                }
            `)
        ));

    test('hierarchic namespaces with body with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                /*0*/
                namespace X {
                    /*a*/
                    namespace Y {
                        /*b*/
                        namespace Z {
                            /*c*/
                            interface Person {
                                name: string;
                            }
                        }
                    }
                }
            `)
        ));

    test('hierarchic namespaces with body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
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
        ));

    test('namespace with expression', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                /*pref*/
                declare namespace /*middle*/
                TestNamespace /*after*/
                {
                    /*bcd*/
                    /*1*/
                    const a = 10;
                    /*efg*/

                    /*2*/
                    function abc() {
                        return null
                    }

                    /*fgh*/

                    /*3*/
                    class X {
                        b: number;
                        c: string;
                    }

                    /*ghj*/
                }
            `)
        ));

    test('complex test with namaspaces and modules', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
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
        ));

    test('empty body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                declare module 'vue-count-to' /*a*/
                ;/*b*/
            `)
        ));

    test('namespace export as', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export {}
                declare namespace MyLibrary {
                    function sayHello(name: string): void;
                }

                /*a*/
                export /*b*/ as /*c*/ namespace /*d*/ MyLibrary/*e*/;
            `)
        ));

    test('complex namespace export as', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export {Fraction}

                export as namespace math /*a*/

                /*b*/
                type NoLiteralType<T> = T extends number
                    ? number
                    : T extends string
                        ? string
                        : T extends boolean
                            ? boolean
                            : T
            `)
        ));

    test('extend global type definitions without namespace keyword', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                declare global {
                    interface Window {
                        myCustomGlobalFunction?: () => void; // Add a custom global function
                    }
                }
            `)
        ));
});
