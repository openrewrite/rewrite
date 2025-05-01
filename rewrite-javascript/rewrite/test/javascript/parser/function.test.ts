import {connect, disconnect, rewriteRun, rewriteRunWithOptions, typeScript} from '../testHarness';

describe('function mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript('function f () /*a*/{/*b*/ let c = 1; }')
        );
    });
    test('single parameter', () => {
        rewriteRun(
          //language=typescript
          typeScript('function f(a) {}')
        );
    });
    test('single typed parameter', () => {
        rewriteRun(
          //language=typescript
          typeScript('function f(a : number) {}')
        );
    });
    test('single typed parameter with initializer', () => {
        rewriteRun(
          //language=typescript
          typeScript('function f(a /*0*/ : /*1*/ number /*2*/ = /*3*/ 2 /*4*/ ) {}')
        );
    });
    test('single parameter with initializer', () => {
        rewriteRun(
          //language=typescript
          typeScript('function f(a =  2) {}')
        );
    });
    test('two parameters', () => {
        rewriteRun(
          //language=typescript
          typeScript('function f(a =  2 , b) {}')
        );
    });

    test('parameter with trailing comma', () => {
        rewriteRun(
          //language=typescript
          typeScript('function f(a  , ) {}')
        );
    });

    test('function with type params', () => {
      rewriteRun(
        //language=typescript
        typeScript(`
           function  /*1*/   identity  /*2*/    <  Type  , G    ,   C   >       (arg: Type)  /*3*/ :     G  {
            return arg;
          }
        `)
      );
    });

    test('function with modifiers', () => {
        rewriteRun(
            //language=typescript
            typeScript('export async function f (a =  2 , b) {}')
        );
    });

    test('function with modifiers and without name', () => {
        rewriteRun(
            //language=typescript
            typeScript('export default function(hljs) {}')
        );
    });

    test('function with modifiers and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript('/*a*/ export /*b*/ async /*c*/ function /*d*/f /*e*/ (a =  2 , b) {}')
        );
    });

    test('function expression', () => {
        rewriteRun(
            //language=typescript
            typeScript('const greet = function  (name: string) : string { return name; }')
        );
    });

    test('function expression with type parameter', () => {
        rewriteRun(
            //language=typescript
            typeScript('const greet = function<T> (name: T): number { return 1; }')
        );
    });

    test('function expression with type parameter and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript('const greet = /*a*/ function/*b*/ </*c*/T/*d*/>/*e*/(/*g*/name/*h*/:/*j*/T)/*k*/:/*l*/ number /*m*/{ return 1; }')
        );
    });

    test('function with void return type', () => {
        rewriteRun(
            //language=typescript
            typeScript('function f ( a : string ) : void {}')
        );
    });

    test('function type expressions', () => {
        rewriteRun(
            //language=typescript
            typeScript('function greeter(fn: (a: string) => void) { fn("Hello, World"); }')
        );
    });

    test('function with type ref', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function getLength(arr: Array<string>): number {
                    return arr.length;
                }
            `)
        );
    });

    test('function declaration with obj binding params', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
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
        );
    });

    test('function type with parameter', () => {
        rewriteRun(
            //language=typescript
            typeScript('type Transformer<T> = (input: T) => T;')
        );
    });

    test('parameter with anonymous type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
               function create<Type>(c: { new (): Type }): Type {
                  return new c();
               }
            `)
        );
    });

    test('immediately invoked anonymous function', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                (function() {
                    console.log('IIFE');
                })/*a*/();
            `)
        );
    });

    test('immediately invoked anonymous function with ?.', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                (function() {
                    console.log('IIFE');
                })/*a*/?./*b*/();
            `)
        );
    });

    test('function expression with name assigment', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                var helloString = 'Hello world!';

                var hello = function hello() {
                    return helloString;
                };
            `)
        );
    });

    test('function expression with name assigment with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                var helloString = 'Hello world!';

                var hello = /*a*/function/*b*/ hello /*c*/(/*d*/) {
                    return helloString;
                };
            `)
        );
    });

    test('function with simple type bound', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function greet<T extends Number>(person: T): void {}
            `)
        );
    });

    test('function with simple type bound and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function /*a*/greet/*b*/</*c*/T /*d*/ extends /*e*/ Number/*f*/>/*g*/(person: T): void {}
            `)
        );
    });

    test('function with union type bound', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function getLength<T extends string | number>(input: T) : void {}
            `)
        );
    });

    test('function with multiple type bound', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function merge<T extends object, U extends object>(obj1: T, obj2: U): T & U {
                    return { ...obj1, ...obj2 };
                }
            `)
        );
    });

    test('function with default type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function createArray<T = string>(value: T, count: number): T[] {
                    return Array(count).fill(value);
                }
            `)
        );
    });

    test('function with default type and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function createArray<T /*a*/ = /*b*/string /*c*/>(value: T, count: number): T[] {
                    return Array(count).fill(value);
                }
            `)
        );
    });

    test('function with multiple default types', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function createMap<K = string, V = number>(key: K, value: V): [K, V] {
                    return [key, value];
                }
            `)
        );
    });

    test('function with extends and default type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function createArray<T extends string | number = string>(value: T, length: number): T[] {
                    return Array(length).fill(value);
                }
            `)
        );
    });

    test('function with extends and default type with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function createArray<T /*-2*/extends/*-1*/ string | number /*0*/ = /*1*/ string/*2*/>/*3*/(value: T, length: number): /*a*/T/*b*/[/*c*/]/*d*/ {
                    return Array(length).fill(value);
                }
            `)
        );
    });

    test('function with constrained type literal ', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function logLength<T extends { length: number }>(input: T): void {}
            `)
        );
    });

    test('function with rest type parameters ', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function f(...args: any[]): void {}
            `)
        );
    });

    test('function with rest type parameters and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function /*a*/f/*b*/(/*c*/.../*d*/args/*e*/: /*f*/any[]): void {}
            `)
        );
    });

    test('unnamed function', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                /*1*/ export /*2*/ default /*3*/ function /*4*/(/*5*/hljs/*6*/) /*7*/ {
                }
            `)
        );
    });

    test('function with type predicate simple', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function isString(value: unknown): value is string {
                    return typeof value === 'string';
                }
            `)
        );
    });

    test('function with type predicate simple with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function isString(value: unknown): /*a*/value /*b*/is/*c*/ string/*d*/ {
                    return typeof value === 'string';
                }
            `)
        );
    });

    test('function with type predicate and asserts', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function assertIsString(value: unknown): asserts value is string {
                    if (typeof value !== "string") {
                        throw new Error("Value is not a string");
                    }
                }
            `)
        );
    });

    test('function with type predicate and asserts with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function assertIsString(value: unknown): /*a*/asserts/*b*/ value /*c*/is/*d*/ string /*e*/{
                    if (typeof value !== "string") {
                        throw new Error("Value is not a string");
                    }
                }
            `)
        );
    });

    test('function with type predicate, asserts and without type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function assert(value: unknown, message: string): asserts /*a*/value/*b*/ {
                    if (!value) {
                        throw new Error(message);
                    }
                }
            `)
        );
    });

    test('function with type predicate, asserts and complex type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Animal = { kind: "dog"; bark(): void } | { kind: "cat"; purr(): void };

                function isDog(animal: Animal): animal is { kind: "dog"; bark(): void } {
                    return animal.kind === "dog";
                }
            `)
        );
    });

    test('no additional comma test', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                new Promise(function() {
                   let x;
                   let y;
                })
            `)
        );
    });

    test('empty body function', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export function getHeader(headers: ResponseHeaders, name: string): ResponseHeaderValue;
            `)
        );
    });

    test('function invocation', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                !function(e, t) {
                    console.log("This is an IIFE", e, t);
                }("Hello", "World");
            `)
        );
    });
});
