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

describe('label mapping', () => {
    const spec = new RecipeSpec();

    test('with trailing semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function f() {
                    /*1*/label/*2*/:/*3*/console.log("A")/*4*/;
                }
                /*1*/alsoLabel/*2*/:/*3*/console.log("A")/*4*/;
            `)
        ));

    test('without trailing semicolon', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function f() {
                    /*1*/label/*2*/:/*3*/console.log("A")/*4*/
                }
                /*1*/alsoLabel/*2*/:/*3*/console.log("A")/*4*/
            `)
        ));

    test('as control flow body', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                if (enabled) {
                    if (ready) label: a();
                }
                if (a) l1: b() ; else l2: c() ;
                while (a) l3: b();
                for (;;) l4: b();
                for (const x of xs) l5: b();
                for (const x in xs) l6: b();
                do l7: b(); while (a);
            `)
        ));

    test('nested labels', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function f() {
                    outer: inner: console.log("A") ;
                }
                if (a) outer: inner: b();
                outer: inner: console.log("A")
            `)
        ));
});
