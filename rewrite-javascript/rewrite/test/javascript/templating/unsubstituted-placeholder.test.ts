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

describe('a parameter without a value to substitute', () => {
    const spec = new RecipeSpec();

    function rewriteCalls(rule: RewriteRule) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, method)) ?? method;
            }
        });
    }

    test('a capture the pattern does not bind fails the application', () => {
        const x = capture('x');
        const y = capture('y');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${x})`, after: template`bar(${y})`})));
        return expect(spec.rewriteRun(
            //language=typescript
            typescript(`foo(1)`, `bar(1)`)
        )).rejects.toThrow(/capture 'y'\) has no value/);
    });

    test('a variadic capture where no list takes it fails the application', () => {
        const args = capture({name: 'args', variadic: true});
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${args})`, after: template`1 + ${args}`})));
        return expect(spec.rewriteRun(
            //language=typescript
            typescript(`const a = foo(1, 2)`, `const a = 1 + 1, 2`)
        )).rejects.toThrow(/capture 'args'\) has no value/);
    });

    test('a string that is no identifier fails the application', () => {
        const lit = capture('lit');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${lit})`, after: template`bar(${lit.value})`})));
        return expect(spec.rewriteRun(
            //language=typescript
            typescript(`foo('a"b')`, `bar(a"b)`)
        )).rejects.toThrow(/not an identifier/);
    });

    test('a number is substituted as a literal', () => {
        const lit = capture('lit');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${lit})`, after: template`bar(${lit.value})`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(42)`, `bar(42)`)
        );
    });

    test('the length of a variadic capture is substituted as a literal', () => {
        const args = capture({name: 'args', variadic: true});
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${args})`, after: template`bar(${(args as any).length})`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(a, b, c)`, `bar(3)`)
        );
    });

    test('a boolean is substituted as a literal', () => {
        const lit = capture('lit');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${lit})`, after: template`bar(${lit.value})`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(true)`, `bar(true)`)
        );
    });
});
