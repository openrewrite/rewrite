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
import {JS, typescript} from "../../../src/javascript";
import {J, JavaType} from "../../../src/java";

describe('arithmetic operator mapping', () => {
    const spec = new RecipeSpec();

    test('plus', () => {
        return spec.rewriteRun({
            //language=typescript
            ...typescript(
                '1 + 2'
            ),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const binary = (cu.statements[0].element as JS.ExpressionStatement).expression as J.Binary;
                expect(binary.type).toBe(JavaType.Primitive.Double);
            }
        });
    });

    test('concat', () => {
        return spec.rewriteRun({
            //language=typescript
            ...typescript(
                '"1" + 2'
            ),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const binary = (cu.statements[0].element as JS.ExpressionStatement).expression as J.Binary;
                expect(binary.type).toBe(JavaType.Primitive.String);
            }
        });
    });

    test('minus', () => {
        return spec.rewriteRun(
            //language=typescript
            typescript('1 - 2')
        );
    });

    test('multiply', () => {
        return spec.rewriteRun(
            //language=typescript
            typescript('1 * 2')
        );
    });

    test('divide', () => {
        return spec.rewriteRun(
            //language=typescript
            typescript('1 / 2')
        );
    });

    test('modulo', () => {
        return spec.rewriteRun(
            //language=typescript
            typescript('1 % 2')
        );
    });

    test('left shift', () => {
        return spec.rewriteRun(
            //language=typescript
            typescript('1 << 2')
        );
    });

    test('right shift', () => {
        return spec.rewriteRun(
            //language=typescript
            typescript('1 >> 2')
        );
    });

    test('unsigned right shift', () => {
        return spec.rewriteRun(
            //language=typescript
            typescript('1 >>> 2')
        );
    });

    test('power operation', () => {
        return spec.rewriteRun(
            //language=typescript
            typescript('2 ** 3')
        );
    });

    test('exponentiation operation', () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(`
                let x = 0
                x **= 1
            `)
        );
    });
});
