/*
 * Copyright 2026 the original author or authors.
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
import {capture, JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('a string put where a statement goes', () => {
    const spec = new RecipeSpec();

    function unwrapFoo() {
        const x = capture('x');
        const rule = rewrite(() => ({before: pattern`foo(${x})`, after: template`${x}`}));
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, method)) ?? method;
            }
        });
    }

    test('is parenthesized where it would start the body as a directive', () => {
        spec.recipe = unwrapFoo();
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function f() {
                    foo("use strict");
                    g();
                }
                `,
                `
                function f() {
                    ("use strict");
                    g();
                }
                `
            )
        );
    });

    test('is parenthesized where it would start the file as a directive', () => {
        spec.recipe = unwrapFoo();
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                foo('use client');
                g();
                `,
                `
                ('use client');
                g();
                `
            )
        );
    });

    test('is left alone after the first statement that is not a string', () => {
        spec.recipe = unwrapFoo();
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function f() {
                    g();
                    foo("use strict");
                }
                `,
                `
                function f() {
                    g();
                    "use strict";
                }
                `
            )
        );
    });
});
