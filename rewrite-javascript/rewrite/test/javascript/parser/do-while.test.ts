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

describe('do-while mapping', () => {
    const spec = new RecipeSpec();

    test('empty do-while', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('do {} while (true);')
        ));

    test('empty do-while with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('/*a*/ do /*b*/{/*c*/} /*d*/while/*e*/ (/*f*/true/*g*/)/*h*/;')
        ));

    test('empty do-while with expression and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('/*a*/ do /*b*/{/*c*/} /*d*/while/*e*/ (/*f*/Math/*0*/./*1*/random(/*2*/) /*3*/ > /*4*/0.7/*g*/)/*h*/;')
        ));

    test('do-while with statements', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 let count = 0;
                 do {
                     console.log(count)
                     /*count*/
                     count++;
                 } while (count < 10);
             `)
        ));

    test('do-while with labeled statement and semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 partition: do {
                     break partition
                 } while (from < to)/*a*/;/*b*/
             `)
        ));

    test('do-while statement with semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export function getMarkoRoot(path: t.NodePath<t.Node>) {
                     do curPath = curPath.parentPath/*a*/;/*b*/
                     while (curPath && !isMarko(curPath));
                     return curPath;
                 }
             `)
        ));
});
