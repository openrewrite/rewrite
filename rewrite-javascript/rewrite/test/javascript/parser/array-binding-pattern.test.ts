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

describe('array binding pattern', () => {
    const spec = new RecipeSpec();

    test('empty', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const [ ] = [10, 20, 30, 40, 50];
        `)
    ));

    test('simple destructuring', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const [a, b, ...rest] = [10, 20, 30, 40, 50];
        `)
    ));

    test('advanced destructuring', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            const aDefault = "";
            const array = [];
            const /*1*/ [/*2*/a/*3*/, /*4*/b/*5*/, /*6*/] /*7*/=/*8*/ array/*9*/;/*10*/
            const [a1/*12*/, /*11*/,/*3*/ b1] = array;
            const [/*16*/a2 /*14*/=/*15*/ aDefault/*17*/, b2/*18*/] = array;
            const [/*21*/a3, /*22*/b3, /*20*/... /*19*/rest/*23*/] = array;
            const [a4, , b4, ...rest1] = array;
            const [a5, b5, /*26*/.../*25*/{/*27*/ pop/*28*/,/*29*/ push /*30*/, /*31*/}] = array;
            const [a6, b6, /*33*/.../*32*/[/*34*/c, d]/*35*/] = array;
        `)
    ));

    test('destructuring with existing variables', () => spec.rewriteRun(
        //language=typescript
        typescript(`
            let aDefault = 1;
            let array = [];
            let a, b, a1, b1, c, d, rest, pop, push;
            [a, b] = array;
            [a, , b] = array;
            [a = aDefault, b] = array;
            [a, b, ...rest] = array;
            [a, , b, ...rest] = array;
            [a, b, ...{ pop, push }] = array;
            [a, b, ...[c, d]] = array;
        `)
    ));
});
