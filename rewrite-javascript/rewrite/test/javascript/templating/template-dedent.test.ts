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
import {capture, JavaScriptVisitor, pattern, raw, rewrite, RewriteRule, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('the indentation a template is written with', () => {
    const spec = new RecipeSpec();

    function rewriteCalls(rule: RewriteRule) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, method)) ?? method;
            }
        });
    }

    test('is not taken out of the text of a template literal', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({
            before: pattern`foo(${x})`,
            after: template`bar(${x}, \`a
                b\`)`
        })));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `foo(1);`,
                'bar(1, `a\n                b`);'
            )
        );
    });

    test('is not taken out of a template literal with substitutions', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({
            before: pattern`foo(${x})`,
            after: template`bar(\`\${${x}} {
                \${${x}}\`)`
        })));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `foo(1);`,
                'bar(`${1} {\n                ${1}`);'
            )
        );
    });

    test('is not taken out of raw code', () => {
        spec.recipe = rewriteCalls(rewrite(() => ({
            before: pattern`foo()`,
            after: template`bar(${raw('`x\n  y`')})`
        })));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `foo();`,
                'bar(`x\n  y`);'
            )
        );
    });
});
