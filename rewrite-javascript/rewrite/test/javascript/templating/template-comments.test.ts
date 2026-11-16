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

describe('a comment in a template', () => {
    const spec = new RecipeSpec();

    function rewriteCalls(rule: RewriteRule) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, method)) ?? method;
            }
        });
    }

    test('that opens the code is kept', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`/* TODO: check */ bar(${x})`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`const y = foo(1);`, `const y = /* TODO: check */ bar(1);`)
        );
    });

    test('that opens the code follows a comment of the replaced code', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`/* new */ bar(${x})`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`const y = /* old */ foo(1);`, `const y = /* old */ /* new */ bar(1);`)
        );
    });

    test('that ends the code on its line does not swallow the rest of the template', () => {
        const x = capture('x');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`bar(${x}) // trailing`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`const y = foo(1);`, `const y = bar(1);`)
        );
    });
});
