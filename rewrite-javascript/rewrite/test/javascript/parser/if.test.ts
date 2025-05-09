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

describe('if mapping', () => {
    const spec = new RecipeSpec();

    test('simple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('if (true) console.log("foo");')
        ));

    test('simple parse failure', () => {
        expect(() => {
            return spec.rewriteRun(typescript('if'));
        }).rejects.toThrow();
    });

    test('simple with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('/*a*/if /*b*/(/*c*/true/*d*/)/*e*/ console.log("foo")/*f*/;/*g*/')
        ));

    test('braces', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('if (true) /*a*/{/*b*/ console.log("foo")/*c*/; /*d*/}/*e*/')
        ));
    test('else', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('if (true) console.log("foo"); /*a*/ else/*b*/ console.log("bar")/*c*/;/*d*/')
        ));

    test('if-else with semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 if (true) {
                     console.log("foo")/*a*/;/*b*/
                 } else
                     console.log("bar")/*a*/;/*b*/
             `)
        ));

    test('if-else-if with semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                     if (false)
                         console.log("foo")/*b*/;/*c*/
                     else /*d*/if (true)
                         console.log("bar")/*e*/;/*f*/
             `)
        ));

    test('if-if-else with semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                     if (false)
                         /*a*/if (true)
                             console.log("foo")/*b*/;/*c*/
                         else /*d*/if (true)
                             console.log("bar")/*e*/;/*f*/
             `)
        ));

    test('if with for with semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 if (prevProps)
                     for (let name in prevProps) name in nextProps || (node[name] = void 0)/*a*/;/*b*/
             `)
        ));

    test('for with if with return and semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function foo() {
                     for (let opt of el.options)
                         if (opt.selected !== opt.defaultSelected) return !0;
                 }
             `)
        ));

    test('for with if with semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 for(;;)
                     if (true)
                         console.log("foo")/*a*/;/*b*/
             `)
        ));

    test('if with return', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function foo() {
                     if (prevProps)
                         return abs(prevProps)/*a*/;/*b*/
                 }
             `)
        ));

    test('if with break', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 for(;;) {
                     if (!len--) break;
                 }
             `)
        ));

    test('if with continue', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 for(;;) {
                     if (!len--) continue;
                 }
             `)
        ));

    test('if with do', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 if (newStart > newEnd)
                     do {
                         abs(x);
                     } while (oldStart <= oldEnd);
             `)
        ));
});
