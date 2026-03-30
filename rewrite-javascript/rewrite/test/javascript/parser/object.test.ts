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
import {javascript, typescript} from "../../../src/javascript";

describe('object literal mapping', () => {
    const spec = new RecipeSpec();

    test('empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const c = {}')
        ));

    test('single', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const c = { foo: 1 }')
        ));

    test('duplicate', () =>
        spec.rewriteRun(
            //language=javascript
            javascript('const c = { foo: 1, foo: 2 }')
        ));

    test('multiple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const c = { foo: 1, bar: 2 }')
        ));
    test('trailing comma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const c = { foo: 1, /*1*/ }')
        ));
    test('string key', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const c = { "foo": 1 }')
        ));
    test('undefined key', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const c = { undefined: 1 }')
        ));
    test('computed property', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript(
                'const c = { [ 1 + 1 ] : 1 }'
            ),
            afterRecipe: cu => {
                // const literal = (<J.NewClass>(<J.VariableDeclarations>(<JS.ScopedVariableDeclarations>cu.statements[0]).variables[0]).variables[0].initializer);
                // expect(literal.body).toBeDefined();
                // const computedName = (<J.NewArray>(<JS.PropertyAssignment>literal.body?.statements[0]).name);
                // expect(computedName).toBeDefined();
                // const expression = <J.Binary>computedName.initializer![0];
                // expect(expression).toBeDefined();
                // expect((<J.Literal>expression.left).valueSource).toBe("1");
            }
        }));
});
