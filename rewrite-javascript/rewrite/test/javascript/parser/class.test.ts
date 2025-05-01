/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";

describe('class mapping', () => {
    const spec = new RecipeSpec();

    test('empty', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('class A {}')
        );
    });
    test('type parameter', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('class A   <   T   ,   G> {}')
        );
    });
    test('body', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('class A { foo: number; }')
        );
    });
    test('extends', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('class A extends Object {}')
        );
    });
    test('implements single', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('class A implements B {}')
        );
    });
    test('implements multiple', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('class A implements B , C,D {}')
        );
    });
    test('extends and implements', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('class A extends Object implements B , C,D {}')
        );
    });
    test('export', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('export class A {}')
        );
    });
    test('export default', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('export default class A {}')
        );
    });

    test('class with properties', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`
                class X {
                    a = 5
                    b = 6
                }
          `)
        );
    });

    test('class with properties and semicolon', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`
               class X {
                  a = 5;
                  b = 6;
              } /*asdasdas*/
              //asdasf
          `)
        );
    });

    test('class with mixed properties with semicolons', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`
              class X {

                  b = 6
                  c = 10;
                  a /*asdasd*/ =  /*abc*/   5
                  d = "d";

              } /*asdasdas*/
              //asdasf
          `)
        );
    });

    test('class with properties, semicolon, methods, comments', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`
                class X {
                    a /*asdasd*/ =  /*abc*/   5;
                    b = 6;

                    // method 1
                    abs(x): string{
                        return "1";
                    }

                    //method 2
                    max(x, y /*a*/, /*b*/): number {
                        return 2;
                    }
                } /*asdasdas*/
                //asdasf
          `)
        );
    });

    test('class with several typed properties', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`
                class X {

                    b: number = 6
                    c: string = "abc";
                    a /*asdasd*/ =  /*abc*/   5

                } /*asdasdas*/
                //asdasf
          `)
        );
    });

    test('class with reference-typed property', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`
              class X {

                  a: globalThis.Promise<string> = null;
                  b: number = 6;
                  c /*asdasd*/ =  /*abc*/   5

              } /*asdasdas*/
              //asdasf
          `)
        );
    });

    test('class with typed properties, modifiers, comments', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`
              class X /*abc*/ {
                  public name   /*asdasda*/    :  /*dasdasda*/   string;
                  private surname   /*asdasda*/    :  /*dasdasda*/   string =  "abc";
                  b: number /* abc */ = 6;
                  c = 10;
                  a /*asdasd*/ =  /*abc*/   5

              }
              //asdasf
          `)
        );
    });

    test('class with simple ctor', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`class A {
              constructor() {
              }
          }`)
        );
    });

    test('class with ctor as literal', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`class A {
              "constructor"() {
              }
          }`)
        );
    });

    test('class with private ctor', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`class A {
              /*0*/     private      /*1*/   constructor  /*2*/    (    /*3*/  )  /*4*/     {
              }
          }`)
        );
    });

    test('class with parametrized ctor', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`class A {
                    /*1*/   constructor  /*2*/    (  a,  /*3*/   b   :     string, /*4*/    c     /*5*/     ) {
              }
          }`)
        );
    });

    test('class with type parameters', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class A<T> {
                }
            `)
        );
    });

    test('anonymous class expression', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                const MyClass = class {
                    constructor(public name: string) {
                    }
                };
            `)
        );
    });

    test('anonymous class expression with comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                const MyClass = /*a*/class/*b*/ {/*c*/
                    constructor(public name: string) {
                    }
                };
            `)
        );
    });

    test('named class expression', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                const Employee = class EmployeeClass {
                    constructor(public position: string, public salary: number, ) {
                    }
                };
            `)
        );
    });

    test('class extends expression', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class OuterClass extends (class extends Number { }) {
                }
            `)
        );
    });

    test('class extends expression with constructor', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class OuterClass extends (class extends Number {
                    constructor() {
                        /*1*/ super /*2*/ () /*3*/;
                    }
                }) {
                    constructor() {
                        /*1*/ super /*2*/ () /*3*/;
                    }
                }
            `)
        );
    });

    test('class expressions inline', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                function createInstance(ClassType: new () => any) {
                    return new ClassType();
                }

                const instance = createInstance(class {
                    sayHello() {
                        console.log("Hello from an inline class!");
                    }
                });
            `)
        );
    });

    test('inner class declaration with extends', () => {
     spec.rewriteRun(
        //language=typescript
        typescript(`
            class OuterClass {
              public static InnerClass = class extends Number { };
            }
        `)
      );
    });

    test('class with optional properties, ctor and modifiers', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class Employee {
                    public id: number;
                    protected name: string;
                    private department?: string; // Optional property

                    constructor(id: number, name: string, department?: string) {
                        this.id = id;
                        this.name = name;
                        this.department = department;
                    }
                }
          `)
        );
    });

    test('class with optional properties and methods', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class Person {
                    name: string;
                    age?: number;              // Optional property
                    greetFirst ?: () => void;   // Optional method
                    greetSecond ?(): string;    // Optional method
                }
            `)
        );
    });

    test('class with optional methods with generics', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class Person {
                      // Optional methods
                    transform?<T>(input: T): T { return input; }   // Optional method
                    echo ? < R > (input: R): R { return input; }   // Optional method
                }
            `)
        );
    });

    test('class with get/set accessors', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class Person {
                    private _name = '';

                    // Getter
                    public get name(): string {
                        return this._name;
                    }

                    // Setter
                    set name(value: string) {
                        this._name = value;
                    }
                }
            `)
        );
    });

    test('class with get/set accessors with comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class Person {
                    private _name = '';

                    // Getter
                    public /*a*/ get /*b*/ name/*c*/(/*d*/): string {
                        return this._name;
                    }

                    // Setter
                    public /*a*/ set /*b*/ name/*c*/(/*d*/value/*e*/: /*f*/ string /*g*/) {
                        this._name = value;
                    }
                }
            `)
        );
    });

    test('class with static blocks', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class Example {
                    static valueA: number;
                    static valueB: number;

                    static {
                        this.valueA = 10
                        console.log("Static block 1 executed. valueA:", this.valueA);
                    }

                    static {
                        this.valueB = this.valueA * 2
                        console.log("Static block 2 executed. valueB:", this.valueB);
                    }
                }
          `)
        );
    });

    test('class with static blocks and comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class Example {
                    static /*1*/ valueA /*2*/: /*3*/ number;
                    static valueB: number;

                    /*a*/ static /*b*/{ /*c*/
                        this.valueA = 10  /*d*/; /*e*/
                        console.log("Static block 1 executed. valueA:", this.valueA)  /*f*/
                       }

                    /*g*/static{
                        this.valueB = this.valueA * 2  /*h*/
                        console.log("Static block 2 executed. valueB:", this.valueB);
                    }
                }
          `)
        );
    });

    test('class with SemicolonClassElement', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class MyClass {
                    /*a*/; // This is a SemicolonClassElement
                    myMethod() {
                        console.log('Hello');
                    }
                }
            `)
        );
    });

    test('this type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class FluentAPI {
                    use_this(): this {
                        return this; // \`this\` refers to the current instance
                    }
                }
            `)
        );
    });

    test('new class with type arguments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                const _onDidChangeEnvironment = /*a*/new/*b*/ EventEmitter/*c*/</*d*/string/*e*/>/*f*/(/*g*/)/*h*/;
            `)
        );
    });

    test('property declaration', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                export class CodeLoopbackClient {
                    private server!: http.Server | https.Server;
                }
            `)
        );
    });

    test('static method with asterisk', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class A {
                    public static/*a*/*/*b*/getMarkdownRestSnippets?(document: TextDocument): Generator<Range> {
                    }
                }
            `)
        );
    });

    test('new expression parentheses', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class Base {
                    attrs: any;

                    constructor(attrs: any) {
                        this.attrs = attrs;
                    }

                    clone() {
                        return new (<any>this.constructor)(this.attrs);
                    }
                }
            `)
        );
    });

    test('get/set accessor with a name as expression', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                export function mochaGlobalSetup() {
                    globalThis.Path2D ??= class Path2D {
                        constructor(path) {
                            this.path = path
                        }

                        get [Symbol.toStringTag]() {
                            return 'Path2D';
                        }

                        set [Symbol.toStringTag](path) {
                        }
                    }
                }
            `)
        );
    });

    test('class with type param with trailing comma', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                export class APIError<
                    TData extends null | object = { [key: string]: unknown } | null,
                > extends ExtendableError<TData> {}
            `)
        );
    });

    test('new anonymous class with type params', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                new (class/*a*/<T extends object> extends WeakRef<T> {
                    foo = "bar";
                })({hello: "world"});
            `)
        );
    });
});
