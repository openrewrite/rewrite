import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('type alias mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple alias', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type StringAlias = string;
            `)
        );
    });

    test('simple alias with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                /*a*/type /*b*/ StringAlias /*c*/= /*d*/string /*e*/;/*f*/
            `)
        );
    });

    test('function type alias', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type MyFunctionType = (x: number, y: number) => string;
            `)
        );
    });

    test('generic function type alias', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Response<T, R, Y> = (x: T, y: R) => Y;;
            `)
        );
    });

    test('generic type alias with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                /*a*/type/*b*/ Response/*c*/</*d*/T/*e*/> /*f*/ = /*g*/(x: T, y: number) => string;;
            `)
        );
    });

    test('union type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type ID = /*a*/ number /*b*/ | /*c*/ string /*d*/;
            `)
        );
    });

    test('union type with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type ID = number | string;
            `)
        );
    });

    test('construct function type alias', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type MyConstructor = abstract new (arg: string) => string;
            `)
        );
    });

    test('construct function type alias with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type MyConstructor = /*a*/new/*b*/ (/*c*/arg: string) => string;
            `)
        );
    });

    test('construct function type alias with abstract and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type MyConstructor = /*0*/ abstract /*a*/new/*b*/ (/*c*/arg: string) => string;
            `)
        );
    });

    test('recursive array type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type NestedArray<T> = T | NestedArray<T[]>;
            `)
        );
    });

    test('construct function type alias with generic', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type GenericConstructor<T> = new (/*a*/.../*b*/args: any[]) => T;
            `)
        );
    });

    test('tuple type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type MyTuple = [number, string, boolean];
            `)
        );
    });

    test('tuple type with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type MyTuple = /*a*/[/*b*/number/*c*/, /*d*/string/*e*/, /*f*/boolean/*g*/, /*h*/]/*j*/;
            `)
        );
    });

    test('tuple type empty', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type MyTuple = [/*a*/];
            `)
        );
    });

    test('nested tuple type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type NestedTuple = [number, [string, boolean]];
            `)
        );
    });

    test('optional tuple type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type OptionalTuple = [string, /*a*/number/*b*/?/*c*/];
            `)
        );
    });

    test('tuple rest type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type FlexibleTuple = [string, ...number[]];
            `)
        );
    });

    test('readonly operator tuple type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type ReadonlyTuple = readonly [string, number];
            `)
        );
    });

    test('readonly operator tuple type with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type ReadonlyTuple = /*a*/keyof /*b*/ [string, number];
            `)
        );
    });

    test('basic conditional type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type IsString<T> = T extends string ? 'Yes' : 'No';
            `)
        );
    });

    test('basic conditional type with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type IsString<T> = /*a*/T/*b*/ extends /*c*/string /*d*/? /*e*/'Yes' /*f*/:/*g*/ 'No'/*h*/;
            `)
        );
    });

    test('conditional type with parenthesized type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Flatten<T> = T extends (infer R)[] ? Flatten<R> : T;
            `)
        );
    });

    test('conditional type with parenthesized type and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Flatten<T> = T extends /*a*/(/*b*/infer/*c*/ R/*d*/)/*e*/[] ? Flatten<R> : T;
            `)
        );
    });

    test('conditional type with parenthesized type and never', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type GetReturnType<T> = T extends (...args: any[]) => infer R ? R : never;
            `)
        );
    });

    test('named tuple member type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Coordinate = [x: number, y: number, z?: number];
                type VariableArgs = [name: string, ...args: number[]];
            `)
        );
    });

    test('trailing comma in params', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type RichText = (
                    overrides?: Partial<RichTextField>/*a*/,/*b*/
                ) => RichTextField
            `)
        );
    });

    test('trailing comma in type args', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export type AfterReadRichTextHookArgs<
                    TValue = any,
                > = {}
            `)
        );
    });

    test('type with empty type argument', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type A/*a*/</*b*/>/*c*/ = {/*d*/}
            `)
        );
    });

    test('type with intrinsic keyword', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Uppercase<S extends string> = intrinsic
            `)
        );
    });

    test('constructor type with trailing coma', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type ElementConstructor<P> =
                    (new(
                    x: P,
                    y?: any/*a*/,/*b*/
                ) => Component<any, any>);
            `)
        );
    });

    test('constructor type with empty param', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type ElementConstructor<P> =
                    (new(/*a*/) => Component<any, any>);
            `)
        );
    });

});
