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

describe('a pattern and the `?.` or `!` of the code it matches', () => {
    const spec = new RecipeSpec();

    function renameFoo() {
        const x = capture('x');
        const rule = rewrite(() => ({before: pattern`a.foo(${x})`, after: template`a.bar(${x})`}));
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, method)) ?? method;
            }
        });
    }

    test('a pattern without `?.` does not match an optional chain, which the template would drop', () => {
        spec.recipe = renameFoo();
        return spec.rewriteRun(
            //language=typescript
            typescript(`a?.foo(1);`)
        );
    });

    test('a pattern without `!` does not match a non-null assertion, which the template would drop', () => {
        spec.recipe = renameFoo();
        return spec.rewriteRun(
            //language=typescript
            typescript(`a!.foo(1);`)
        );
    });

    test('a pattern matches the code it writes', () => {
        spec.recipe = renameFoo();
        return spec.rewriteRun(
            //language=typescript
            typescript(`a.foo(1);`, `a.bar(1);`)
        );
    });

    test('a pattern matches a node whose own `?.` follows it, which the replacement keeps', () => {
        spec.recipe = renameFoo();
        return spec.rewriteRun(
            //language=typescript
            typescript(`a.foo(1)?.length;`, `a.bar(1)?.length;`)
        );
    });

    test('a capture takes a receiver with its `?.`, which the template then writes', () => {
        const [obj, x] = [capture('obj'), capture('x')];
        const rule = rewrite(() => ({before: pattern`${obj}.foo(${x})`, after: template`${obj}.bar(${x})`}));
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, method)) ?? method;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript(`a?.foo(1);`, `a?.bar(1);`)
        );
    });

    test('a pattern with `?.` matches it', () => {
        const x = capture('x');
        const rule = rewrite(() => ({before: pattern`a?.foo(${x})`, after: template`a?.bar(${x})`}));
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, method)) ?? method;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript(`a?.foo(1);`, `a?.bar(1);`)
        );
    });
});
