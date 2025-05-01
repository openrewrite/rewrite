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

describe('type alias mapping', () => {
    const spec = new RecipeSpec();

    test('simple alias', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type StringAlias = string;
            `)
        );
    });

    test('simple alias with comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                /*a*/type /*b*/ StringAlias /*c*/= /*d*/string /*e*/;/*f*/
            `)
        );
    });

    test('function type alias', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type MyFunctionType = (x: number, y: number) => string;
            `)
        );
    });

    test('generic function type alias', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type Response<T, R, Y> = (x: T, y: R) => Y;;
            `)
        );
    });

    test('generic type alias with comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                /*a*/type/*b*/ Response/*c*/</*d*/T/*e*/> /*f*/ = /*g*/(x: T, y: number) => string;;
            `)
        );
    });

    test('union type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type ID = /*a*/ number /*b*/ | /*c*/ string /*d*/;
            `)
        );
    });

    test('union type with comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type ID = number | string;
            `)
        );
    });

    test('construct function type alias', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type MyConstructor = abstract new (arg: string) => string;
            `)
        );
    });

    test('construct function type alias with comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type MyConstructor = /*a*/new/*b*/ (/*c*/arg: string) => string;
            `)
        );
    });

    test('construct function type alias with abstract and comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type MyConstructor = /*0*/ abstract /*a*/new/*b*/ (/*c*/arg: string) => string;
            `)
        );
    });

    test('recursive array type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type NestedArray<T> = T | NestedArray<T[]>;
            `)
        );
    });

    test('construct function type alias with generic', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type GenericConstructor<T> = new (/*a*/.../*b*/args: any[]) => T;
            `)
        );
    });

    test('tuple type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type MyTuple = [number, string, boolean];
            `)
        );
    });

    test('tuple type with comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type MyTuple = /*a*/[/*b*/number/*c*/, /*d*/string/*e*/, /*f*/boolean/*g*/, /*h*/]/*j*/;
            `)
        );
    });

    test('tuple type empty', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type MyTuple = [/*a*/];
            `)
        );
    });

    test('nested tuple type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type NestedTuple = [number, [string, boolean]];
            `)
        );
    });

    test('optional tuple type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type OptionalTuple = [string, /*a*/number/*b*/?/*c*/];
            `)
        );
    });

    test('tuple rest type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type FlexibleTuple = [string, ...number[]];
            `)
        );
    });

    test('readonly operator tuple type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type ReadonlyTuple = readonly [string, number];
            `)
        );
    });

    test('readonly operator tuple type with comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type ReadonlyTuple = /*a*/keyof /*b*/ [string, number];
            `)
        );
    });

    test('basic conditional type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type IsString<T> = T extends string ? 'Yes' : 'No';
            `)
        );
    });

    test('basic conditional type with comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type IsString<T> = /*a*/T/*b*/ extends /*c*/string /*d*/? /*e*/'Yes' /*f*/:/*g*/ 'No'/*h*/;
            `)
        );
    });

    test('conditional type with parenthesized type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type Flatten<T> = T extends (infer R)[] ? Flatten<R> : T;
            `)
        );
    });

    test('conditional type with parenthesized type and comments', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type Flatten<T> = T extends /*a*/(/*b*/infer/*c*/ R/*d*/)/*e*/[] ? Flatten<R> : T;
            `)
        );
    });

    test('conditional type with parenthesized type and never', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type GetReturnType<T> = T extends (...args: any[]) => infer R ? R : never;
            `)
        );
    });

    test('named tuple member type', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type Coordinate = [x: number, y: number, z?: number];
                type VariableArgs = [name: string, ...args: number[]];
            `)
        );
    });

    test('trailing comma in params', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type RichText = (
                    overrides?: Partial<RichTextField>/*a*/,/*b*/
                ) => RichTextField
            `)
        );
    });

    test('trailing comma in type args', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                export type AfterReadRichTextHookArgs<
                    TValue = any,
                > = {}
            `)
        );
    });

    test('type with empty type argument', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type A/*a*/</*b*/>/*c*/ = {/*d*/}
            `)
        );
    });

    test('type with intrinsic keyword', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type Uppercase<S extends string> = intrinsic
            `)
        );
    });

    test('constructor type with trailing coma', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type ElementConstructor<P> =
                    (new(
                    x: P,
                    y?: any/*a*/,/*b*/
                ) => Component<any, any>);
            `)
        );
    });

    test('constructor type with empty param', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                type ElementConstructor<P> =
                    (new(/*a*/) => Component<any, any>);
            `)
        );
    });
});
