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
import {capture, JavaScriptVisitor, pattern, rewrite, RewriteRule, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('an `in` substituted into the initializer of a for loop', () => {
    const spec = new RecipeSpec();

    function rewriteCalls(rule: RewriteRule) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, method)) ?? method;
            }
        });
    }

    test('is parenthesized where the template replaces a node of the initializer', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`${x}`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`for (let i = foo(a in b); i;) {}`, `for (let i = (a in b); i;) {}`)
        );
    });

    test('is parenthesized where the template writes the loop', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`(() => { for (let i = ${x}; i;) {} })()`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                foo(a in b);
                `,
                `
                (() => {
                    for (let i = (a in b); i; ) {
                    }
                })();
                `
            )
        );
    });

    test('is left alone in the parentheses of a call', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`g(${x})`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`for (let i = foo(a in b); i;) {}`, `for (let i = g(a in b); i;) {}`)
        );
    });

    test('is left alone in the condition', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`${x}`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`for (let i = 0; foo(a in b);) {}`, `for (let i = 0; a in b;) {}`)
        );
    });
});
