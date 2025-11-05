/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {any, capture, JavaScriptVisitor, pattern, Pattern, template, Template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('variadic template expansion', () => {
    const spec = new RecipeSpec();

    /**
     * Helper to create a method invocation visitor that matches a pattern and applies a template.
     */
    function matchAndReplace(pat: Pattern, tmpl: Template) {
        return new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const match = await pat.match(method);
                if (match) {
                    return await tmpl.apply(this.cursor, method, match);
                }
                return method;
            }
        };
    }

    test('expand variadic capture - zero arguments', () => {
        const args = capture({ variadic: true });
        const pat = pattern`foo(${args})`;
        const tmpl = template`bar(${args})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript('foo()', 'bar()')
        );
    });

    test('expand variadic capture - single argument', () => {
        const args = capture({ variadic: true });
        const pat = pattern`foo(${args})`;
        const tmpl = template`bar(${args})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript('foo(1)', 'bar(1)')
        );
    });

    test('expand variadic capture - multiple arguments', () => {
        const args = capture({ variadic: true });
        const pat = pattern`foo(${args})`;
        const tmpl = template`bar(${args})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript('foo(1, 2, 3)', 'bar(1, 2, 3)')
        );
    });

    test('expand variadic with fixed arguments before', () => {
        const first = capture();
        const rest = capture({ variadic: true });
        const pat = pattern`foo(${first}, ${rest})`;
        const tmpl = template`bar(${first}, ${rest})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript('foo(1, 2, 3)', 'bar(1, 2, 3)')
        );
    });

    test('expand variadic with fixed arguments after', () => {
        const first = capture({ variadic: true });
        const last = capture();
        const pat = pattern`foo(${first}, ${last})`;
        const tmpl = template`bar(${first}, ${last})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript('foo(1, 2, 3)', 'bar(1, 2, 3)')
        );
    });

    test('match with any() before-middle-after - zero before, zero after', () => {
        const before = any({ variadic: true });
        const middle = capture<J.Identifier>({constraint: node => node.simpleName === 'x'});
        const after = any({ variadic: true });
        const pat = pattern`foo(${before}, ${middle}, ${after})`;
        const tmpl = template`bar(${middle})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript('foo(x)', 'bar(x)')
        );
    });

    test('match with any() before-middle-after - one before, zero after', () => {
        const before = any({ variadic: true });
        const middle = capture<J.Identifier>({constraint: node => node.simpleName === 'x'});
        const after = any({ variadic: true });
        const pat = pattern`foo(${before}, ${middle}, ${after})`;
        const tmpl = template`bar(${middle})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript('foo(a, x)', 'bar(x)')
        );
    });

    test('match with any() before-middle-after - zero before, one after', () => {
        const before = any({ variadic: true });
        const middle = capture<J.Identifier>({constraint: node => node.simpleName === 'x'});
        const after = any({ variadic: true });
        const pat = pattern`foo(${before}, ${middle}, ${after})`;
        const tmpl = template`bar(${middle})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript('foo(x, b)', 'bar(x)')
        );
    });

    test('match with any() before-middle-after - one before, one after', () => {
        const before = any({ variadic: true });
        const middle = capture<J.Identifier>({constraint: node => node.simpleName === 'x'});
        const after = any({ variadic: true });
        const pat = pattern`foo(${before}, ${middle}, ${after})`;
        const tmpl = template`bar(${middle})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript('foo(a, x, b)', 'bar(x)')
        );
    });

    test('variadic followed by literal - should not consume literal', () => {
        const args = capture({ variadic: true });
        const pat = pattern`foo(${args}, 'bar')`;
        const tmpl = template`baz(${args})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript(`foo(1, 2, 'bar')`, `baz(1, 2)`)
        );
    });

    test('variadic followed by literal - no match if literal missing', () => {
        const args = capture({ variadic: true });
        const pat = pattern`foo(${args}, 'bar')`;
        const tmpl = template`baz(${args})`;

        spec.recipe = fromVisitor(matchAndReplace(pat, tmpl));

        return spec.rewriteRun(
            typescript(`foo(1, 2, 'baz')`)  // No change - 'baz' doesn't match 'bar'
        );
    });
});
