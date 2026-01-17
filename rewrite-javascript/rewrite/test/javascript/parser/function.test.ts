// noinspection TypeScriptUnresolvedReference,TypeScriptValidateTypes,JSUnusedLocalSymbols

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
import {J} from "../../../src/java";

describe('function mapping', () => {
    const spec = new RecipeSpec();

    // noinspection JSUnusedLocalSymbols
    test('simple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function f () /*a*/{/*b*/ let c = 1; }')
        ));
    test('single parameter', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function f(a) {}')
        ));
    test('single typed parameter', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function f(a : number) {}')
        ));
    test('single typed parameter with initializer', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function f(a /*0*/ : /*1*/ number /*2*/ = /*3*/ 2 /*4*/ ) {}')
        ));
    test('single parameter with initializer', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function f(a =  2) {}')
        ));
    test('two parameters', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function f(a =  2 , b) {}')
        ));

    test('parameter with trailing comma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function f(a  , ) {}')
        ));

    test('function with type params', () =>
        spec.rewriteRun(
            //@formatter:off
            //language=typescript
            typescript(`
              function  /*1*/   identity  /*2*/    <  Type  , G    ,   C   >       (arg: Type)  /*3*/ :     G  {
               return arg;
             }
            `)
            //@formatter:on
        ));

    test('function with modifiers', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('export async function f (a =  2 , b) {}')
        ));

    test('function with modifiers and without name', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('export default function(hljs) {}')
        ));

    test('function with modifiers and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('/*a*/ export /*b*/ async /*c*/ function /*d*/f /*e*/ (a =  2 , b) {}')
        ));

    test('function expression', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const greet = function  (name: string) : string { return name; }')
        ));

    test('function expression with type parameter', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const greet = function<T> (name: T): number { return 1; }')
        ));

    test('function expression with type parameter and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const greet = /*a*/ function/*b*/ </*c*/T/*d*/>/*e*/(/*g*/name/*h*/:/*j*/T)/*k*/:/*l*/ number /*m*/{ return 1; }')
        ));

    test('function with void return type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function f ( a : string ) : void {}')
        ));

    test('function type expressions', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function greeter(fn: (a: string) => void) { fn("Hello, World"); }')
        ));

    test('function with type ref', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function getLength(arr: Array<string>): number {
                    return arr.length;
                }
            `)
        ));

    test('function declaration with obj binding params', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export function reverseGeocode(
                    {
                        params,
                        method = "get",
                        url = defaultUrl,
                        paramsSerializer = defaultParamsSerializer,
                        ...config
                    }: ReverseGeocodeRequest,
                    axiosInstance: AxiosInstance = defaultAxiosInstance
                ): Promise<ReverseGeocodeResponse> {
                    return axiosInstance({
                        params,
                        method,
                        url,
                        paramsSerializer,
                        ...config,
                    }) as Promise<ReverseGeocodeResponse>;
                }
            `)
        ));

    test('function type with parameter', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('type Transformer<T> = (input: T) => T;')
        ));

    test('parameter with anonymous type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function create<Type>(c: { new(): Type }): Type {
                    return new c();
                }
            `)
        ));

    test('immediately invoked anonymous function', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                (function () {
                    console.log('IIFE');
                })/*a*/();
            `)
        ));

    test('immediately invoked anonymous function with ?.', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                (function () {
                    console.log('IIFE');
                })/*a*/?./*b*/();
            `)
        ));

    test('function expression with name assigment', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                var helloString = 'Hello world!';

                var hello = function hello() {
                    return helloString;
                };
            `)
        ));

    test('function expression with name assigment with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                var helloString = 'Hello world!';

                var hello = /*a*/function/*b*/ hello /*c*/(/*d*/) {
                    return helloString;
                };
            `)
        ));

    test('function with simple type bound', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function greet<T extends Number>(person: T): void {
                }
            `)
        ));

    test('function with simple type bound and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function /*a*/greet/*b*/</*c*/T /*d*/ extends /*e*/ Number/*f*/>/*g*/(person: T): void {
                }
            `)
        ));

    test('function with union type bound', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function getLength<T extends string | number>(input: T): void {
                }
            `)
        ));

    test('function with multiple type bound', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function merge<T extends object, U extends object>(obj1: T, obj2: U): T & U {
                    return {...obj1, ...obj2};
                }
            `)
        ));

    test('function with default type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function createArray<T = string>(value: T, count: number): T[] {
                    return Array(count).fill(value);
                }
            `)
        ));

    test('function with default type and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function createArray<T /*a*/ = /*b*/string /*c*/>(value: T, count: number): T[] {
                    return Array(count).fill(value);
                }
            `)
        ));

    test('function with multiple default types', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function createMap<K = string, V = number>(key: K, value: V): [K, V] {
                    return [key, value];
                }
            `)
        ));

    test('function with extends and default type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function createArray<T extends string | number = string>(value: T, length: number): T[] {
                    return Array(length).fill(value);
                }
            `)
        ));

    test('function with extends and default type with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function createArray<T /*-2*/ extends/*-1*/ string | number /*0*/ = /*1*/ string/*2*/>/*3*/(value: T, length: number): /*a*/T/*b*/[/*c*/]/*d*/ {
                    return Array(length).fill(value);
                }
            `)
        ));

    test('function with constrained type literal ', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function logLength<T extends { length: number }>(input: T): void {
                }
            `)
        ));

    test('function with rest type parameters ', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function f(...args: any[]): void {
                }
            `)
        ));

    test('function with rest type parameters and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function /*a*/f/*b*/(/*c*/.../*d*/args/*e*/: /*f*/any[]): void {
                }
            `)
        ));

    test('unnamed function', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                /*1*/
                export /*2*/ default /*3*/ function /*4*/(/*5*/hljs/*6*/) /*7*/ {
                }
            `)
        ));

    test('function with type predicate simple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function isString(value: unknown): value is string {
                    return typeof value === 'string';
                }
            `)
        ));

    test('function with type predicate simple with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function isString(value: unknown): /*a*/value /*b*/is/*c*/ string/*d*/ {
                    return typeof value === 'string';
                }
            `)
        ));

    test('function with type predicate and asserts', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function assertIsString(value: unknown): asserts value is string {
                    if (typeof value !== "string") {
                        throw new Error("Value is not a string");
                    }
                }
            `)
        ));

    test('function with type predicate and asserts with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function assertIsString(value: unknown): /*a*/asserts/*b*/ value /*c*/is/*d*/ string /*e*/ {
                    if (typeof value !== "string") {
                        throw new Error("Value is not a string");
                    }
                }
            `)
        ));

    test('function with type predicate, asserts and without type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function assert(value: unknown, message: string): asserts /*a*/value/*b*/ {
                    if (!value) {
                        throw new Error(message);
                    }
                }
            `)
        ));

    test('function with type predicate, asserts and complex type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                type Animal = { kind: "dog"; bark(): void } | { kind: "cat"; purr(): void };

                function isDog(animal: Animal): animal is { kind: "dog"; bark(): void } {
                    return animal.kind === "dog";
                }
            `)
        ));

    test('no additional comma test', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                new Promise(function () {
                    let x;
                    let y;
                })
            `)
        ));

    test('empty body function', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                // noinspection JSAnnotator

                export function getHeader(headers: ResponseHeaders, name: string): ResponseHeaderValue;
            `)
        ));

    test('function invocation', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                !function (e, t) {
                    console.log("This is an IIFE", e, t);
                }("Hello", "World");
            `)
        ));

    test('function type mapping', async () => {
        await new RecipeSpec().rewriteRun({
            //language=typescript
            ...typescript(
                `
                    function f(s: string): number {
                        return s.length;
                    }

                    const a = f;
                `
            ),
            afterRecipe: tree => {
                const varDecl = tree.statements[1] as unknown as J.VariableDeclarations;
                const ident = varDecl.variables[0].name as J.Identifier;
                expect(ident.simpleName).toEqual("a");
            }
        });
    })
});
