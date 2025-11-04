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
import {capture, JavaScriptVisitor, pattern, Pattern, template, Template, typescript} from "../../../src/javascript";
import {Expression, J} from "../../../src/java";

describe('variadic array proxy behavior', () => {
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

    test('access first element with index [0]', () => {
        const args = capture({ variadic: true });
        spec.recipe = fromVisitor(matchAndReplace(pattern`foo(${args})`, template`bar(${args[0]})`));

        return spec.rewriteRun(
            typescript('foo(1, 2, 3)', 'bar(1)')
        );
    });

    test('access second element with index [1]', () => {
        const args = capture({ variadic: true });
        spec.recipe = fromVisitor(matchAndReplace(pattern`foo(${args})`, template`bar(${args[1]})`));

        return spec.rewriteRun(
            typescript('foo(1, 2, 3)', 'bar(2)')
        );
    });

    test('access last element with index [2]', () => {
        const args = capture({ variadic: true });
        spec.recipe = fromVisitor(matchAndReplace(pattern`foo(${args})`, template`bar(${args[2]})`));

        return spec.rewriteRun(
            typescript('foo(1, 2, 3)', 'bar(3)')
        );
    });

    test('slice from index 1', () => {
        const args = capture({ variadic: true });
        spec.recipe = fromVisitor(matchAndReplace(pattern`foo(${args})`, template`bar(${(args.slice(1))})`));

        return spec.rewriteRun(
            typescript('foo(1, 2, 3)', 'bar(2, 3)')
        );
    });

    test('slice with start and end indices', () => {
        const args = capture({ variadic: true });
        spec.recipe = fromVisitor(matchAndReplace(pattern`foo(${args})`, template`bar(${(args.slice(1, 3))})`));

        return spec.rewriteRun(
            typescript('foo(1, 2, 3, 4)', 'bar(2, 3)')
        );
    });

    test('combine first element and slice for rest', () => {
        const args = capture({ variadic: true });
        spec.recipe = fromVisitor(matchAndReplace(pattern`foo(${args})`, template`bar(${args[0]}, ${(args.slice(1))})`));

        return spec.rewriteRun(
            typescript('foo(1, 2, 3)', 'bar(1, 2, 3)')
        );
    });

    test('slice can return empty array', () => {
        const args = capture<Expression>({ variadic: true });
        spec.recipe = fromVisitor(matchAndReplace(pattern`foo(${args})`, template`bar(${(args.slice(10))})`));

        return spec.rewriteRun(
            typescript('foo(1, 2, 3)', 'bar()')
        );
    });

    test('reorder arguments using indices', () => {
        const args = capture<Expression>({ variadic: true });
        spec.recipe = fromVisitor(matchAndReplace(
            pattern`foo(${args})`,
            template`bar(${args[1]}, ${args[0]}, ${args.slice(2)})`)
        );

        return spec.rewriteRun(
            //language=typescript
            typescript('foo(/*0*/ 1, 2, 3)', 'bar(2, /*0*/ 1, 3)')
        );
    });
});
