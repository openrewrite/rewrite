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

describe('a callee substituted into a call', () => {
    const spec = new RecipeSpec();

    function rewriteCalls(rule: RewriteRule) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, method)) ?? method;
            }
        });
    }

    function callWith1() {
        const f = capture('f');
        return rewriteCalls(rewrite(() => ({before: pattern`foo(${f})`, after: template`${f}(1)`})));
    }

    test('an identifier stays the name of the call', () => {
        spec.recipe = callWith1();
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(g)`, `g(1)`)
        );
    });

    test('a member access becomes the select and name of the call', () => {
        spec.recipe = callWith1();
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(a.b)`, `a.b(1)`)
        );
    });

    test('a call is called again', () => {
        spec.recipe = callWith1();
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(a())`, `a()(1)`)
        );
    });

    test('an element access is called', () => {
        spec.recipe = callWith1();
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(a[0])`, `a[0](1)`)
        );
    });

    test('an arrow function is parenthesized', () => {
        spec.recipe = callWith1();
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo((a: number) => a)`, `((a: number) => a)(1)`)
        );
    });

    test('a function expression is parenthesized', () => {
        spec.recipe = callWith1();
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(function (a: number) { return a; })`, `(function (a: number) { return a; })(1)`)
        );
    });

    test('a binary is parenthesized', () => {
        spec.recipe = callWith1();
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(a || b)`, `(a || b)(1)`)
        );
    });

    test('an optional chain is parenthesized, so the call does not join it', () => {
        spec.recipe = callWith1();
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(a?.b)`, `(a?.b)(1)`)
        );
    });

    test('type arguments stay with the call', () => {
        const f = capture('f');
        spec.recipe = rewriteCalls(rewrite(() => ({before: pattern`foo(${f})`, after: template`${f}<string>(1)`})));
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(a.b)`, `a.b<string>(1)`)
        );
    });
});
