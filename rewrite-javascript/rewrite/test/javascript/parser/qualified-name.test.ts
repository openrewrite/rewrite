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

describe('qualified name mapping', () => {
    const spec = new RecipeSpec();

    test('globalThis qualified name', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const value: globalThis.Number = 1')
        ));

    test('globalThis qualified name with generic', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const value: globalThis.Promise  <    string  > = null')
        ));

    test('globalThis qualified name with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const value /*a123*/ : globalThis. globalThis . /*asda*/ globalThis.Promise<string> = null;')
        ));

    test.skip('nested class qualified name', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               class OuterClass {
                 public static InnerClass = class extends Number { };
               }
               const a: typeof OuterClass.InnerClass.prototype = 1;
           `)
        ));

    test('nested class qualified name', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 class OuterClass {
                     public static InnerClass = class extends Number { };
                 }
                 const a: typeof OuterClass.InnerClass.prototype = 1;
             `)
        ));

    test('namespace qualified name', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               namespace TestNamespace {
                 export class Test {}
               };
               const value: TestNamespace.Test = null;
           `)
        ));

    test('enum qualified name', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                enum Test {
                    A, B
                };

                const val: Test.A = Test.A;
            `)
        ));
});
