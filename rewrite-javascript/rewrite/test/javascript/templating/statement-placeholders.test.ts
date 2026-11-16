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

describe('a statement placeholder followed by another statement on its line', () => {
    const spec = new RecipeSpec();

    function rewriteIfs(rule: RewriteRule) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitIf(iff: J.If, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, iff)) ?? iff;
            }
        });
    }

    test('parses in a template', () => {
        const s = capture('s');
        spec.recipe = rewriteIfs(rewrite(() => ({before: pattern`if (x) ${s}`, after: template`if (y) { ${s} other(); }`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (x) a();
                `,
                `
                if (y) {
                    a();
                    other();
                }
                `
            )
        );
    });

    test('parses in a pattern', () => {
        const s = capture({name: 's', variadic: true});
        const r = capture('r');
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodDeclaration(method: J.MethodDeclaration, p: any): Promise<J | undefined> {
                return (await rewrite(() => ({
                    before: pattern`function f() { ${s} return ${r}; }`,
                    after: template`function g() { ${s} return ${r}; }`
                })).tryOn(this.cursor, method)) ?? method;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function f() {
                    a();
                    b();
                    return 1;
                }
                `,
                `
                function g() {
                    a();
                    b();
                    return 1;
                }
                `
            )
        );
    });

    test('is still an operand where a word operator follows it', () => {
        const x = capture('x');
        spec.recipe = rewriteIfs(rewrite(() => ({before: pattern`if (${x}) a()`, after: template`if (${x} in o) b()`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`if (k) a()`, `if (k in o) b()`)
        );
    });
});
