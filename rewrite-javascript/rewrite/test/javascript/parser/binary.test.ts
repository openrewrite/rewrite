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

describe('arithmetic operator mapping', () => {
    const spec = new RecipeSpec();

    test('plus', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript(
                '1 + 2'
            ),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const binary = (cu.statements[0] as unknown as JS.ExpressionStatement).expression;
                expect(binary.type).toBe(Type.Primitive.Double);
            }
        }));

    test('concat', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript(
                '"1" + 2'
            ),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const binary = (cu.statements[0] as unknown as JS.ExpressionStatement).expression;
                expect(binary.type).toBe(Type.Primitive.String);
            }
        }));

    test('minus', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('1 - 2')
        ));

    test('multiply', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('1 * 2')
        ));

    test('divide', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('1 / 2')
        ));

    test('modulo', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('1 % 2')
        ));

    test('left shift', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('1 << 2')
        ));

    test('right shift', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('1 >> 2')
        ));

    test('unsigned right shift', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('1 >>> 2')
        ));

    test('power operation', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('2 ** 3')
        ));

    test('exponentiation operation', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                let x = 0
                x **= 1
            `)
        ));
});
