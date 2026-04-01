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

describe('array literal mapping', () => {
    const spec = new RecipeSpec();

    test('access by index', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const numbers = [10, 20, 30, 40];
                 const v = numbers[2];
           `)
        ));

    test('access by index with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const numbers = [10, 20, 30, 40];
                 const v = /*a*/ numbers/*b*/[/*c*/2/*d*/]/*e*/;/*f*/
             `)
        ));

    test('access by index with !', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const numbers = [10, 20, 30, 40];
                 const v = numbers[2]!;
             `)
        ));

    test('access by index with ! and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const numbers = [10, 20, 30, 40];
                 const v = numbers[2]/*a*/!/*b*/;
             `)
        ));

    test('access by key', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const data = { a: 100, b: 200, c: 300 };
                 const v = /*a*/data/*b*/[/*c*/'a'/*d*/]/*e*/;/*f*/
           `)
        ));

    test('access by key with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const data = { a: 100, b: 200, c: 300 };
                 const v = data['a'];
             `)
        ));

    test('access by key with ?.', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const data = { a: 100, b: 200, c: 300 };
                 const v = data['d'] ?. toString();
           `)
        ));

    test('with optional chaining operator', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 const arr = [10, 20, 30];
                 const value1 = arr/*a*/?./*b*/[1];
           `)
        ));

    test('with optional chaining operator and object access', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const obj = {
                    val: null
                };

                const result2 = obj.val?.[1];
            `)
        ));
});
