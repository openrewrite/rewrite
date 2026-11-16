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

describe('a statement template applied to an expression', () => {
    const spec = new RecipeSpec();

    function rewriteCalls(rule: RewriteRule) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const visited = await super.visitMethodInvocation(method, p) as J;
                return (await rule.tryOn(this.cursor, visited)) ?? visited;
            }
        });
    }

    test('an arrow function body takes the statement in a block', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`if (${x}) a();`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const f = () => foo(1);
                `,
                `
                const f = () => {
                    if (1) a();
                };
                `
            )
        );
    });

    test('an arrow function body takes a return in a block', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`return ${x}`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const f = () => foo(1);
                `,
                `
                const f = () => {
                    return 1
                };
                `
            )
        );
    });

    test('an arrow function body takes several statements as its block', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`a(${x}); b(${x});`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const f = () => foo(1);
                `,
                `
                const f = () => {
                    a(1);
                    b(1);
                };
                `
            )
        );
    });

    test('an argument does not take a statement', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`if (${x}) {}`})));
        return expect(spec.rewriteRun(
            //language=typescript
            typescript(`g(foo(1));`, `g(if (1) {});`)
        )).rejects.toThrow(/Template code is a statement \(J\$If\)/);
    });

    test('an initializer does not take several statements', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`a(${x}); b(${x});`})));
        return expect(spec.rewriteRun(
            //language=typescript
            typescript(`const y = foo(1);`, `const y = {a(1); b(1)};`)
        )).rejects.toThrow(/Template code is a statement \(J\$Block\)/);
    });
});
