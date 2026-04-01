// noinspection TypeScriptCheckImport,TypeScriptUnresolvedReference

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

describe('import type mapping', () => {
    const spec = new RecipeSpec();

    test('simple import', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`type ModuleType = import('fs');`)
        ));

    test('simple import with isTypeOf', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`type MyType = typeof import("module-name");`)
        ));

    test('simple import with isTypeOf and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`type MyType = /*a*/typeof /*b*/ import/*c*/(/*d*/"module-name"/*e*/)/*f*/;`)
        ));

    test('import with qualifier', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`type ReadStream = import("fs").ReadStream;`)
        ));

    test('import with qualifier and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`type ReadStream = import("fs")/*a*/./*b*/ReadStream/*c*/;`)
        ));

    test('import with sub qualifiers', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export default class Utils {
                     static Tools = class {
                         static UtilityName = "Helper";
                     };
                 }
 
                 // main.ts
                 type UtilityNameType = import("./module")/*a*/./*b*/default/*c*/./*d*/Tools/*e*/./*f*/UtilityName;
             `)
        ));

    test('function with import type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function useModule(module: import("fs")): void {
                     console.log(module);
                 }
             `)
        ));

    test('import type with type argument', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type AnotherType = import("module-name").GenericType<string>;
             `)
        ));

    test('import type with type argument adv1', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export namespace Shapes {
                     export type Box<T> = { value: T; size: number };
                     export type Circle = { radius: number };
                 }
 
                 // main.ts
                 type CircleBox = import("./shapes").Shapes.Box<import("./shapes").Shapes.Circle>;
             `)
        ));

    test('import type with type argument adv2', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export namespace Models {
                     export type Response<T> = {
                         status: number;
                         data: T;
                     };
                 }
 
                 // main.ts
                 type UserResponse = import(/*a*/"./library"/*b*/).Models.Response<{ id: string; name: string }>;
             `)
        ));

    test('import with attributes', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type A = import("foo", {with: {type: "json"}})
             `)
        ));

    test('import with attributes with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type A = import("foo"/*0*/, /*a*/{/*b*/assert/*c*/:/*d*/ {type: "json"}/*e*/}/*f*/)
             `)
        ));

    test('import with attributes and qualifiers', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export type LocalInterface =
                     & import("pkg", { with: {"resolution-mode": "require"} }).RequireInterface
                     & import("pkg", { with: {"resolution-mode": "import"} }).ImportInterface;
 
                 export const a = (null as any as import("pkg", { with: {"resolution-mode": "require"} }).RequireInterface);
                 export const b = (null as any as import("pkg", { with: {"resolution-mode": "import"} }).ImportInterface);
             `)
        ));

    test('import type without qualifier an with type argument', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 declare module "ContextUtils" {
                     export function createContext<Config extends import("tailwindcss").Config>(config: ReturnType<typeof import("tailwindcss/resolveConfig")<Config>>,): import("./types.ts").TailwindContext;
                 }
             `)
        ));
});
