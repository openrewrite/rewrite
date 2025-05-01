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

describe('prefix operator mapping', () => {
    const spec = new RecipeSpec();

    test('plus', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('+1')
        );
    });
    test('minus', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('-1')
        );
    });
    test('not', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('!1')
        );
    });
    test('tilde', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('~1')
        );
    });
    test('increment', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('++1')
        );
    });
    test('decrement', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('--a;')
        );
    });
    test('spread', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('[ ...[] ]')
        );
    });
    test('spread in method param', () => {
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                class Foo {
                    constructor(@multiInject(BAR) /*a*/...args: Bar[][]) {}
                }
            `)
        );
    });
});
