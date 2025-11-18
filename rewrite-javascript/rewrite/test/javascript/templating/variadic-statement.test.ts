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
import {capture, JavaScriptVisitor, Pattern, pattern, Template, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {produce} from "immer";

describe('variadic statement matching and expansion', () => {
    const spec = new RecipeSpec();

    /**
     * Helper to create a function visitor that matches a pattern and applies a template.
     */
    function matchAndReplaceFunction(pat: Pattern, tmpl: Template) {
        return new class extends JavaScriptVisitor<any> {
            override async visitMethodDeclaration(func: J.MethodDeclaration, p: any): Promise<J | undefined> {
                if (func.body) {
                    const match = await pat.match(func.body);
                    if (match) {
                        const newBody = await tmpl.apply(this.cursor, func.body, match);
                        if (newBody && newBody !== func.body) {
                            return produce(func, draft => {
                                draft.body = newBody as J.Block;
                            });
                        }
                    }
                }
                return func;
            }
        };
     }

    test('match block with zero leading statements using any()', () => {
        const leadingStmts = capture({ variadic: true });
        const pat = pattern`{
            ${leadingStmts}
            return x;
        }`;
        const tmpl = template`{
            ${leadingStmts}
            console.log('returning');
            return x;
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            typescript(
                `function foo(x) {
                    return x;
                }`,
                `
                function foo(x) {
                    console.log('returning');
                    return x;
                }`
            )
        );
    });

    test('match block with one leading statement using any()', () => {
        const leadingStmts = capture({ variadic: true });
        const pat = pattern`{
            ${leadingStmts}
            return x;
        }`;
        const tmpl = template`{
            ${leadingStmts}
            console.log('returning');
            return x;
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            typescript(
                `function foo(x) {
                    console.log('entering');
                    return x;
                }`,
                `
                function foo(x) {
                    console.log('entering');
                    console.log('returning');
                    return x;
                }`
            )
        );
    });

    test('match block with multiple leading statements using any()', () => {
        const leadingStmts = capture({ variadic: true });
        const pat = pattern`{
            ${leadingStmts}
            return x;
        }`;
        const tmpl = template`{
            ${leadingStmts}
            console.log('returning');
            return x;
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            typescript(
                `function foo(x) {
                    const y = x * 2;
                    console.log(y);
                    return x;
                }`,
                `
                function foo(x) {
                    const y = x * 2;
                    console.log(y);
                    console.log('returning');
                    return x;
                }`
            )
        );
    });

    test('match block with trailing statements using any()', () => {
        const trailingStmts = capture({ variadic: true });
        const pat = pattern`{
            console.log('start');
            ${trailingStmts}
        }`;
        const tmpl = template`{
            console.log('start');
            console.log('middle');
            ${trailingStmts}
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            typescript(
                `function foo() {
                    console.log('start');
                    console.log('end');
                }`,
                `
                function foo() {
                    console.log('start');
                    console.log('middle');
                    console.log('end');
                }`
            )
        );
    });

    test('capture and reorder statements', () => {
        const first = capture();
        const second = capture();
        const pat = pattern`{
            ${first}
            ${second}
        }`;
        const tmpl = template`{
            ${second}
            ${first}
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            typescript(
                `function foo() {
                    const a = 1;
                    const b = 2;
                }`,
                `
                function foo() {
                    const b = 2;
                    const a = 1;
                }`
            )
        );
    });

    test('match with variadic min constraint', () => {
        const leadingStmts = capture({ variadic: { min: 1 } });
        const pat = pattern`{
            ${leadingStmts}
            return x;
        }`;
        const tmpl = template`{
            ${leadingStmts}
            console.log('returning');
            return x;
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            // Should NOT match - needs at least 1 leading statement
            typescript(
                `function foo(x) {
                    return x;
                }`
            ),
            // Should match - has 1 leading statement
            typescript(
                `function bar(x) {
                    const y = x;
                    return x;
                }`,
                `
                function bar(x) {
                    const y = x;
                    console.log('returning');
                    return x;
                }`
            )
        );
    });

    test('match with variadic max constraint', () => {
        const leadingStmts = capture({ variadic: { max: 1 } });
        const pat = pattern`{
            ${leadingStmts}
            return x;
        }`;
        const tmpl = template`{
            ${leadingStmts}
            console.log('returning');
            return x;
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            // Should match - zero leading statements
            typescript(
                `function foo(x) {
                    return x;
                }`,
                `
                function foo(x) {
                    console.log('returning');
                    return x;
                }`
            ),
            // Should match - one leading statement
            typescript(
                `function bar(x) {
                    const y = x;
                    return x;
                }`,
                `
                function bar(x) {
                    const y = x;
                    console.log('returning');
                    return x;
                }`
            ),
            // Should NOT match - two leading statements exceeds max
            typescript(
                `function baz(x) {
                    const y = x;
                    const z = y;
                    return x;
                }`
            )
        );
    });

    test('match empty block with variadic capture', () => {
        const stmts = capture({ variadic: true });
        const pat = pattern`{
            ${stmts}
        }`;
        const tmpl = template`{
            console.log('hello');
            ${stmts}
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            typescript(
                `function foo() {
                }`,
                `
                function foo() {
                    console.log('hello');
                }`
            )
        );
    });

    test('capture variadic statements for reuse', () => {
        const stmts = capture({ variadic: true });
        const pat = pattern`{
            try {
                ${stmts}
            } catch (e) {
            }
        }`;
        const tmpl = template`{
            try {
                ${stmts}
            } catch (e) {
                console.error(e);
            }
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            typescript(
                `function foo() {
                    try {
                        doSomething();
                        doMore();
                    } catch (e) {
                    }
                }`,
                `
                function foo() {
                    try {
                        doSomething();
                        doMore();
                    } catch (e) {
                        console.error(e);
                    }
                }`
            )
        );
    });

    test('non-variadic capture should preserve trailing semicolons', () => {
        // Bug report: using capture() (non-variadic) for function bodies loses trailing semicolons
        const body = capture();
        const pat = pattern`{${body}}`;
        const tmpl = template`{
            console.log('before');
            ${body}
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            typescript(
                `function foo() {
                    return 42;
                }`,
                `
                function foo() {
                    console.log('before');
                    return 42;
                }`
            )
        );
    });

    test('variadic capture should preserve trailing semicolons', () => {
        // Variadic captures should also preserve semicolons and formatting
        const body = capture({ variadic: true });
        const pat = pattern`{${body}}`;
        const tmpl = template`{
            console.log('before');
            ${body}
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            typescript(
                `function foo() {
                    return 42;
                }`,
                `
                function foo() {
                    console.log('before');
                    return 42;
                }`
            )
        );
    });

    test('function body capture with wrapper pattern should preserve semicolons', () => {
        // More complex example: extracting function body from wrapper pattern
        const {args, body} = {args: capture(), body: capture({ variadic: true })};
        const pat = pattern`{
            return wrapper(function(${args}) {${body}});
        }`;
        const tmpl = template`{
            function extracted(${args}) {${body}}
            return extracted;
        }`;

        spec.recipe = fromVisitor(matchAndReplaceFunction(pat, tmpl));

        return spec.rewriteRun(
            typescript(
                `function foo() {
                    return wrapper(function(props) {
                        return props.value;
                    });
                }`,
                `
                function foo() {
                    function extracted(props) {
                        return props.value;
                    }
                    return extracted;
                }`
            )
        );
    });
});
