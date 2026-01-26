// noinspection TypeScriptUnresolvedReference,JSUnusedLocalSymbols,TypeScriptValidateTypes

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
import {JS, typescript} from "../../../src/javascript";
import {J, Type} from "../../../src/java";
import {tap} from "../../test-util";

describe('class mapping', () => {
    const spec = new RecipeSpec();

    test('empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('class A {}')
        ));

    test('type parameter', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('class A   <   T   ,   G> {}')
        ));

    test('body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('class A { foo: number; }')
        ));

    test('extends', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('class A extends Object {}')
        ));

    test('implements single', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('class A implements B {}')
        ));

    test('implements multiple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('class A implements B , C,D {}')
        ));

    test('extends and implements', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('class A extends Object implements B , C,D {}')
        ));

    test('export', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('export class A {}')
        ));

    test('export default', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('export default class A {}')
        ));

    test('class with properties', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class X {
                    a = 5
                    b = 6
                }
            `)
        ));

    test('class with properties and semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class X {
                    a = 5;
                    b = 6;
                } /*asdasdas*/
                //asdasf
            `)
        ));

    test('class with mixed properties with semicolons', () =>
        spec.rewriteRun(
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
        ));

    test('class with properties, semicolon, methods, comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class X {
                    a /*asdasd*/ =  /*abc*/   5;
                    b = 6;

                    // method 1
                    abs(x): string {
                        return "1";
                    }

                    //method 2
                    max(x, y /*a*/, /*b*/): number {
                        return 2;
                    }
                } /*asdasdas*/
                //asdasf
            `)
        ));

    test('class with several typed properties', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class X {

                    b: number = 6
                    c: string = "abc";
                    a /*asdasd*/ =  /*abc*/   5

                } /*asdasdas*/
                //asdasf
            `)
        ));

    test('class with reference-typed property', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class X {

                    a: globalThis.Promise<string> = null;
                    b: number = 6;
                    c /*asdasd*/ =  /*abc*/   5

                } /*asdasdas*/
                //asdasf
            `)
        ));

    test('class with typed properties, modifiers, comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class X /*abc*/ {
                    public name   /*asdasda*/:  /*dasdasda*/   string;
                    private surname   /*asdasda*/:  /*dasdasda*/   string = "abc";
                    b: number /* abc */ = 6;
                    c = 10;
                    a /*asdasd*/ =  /*abc*/   5

                }

                //asdasf
            `)
        ));

    test('class with simple ctor', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`class A {
                constructor() {
                }
            }`)
        ));

    test('class with ctor as literal', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`class A {
                "constructor"() {
                }
            }`)
        ));

    test('class with private ctor', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`class A {
                /*0*/
                private      /*1*/   constructor  /*2*/(    /*3*/)  /*4*/ {
                }
            }`)
        ));

    test('class with parametrized ctor', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`class A {
                /*1*/
                constructor  /*2*/(a,  /*3*/   b: string, /*4*/    c     /*5*/) {
                }
            }`)
        ));

    test('class with type parameters', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class A<T> {
                }
            `)
        ));

    test('anonymous class expression', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const MyClass = class {
                    constructor(public name: string) {
                    }
                };
            `)
        ));

    test('anonymous class expression with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const MyClass = /*a*/class/*b*/ {/*c*/
                    constructor(public name: string) {
                    }
                };
            `)
        ));

    test('named class expression', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const Employee = class EmployeeClass {
                    constructor(public position: string, public salary: number,) {
                    }
                };
            `)
        ));

    test('class extends expression', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class OuterClass extends (class extends Number {
                }) {
                }
            `)
        ));

    test('class extends expression with constructor', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class OuterClass extends (class extends Number {
                    constructor() {
                        /*1*/
                        super /*2*/ () /*3*/;
                    }
                }) {
                    constructor() {
                        /*1*/
                        super /*2*/ () /*3*/;
                    }
                }
            `)
        ));

    test('class expressions inline', () =>
        spec.rewriteRun(
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
        ));

    test('inner class declaration with extends', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class OuterClass {
                    public static InnerClass = class extends Number {
                    };
                }
            `)
        ));

    test('class with optional properties, ctor and modifiers', () =>
        spec.rewriteRun(
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
        ));

    test('class with optional properties and methods', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Person {
                    name: string;
                    age?: number;              // Optional property
                    greetFirst ?: () => void;   // Optional method
                    greetSecond ?(): string;    // Optional method
                }
            `)
        ));

    test('class with optional methods with generics', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Person {
                    // Optional methods
                    transform?<T>(input: T): T {
                        return input;
                    }   // Optional method
                    echo ?<R>(input: R): R {
                        return input;
                    }   // Optional method
                }
            `)
        ));

    test('class with get/set accessors', () =>
        spec.rewriteRun(
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
        ));

    test('class with get/set accessors with comments', () =>
        spec.rewriteRun(
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
        ));

    test('class with static blocks', () =>
        spec.rewriteRun(
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
        ));

    test('class with static blocks and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class Example {
                    static /*1*/ valueA /*2*/: /*3*/ number;
                    static valueB: number;

                    /*a*/
                    static /*b*/{ /*c*/
                        this.valueA = 10  /*d*/; /*e*/
                        console.log("Static block 1 executed. valueA:", this.valueA)  /*f*/
                    }

                    /*g*/
                    static {
                        this.valueB = this.valueA * 2  /*h*/
                        console.log("Static block 2 executed. valueB:", this.valueB);
                    }
                }
            `)
        ));

    test('class with SemicolonClassElement', () =>
        spec.rewriteRun(
            //@formatter:off
            typescript(`
                class MyClass {
                    /*a*/; // This is a SemicolonClassElement
                    myMethod() {
                        console.log('Hello');
                    }
                }
            `)
            //@formatter:on
        ));

    test('this type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class FluentAPI {
                    use_this(): this {
                        return this; // \`this\` refers to the current instance
                    }
                }
            `)
        ));

    test('new class with type arguments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const _onDidChangeEnvironment = /*a*/new/*b*/ EventEmitter/*c*/</*d*/string/*e*/>/*f*/(/*g*/)/*h*/;
            `)
        ));

    test('property declaration', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export class CodeLoopbackClient {
                    private server!: http.Server | https.Server;
                }
            `)
        ));

    test('static generator method (uses asterisk syntax)', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class A {
                    public static/*a*/*/*b*/getMarkdownRestSnippets?(document: TextDocument): Generator<Range> {
                    }
                }
            `)
        ));

    test('new expression parentheses', () =>
        spec.rewriteRun(
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
        ));

    test('get/set accessor with a name as expression', () =>
        spec.rewriteRun(
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
        ));

    test('class with type param with trailing comma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export class APIError<
                    TData extends null | object = { [key: string]: unknown } | null,
                > extends ExtendableError<TData> {
                }
            `)
        ));

    test('new anonymous class with type params', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                new (class/*a*/<T extends object> extends WeakRef<T> {
                    foo = "bar";
                })({hello: "world"});
            `)
        ));

    test('class type mapping', async () => {
        const spec = new RecipeSpec();
        //language=typescript
        const source = typescript(`
            class Base {
                s: string;

                constructor(private n: number, s: string) {
                    this.attrs = attrs;
                    this.s = s;
                }

                m(): boolean {
                    return true;
                }
            }
            let base: Base;
        `)
        source.afterRecipe = tree => {
            const varDecl = tree.statements[1] as unknown as J.VariableDeclarations;
            const ident = varDecl.variables[0].name as J.Identifier;
            expect(ident.simpleName).toEqual("base");
        }
        await spec.rewriteRun(source);
    })
});
