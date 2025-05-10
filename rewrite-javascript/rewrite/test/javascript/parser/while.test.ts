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

describe('while mapping', () => {
    const spec = new RecipeSpec();

    test('empty while', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('while (true);')
        ));

    test('empty while with empty statements', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('while (true/*a*/);/*b*/;/*c*/')
        ));

    test('empty while with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('/*a*/while/*b*/ (/*c*/true/*d*/)/*e*/;/*f*/')
        ));

    test('empty while with empty statement', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('while (true) { };')
        ));

    test('empty while with empty statement and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('/*a*/ while /*b*/(/*c*/true /*d*/)/*e*/ {/*f*/}/*g*/;/*h*/')
        ));

    test('while with statements', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 let count = 0;
                 while (count < 10) {
                     console.log(count);
                     /*count*/
                     count++;
                 };
             `)
        ));

    test('while-if with semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('function foo() { while (i--) if (nodeList[i] == elem) return true;}')
        ));

    test('if-do-while with semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                if (true)
                    do console.log("a")
                    while (true)
                if (true) {
                    do console.log("b")
                    while (true)
                }
            `)
        ));
});
