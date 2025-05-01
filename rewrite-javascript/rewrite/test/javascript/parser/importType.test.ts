import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('import type mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple import', () => {
        rewriteRun(
          //language=typescript
          typeScript(`type ModuleType = import('fs');`)
        );
    });

    test('simple import with isTypeOf', () => {
        rewriteRun(
            //language=typescript
            typeScript(`type MyType = typeof import("module-name");`)
        );
    });

    test('simple import with isTypeOf and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`type MyType = /*a*/typeof /*b*/ import/*c*/(/*d*/"module-name"/*e*/)/*f*/;`)
        );
    });

    test('import with qualifier', () => {
        rewriteRun(
            //language=typescript
            typeScript(`type ReadStream = import("fs").ReadStream;`)
        );
    });

    test('import with qualifier and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`type ReadStream = import("fs")/*a*/./*b*/ReadStream/*c*/;`)
        );
    });

    test('import with sub qualifiers', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export default class Utils {
                    static Tools = class {
                        static UtilityName = "Helper";
                    };
                }

                // main.ts
                type UtilityNameType = import("./module")/*a*/./*b*/default/*c*/./*d*/Tools/*e*/./*f*/UtilityName;
            `)
        );
    });


    test('function with import type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function useModule(module: import("fs")): void {
                    console.log(module);
                }
            `)
        );
    });

    test('import type with type argument', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type AnotherType = import("module-name").GenericType<string>;
            `)
        );
    });

    test('import type with type argument adv1', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export namespace Shapes {
                    export type Box<T> = { value: T; size: number };
                    export type Circle = { radius: number };
                }

                // main.ts
                type CircleBox = import("./shapes").Shapes.Box<import("./shapes").Shapes.Circle>;
            `)
        );
    });

    test('import type with type argument adv2', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export namespace Models {
                    export type Response<T> = {
                        status: number;
                        data: T;
                    };
                }

                // main.ts
                type UserResponse = import(/*a*/"./library"/*b*/).Models.Response<{ id: string; name: string }>;
            `)
        );
    });

    test('import with attributes', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type A = import("foo", {with: {type: "json"}})
            `)
        );
    });

    test('import with attributes with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type A = import("foo"/*0*/, /*a*/{/*b*/assert/*c*/:/*d*/ {type: "json"}/*e*/}/*f*/)
            `)
        );
    });

    test('import with attributes and qualifiers', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export type LocalInterface =
                    & import("pkg", { with: {"resolution-mode": "require"} }).RequireInterface
                    & import("pkg", { with: {"resolution-mode": "import"} }).ImportInterface;

                export const a = (null as any as import("pkg", { with: {"resolution-mode": "require"} }).RequireInterface);
                export const b = (null as any as import("pkg", { with: {"resolution-mode": "import"} }).ImportInterface);
            `)
        );
    });

    test('import type without qualifier an with type argument', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                declare module "ContextUtils" {
                    export function createContext<Config extends import("tailwindcss").Config>(config: ReturnType<typeof import("tailwindcss/resolveConfig")<Config>>,): import("./types.ts").TailwindContext;
                }
            `)
        );
    });
});
