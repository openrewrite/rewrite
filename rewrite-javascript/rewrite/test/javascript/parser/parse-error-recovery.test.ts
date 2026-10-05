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

describe('parse error recovery', () => {
    const spec = new RecipeSpec();

    test('TS 1320: await on non-promise is not a fatal parse error', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(
                'async function f() {\n' +
                '    const x = { then: 42 };\n' +
                '    await x;\n' +
                '}'
            )
        ));

    test('a grammar error the checker alone reports is not fatal', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function f(...a: number[], b: number) {}')
        ));

    test('a type the visitor does not map keeps its source text', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('type T = string?;')
        ));

    test('an interface property keeps an initializer', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('interface I { x: number = 1; }')
        ));

    test('a constructor keeps a return type annotation', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('class C { constructor(): void {} }')
        ));

    test('an object literal member keeps its modifiers', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const o = { static readonly x: 1 };')
        ));

    test('a shorthand object literal member keeps its modifiers', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const o = { static x };')
        ));

    test('a file keeps its byte order mark', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('\uFEFFconst a = 1;')
        ));

    test('a getter keeps type parameters', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('class C { get x<T>() { return 1; } }')
        ));

    test('a setter keeps type parameters and a return type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('class C { set x<T>(v: any): void {} }')
        ));

    test('a computed getter keeps type parameters', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const k = "a"; class C { get [k]<T>() { return 1; } }')
        ));

    test('a computed setter keeps type parameters and a return type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const k = "a"; class C { set [k]<T>(v: any): void {} }')
        ));

    test('mutually recursive generic types do not cause stack overflow', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(
                'interface A<T> {\n' +
                '    b: B<T>;\n' +
                '}\n' +
                'interface B<T> {\n' +
                '    a: A<T>;\n' +
                '}\n' +
                'const x: A<string> = {} as any;'
            )
        ));

    test('self-referencing parameterized type does not overflow', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(
                'interface Wrapper<T> {\n' +
                '    value: T;\n' +
                '}\n' +
                'interface Self {\n' +
                '    ref: Wrapper<Self>;\n' +
                '}\n' +
                'const x: Wrapper<Self> = {} as any;'
            )
        ));

    test('recursive type alias with parameterized types does not overflow', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(
                'type ConfigExports<T> = Config<T> | Promise<Config<T>> | (() => Config<T>);\n' +
                'interface Config<T = object> {\n' +
                '    extends?: ConfigExports<T>;\n' +
                '    value?: T;\n' +
                '}\n' +
                'const c: Config = { value: {} };'
            )
        ));
});
