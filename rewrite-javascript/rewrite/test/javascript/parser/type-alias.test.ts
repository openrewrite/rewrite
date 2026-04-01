// noinspection JSUnusedLocalSymbols,TypeScriptUnresolvedReference

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

describe('type alias mapping', () => {
    const spec = new RecipeSpec();

    test('simple alias', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type StringAlias = string;
             `)
        ));

    test('simple alias with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 /*a*/type /*b*/ StringAlias /*c*/= /*d*/string /*e*/;/*f*/
             `)
        ));

    test('function type alias', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type MyFunctionType = (x: number, y: number) => string;
             `)
        ));

    test('generic function type alias', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Response<T, R, Y> = (x: T, y: R) => Y;
             `)
        ));

    test('generic type alias with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 /*a*/type/*b*/ Response/*c*/</*d*/T/*e*/> /*f*/ = /*g*/(x: T, y: number) => string;
             `)
        ));

    test('union type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type ID = /*a*/ number /*b*/ | /*c*/ string /*d*/;
             `)
        ));

    test('union type with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type ID = number | string;
             `)
        ));

    test('construct function type alias', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type MyConstructor = abstract new (arg: string) => string;
             `)
        ));

    test('construct function type alias with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type MyConstructor = /*a*/new/*b*/ (/*c*/arg: string) => string;
             `)
        ));

    test('construct function type alias with abstract and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type MyConstructor = /*0*/ abstract /*a*/new/*b*/ (/*c*/arg: string) => string;
             `)
        ));

    test('recursive array type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type NestedArray<T> = T | NestedArray<T[]>;
             `)
        ));

    test('construct function type alias with generic', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type GenericConstructor<T> = new (/*a*/.../*b*/args: any[]) => T;
             `)
        ));

    test('tuple type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type MyTuple = [number, string, boolean];
             `)
        ));

    test('tuple type with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type MyTuple = /*a*/[/*b*/number/*c*/, /*d*/string/*e*/, /*f*/boolean/*g*/, /*h*/]/*j*/;
             `)
        ));

    test('tuple type empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type MyTuple = [/*a*/];
             `)
        ));

    test('nested tuple type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type NestedTuple = [number, [string, boolean]];
             `)
        ));

    test('optional tuple type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type OptionalTuple = [string, /*a*/number/*b*/?/*c*/];
             `)
        ));

    test('tuple rest type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type FlexibleTuple = [string, ...number[]];
             `)
        ));

    test('readonly operator tuple type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type ReadonlyTuple = readonly [string, number];
             `)
        ));

    test('readonly operator tuple type with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type ReadonlyTuple = /*a*/keyof /*b*/ [string, number];
             `)
        ));

    test('basic conditional type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type IsString<T> = T extends string ? 'Yes' : 'No';
             `)
        ));

    test('basic conditional type with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type IsString<T> = /*a*/T/*b*/ extends /*c*/string /*d*/? /*e*/'Yes' /*f*/:/*g*/ 'No'/*h*/;
             `)
        ));

    test('conditional type with parenthesized type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Flatten<T> = T extends (infer R)[] ? Flatten<R> : T;
             `)
        ));

    test('conditional type with parenthesized type and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Flatten<T> = T extends /*a*/(/*b*/infer/*c*/ R/*d*/)/*e*/[] ? Flatten<R> : T;
             `)
        ));

    test('conditional type with parenthesized type and never', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type GetReturnType<T> = T extends (...args: any[]) => infer R ? R : never;
             `)
        ));

    test('named tuple member type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Coordinate = [x: number, y: number, z?: number];
                 type VariableArgs = [name: string, ...args: number[]];
             `)
        ));

    test('trailing comma in params', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type RichText = (
                     overrides?: Partial<RichTextField>/*a*/,/*b*/
                 ) => RichTextField
             `)
        ));

    test('trailing comma in type args', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export type AfterReadRichTextHookArgs<
                     TValue = any,
                 > = {}
             `)
        ));

    test('type with empty type argument', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type A/*a*/</*b*/>/*c*/ = {/*d*/}
             `)
        ));

    test('type with intrinsic keyword', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Uppercase<S extends string> = intrinsic
             `)
        ));

    test('constructor type with trailing coma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type ElementConstructor<P> =
                     (new(
                     x: P,
                     y?: any/*a*/,/*b*/
                 ) => Component<any, any>);
             `)
        ));

    test('constructor type with empty param', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                type ElementConstructor<P> =
                    (new(/*a*/) => Component<any, any>);
            `)
        ));
});
