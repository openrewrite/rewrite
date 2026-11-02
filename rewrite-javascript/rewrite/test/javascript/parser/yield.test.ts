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
import {fromVisitor, RecipeSpec} from "../../../src/test";

import {JavaScriptVisitor, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('yield mapping', () => {
    const spec = new RecipeSpec();

    test('simple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function* generatorFunction() { yield 42;}')
        ));
    test('empty', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('yield')
        ));
    test('a yield whose value a recipe drops', () => {
        const dropValue = new RecipeSpec();
        dropValue.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitYield(aYield: J.Yield, p: any): Promise<J | undefined> {
                const visited = await super.visitYield(aYield, p) as J.Yield;
                if (!visited.value) {
                    return visited;
                }
                const bare: J.Yield = {
                    kind: J.Kind.Yield,
                    id: visited.id,
                    prefix: visited.prefix,
                    markers: visited.markers,
                    implicit: visited.implicit
                };
                return bare;
            }
        });
        return dropValue.rewriteRun(
            //language=typescript
            typescript('function* g() {\n    yield 42;\n}', 'function* g() {\n    yield;\n}')
        );
    });
    test('delegated', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('yield* other')
        ));

    test('yield expression', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                DenseMatrix.prototype[Symbol.iterator] = function* () {
                    const recurse = function* (value, index) {
                        if (isArray(value)) {
                            for (let i = 0; i < value.length; i++) {
                                yield* recurse(value[i], index.concat(i))
                            }
                        } else {
                            yield ({value, index})
                        }
                    }
                    yield/*a*/* /*b*/recurse(this._data, [])
                }
            `)
        ));
});
