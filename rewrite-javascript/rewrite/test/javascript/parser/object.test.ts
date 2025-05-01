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

describe('object literal mapping', () => {
    const spec = new RecipeSpec();

    test('empty', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('const c = {}')
        );
    });

    test('single', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('const c = { foo: 1 }')
        );
    });

    test('multiple', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('const c = { foo: 1, bar: 2 }')
        );
    });
    test('trailing comma', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('const c = { foo: 1, /*1*/ }')
        );
    });
    test('string key', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('const c = { "foo": 1 }')
        );
    });
    test('undefined key', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('const c = { undefined: 1 }')
        );
    });
    test('computed property', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(
            'const c = { [ 1 + 1 ] : 1 }', undefined,
            cu => {
                const literal = (<J.NewClass>(<J.VariableDeclarations>(<JS.ScopedVariableDeclarations>cu.statements[0]).variables[0]).variables[0].initializer);
                expect(literal.body).toBeDefined();
                const computedName = (<J.NewArray>(<JS.PropertyAssignment>literal.body?.statements[0]).name);
                expect(computedName).toBeDefined();
                const expression = <J.Binary>computedName.initializer![0];
                expect(expression).toBeDefined();
                expect((<J.Literal>expression.left).valueSource).toBe("1");
            }
          )
        );
    });
});
