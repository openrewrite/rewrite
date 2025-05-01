import {connect, disconnect, rewriteRun, rewriteRunWithOptions, typeScript} from '../testHarness';

describe('class mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('empty', () => {
        rewriteRun(
          //language=typescript
          typeScript('class A {}')
        );
    });
    test('type parameter', () => {
        rewriteRun(
          //language=typescript
          typeScript('class A   <   T   ,   G> {}')
        );
    });
    test('body', () => {
        rewriteRun(
          //language=typescript
          typeScript('class A { foo: number; }')
        );
    });
    test('extends', () => {
        rewriteRun(
          //language=typescript
          typeScript('class A extends Object {}')
        );
    });
    test('implements single', () => {
        rewriteRun(
          //language=typescript
          typeScript('class A implements B {}')
        );
    });
    test('implements multiple', () => {
        rewriteRun(
          //language=typescript
          typeScript('class A implements B , C,D {}')
        );
    });
    test('extends and implements', () => {
        rewriteRun(
          //language=typescript
          typeScript('class A extends Object implements B , C,D {}')
        );
    });
    test('export', () => {
        rewriteRun(
          //language=typescript
          typeScript('export class A {}')
        );
    });
    test('export default', () => {
        rewriteRun(
          //language=typescript
          typeScript('export default class A {}')
        );
    });

    test('class with properties', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
                class X {
                    a = 5
                    b = 6
                }
          `)
        );
    });

    test('class with properties and semicolon', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
               class X {
                  a = 5;
                  b = 6;
              } /*asdasdas*/
              //asdasf
          `)
        );
    });

    test('class with mixed properties with semicolons', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
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
        rewriteRun(
          //language=typescript
          typeScript(`
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
        rewriteRun(
          //language=typescript
          typeScript(`
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
        rewriteRun(
          //language=typescript
          typeScript(`
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
        rewriteRun(
          //language=typescript
          typeScript(`
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
        rewriteRun(
          //language=typescript
          typeScript(`class A {
              constructor() {
              }
          }`)
        );
    });

    test('class with ctor as literal', () => {
        rewriteRun(
          //language=typescript
          typeScript(`class A {
              "constructor"() {
              }
          }`)
        );
    });

    test('class with private ctor', () => {
        rewriteRun(
            //language=typescript
            typeScript(`class A {
              /*0*/     private      /*1*/   constructor  /*2*/    (    /*3*/  )  /*4*/     {
              }
          }`)
        );
    });

    test('class with parametrized ctor', () => {
        rewriteRun(
            //language=typescript
            typeScript(`class A {
                    /*1*/   constructor  /*2*/    (  a,  /*3*/   b   :     string, /*4*/    c     /*5*/     ) {
              }
          }`)
        );
    });

    test('class with type parameters', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                class A<T> {
                }
            `)
        );
    });

    test('anonymous class expression', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const MyClass = class {
                    constructor(public name: string) {
                    }
                };
            `)
        );
    });

    test('anonymous class expression with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const MyClass = /*a*/class/*b*/ {/*c*/
                    constructor(public name: string) {
                    }
                };
            `)
        );
    });

    test('named class expression', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const Employee = class EmployeeClass {
                    constructor(public position: string, public salary: number, ) {
                    }
                };
            `)
        );
    });

    test('class extends expression', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                class OuterClass extends (class extends Number { }) {
                }
            `)
        );
    });

    test('class extends expression with constructor', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
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
      rewriteRun(
        //language=typescript
        typeScript(`
            class OuterClass {
              public static InnerClass = class extends Number { };
            }
        `)
      );
    });

    test('class with optional properties, ctor and modifiers', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
                class Person {
                      // Optional methods
                    transform?<T>(input: T): T { return input; }   // Optional method
                    echo ? < R > (input: R): R { return input; }   // Optional method
                }
            `)
        );
    });

    test('class with get/set accessors', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
                class FluentAPI {
                    use_this(): this {
                        return this; // \`this\` refers to the current instance
                    }
                }
            `)
        );
    });

    test('new class with type arguments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const _onDidChangeEnvironment = /*a*/new/*b*/ EventEmitter/*c*/</*d*/string/*e*/>/*f*/(/*g*/)/*h*/;
            `)
        );
    });

    test('property declaration', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export class CodeLoopbackClient {
                    private server!: http.Server | https.Server;
                }
            `)
        );
    });

    test('static method with asterisk', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                class A {
                    public static/*a*/*/*b*/getMarkdownRestSnippets?(document: TextDocument): Generator<Range> {
                    }
                }
            `)
        );
    });

    test('new expression parentheses', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
                export class APIError<
                    TData extends null | object = { [key: string]: unknown } | null,
                > extends ExtendableError<TData> {}
            `)
        );
    });

    test('new anonymous class with type params', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                new (class/*a*/<T extends object> extends WeakRef<T> {
                    foo = "bar";
                })({hello: "world"});
            `)
        );
    });

});
