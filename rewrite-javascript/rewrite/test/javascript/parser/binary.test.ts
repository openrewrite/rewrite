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

describe('arithmetic operator mapping', () => {
    const spec = new RecipeSpec();

    test('plus', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(
            '1 + 2', undefined,
            cu => {
                const binary = <J.Binary>(<JS.ExpressionStatement>cu.statements[0]).expression;
                expect((<JavaType.Primitive>binary.type).kind).toBe(JavaType.PrimitiveKind.Double);
            }
          )
        );
    });
    test('concat', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(
            '"1" + 2', undefined,
            cu => {
                const binary = <J.Binary>(<JS.ExpressionStatement>cu.statements[0]).expression;
                expect((<JavaType.Primitive>binary.type).kind).toBe(JavaType.PrimitiveKind.String);
            }
          )
        );
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
            typescript('x **= 1')
        );
    });
});
