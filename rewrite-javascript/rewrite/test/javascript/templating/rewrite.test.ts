/*
 * Copyright 2025 the original author or authors.
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
import {expr, JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('RewriteRule composition', () => {
    const spec = new RecipeSpec();

    describe('andThen', () => {
        test('chains two rules that both match', () => {
            // Rule 1: Swap operands of addition
            const rule1 = rewrite(() => ({
                before: pattern`${expr('a')} + ${expr('b')}`,
                after: template`${expr('b')} + ${expr('a')}`
            }));

            // Rule 2: Change '1 + x' to '2 + x'
            const rule2 = rewrite(() => ({
                before: pattern`1 + ${expr('x')}`,
                after: template`2 + ${expr('x')}`
            }));

            const combined = rule1.andThen(rule2);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript('const a = x + 1', 'const a = 2 + x'),
            );
        });

        test('first rule matches, second does not', () => {
            // Rule 1: Swap operands of addition
            const rule1 = rewrite(() => ({
                before: pattern`${expr('a')} + ${expr('b')}`,
                after: template`${expr('b')} + ${expr('a')}`
            }));

            // Rule 2: Change 'foo + x' to 'bar + x' (will not match after swap)
            const rule2 = rewrite(() => ({
                before: pattern`foo + ${expr('x')}`,
                after: template`bar + ${expr('x')}`
            }));

            const combined = rule1.andThen(rule2);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                // First rule swaps 'x + y' to 'y + x', second rule doesn't match 'foo + ...', result is 'y + x'
                typescript('const a = x + y', 'const a = y + x'),
            );
        });

        test('first rule does not match, returns undefined', () => {
            // Rule 1: Match subtraction
            const rule1 = rewrite(() => ({
                before: pattern`${expr('a')} - ${expr('b')}`,
                after: template`${expr('b')} - ${expr('a')}`
            }));

            // Rule 2: This should never be called
            const rule2 = rewrite(() => ({
                before: pattern`${expr('x')} + ${expr('y')}`,
                after: template`0`
            }));

            const combined = rule1.andThen(rule2);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                // First rule doesn't match addition, so nothing changes
                typescript('const a = x + y'),
            );
        });

        test('chains three rules', () => {
            // Rule 1: Swap operands
            const rule1 = rewrite(() => ({
                before: pattern`${expr('a')} + ${expr('b')}`,
                after: template`${expr('b')} + ${expr('a')}`
            }));

            // Rule 2: Change '1 + x' to '2 + x'
            const rule2 = rewrite(() => ({
                before: pattern`1 + ${expr('x')}`,
                after: template`2 + ${expr('x')}`
            }));

            // Rule 3: Change '2 + x' to '3 + x'
            const rule3 = rewrite(() => ({
                before: pattern`2 + ${expr('x')}`,
                after: template`3 + ${expr('x')}`
            }));

            const combined = rule1.andThen(rule2).andThen(rule3);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                // x + 1 -> 1 + x -> 2 + x -> 3 + x
                typescript('const a = x + 1', 'const a = 3 + x'),
            );
        });

        test('neither rule matches', () => {
            // Rule 1: Match subtraction
            const rule1 = rewrite(() => ({
                before: pattern`${expr('a')} - ${expr('b')}`,
                after: template`${expr('b')} - ${expr('a')}`
            }));

            // Rule 2: Match multiplication
            const rule2 = rewrite(() => ({
                before: pattern`${expr('a')} * ${expr('b')}`,
                after: template`${expr('b')} * ${expr('a')}`
            }));

            const combined = rule1.andThen(rule2);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                // Neither rule matches addition
                typescript('const a = x + y'),
            );
        });
    });

    describe('orElse', () => {
        test('first rule matches, alternative is not tried', () => {
            // Rule 1: Match addition
            const rule1 = rewrite(() => ({
                before: pattern`${expr('a')} + ${expr('b')}`,
                after: template`${expr('b')} + ${expr('a')}`
            }));

            // Rule 2: This should never be called when rule1 matches
            const rule2 = rewrite(() => ({
                before: pattern`${expr('x')} + ${expr('y')}`,
                after: template`0`
            }));

            const combined = rule1.orElse(rule2);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                // First rule matches and swaps operands
                typescript('const a = x + y', 'const a = y + x'),
            );
        });

        test('first rule does not match, alternative matches', () => {
            // Rule 1: Match subtraction
            const rule1 = rewrite(() => ({
                before: pattern`${expr('a')} - ${expr('b')}`,
                after: template`${expr('b')} - ${expr('a')}`
            }));

            // Rule 2: Match addition
            const rule2 = rewrite(() => ({
                before: pattern`${expr('x')} + ${expr('y')}`,
                after: template`${expr('y')} + ${expr('x')}`
            }));

            const combined = rule1.orElse(rule2);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                // First rule doesn't match, second rule matches and swaps
                typescript('const a = x + y', 'const a = y + x'),
            );
        });

        test('neither rule matches', () => {
            // Rule 1: Match subtraction
            const rule1 = rewrite(() => ({
                before: pattern`${expr('a')} - ${expr('b')}`,
                after: template`${expr('b')} - ${expr('a')}`
            }));

            // Rule 2: Match multiplication
            const rule2 = rewrite(() => ({
                before: pattern`${expr('x')} * ${expr('y')}`,
                after: template`${expr('y')} * ${expr('x')}`
            }));

            const combined = rule1.orElse(rule2);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                // Neither rule matches addition
                typescript('const a = x + y'),
            );
        });

        test('specific pattern with general fallback', () => {
            // Specific: Match foo with second argument being 0
            const specific = rewrite(() => ({
                before: pattern`foo(${expr('x')}, 0)`,
                after: template`bar(${expr('x')})`
            }));

            // General: Match foo with any two arguments
            const general = rewrite(() => ({
                before: pattern`foo(${expr('x')}, ${expr('y')})`,
                after: template`baz(${expr('x')}, ${expr('y')})`
            }));

            const combined = specific.orElse(general);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(invocation: J.MethodInvocation, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, invocation) || invocation;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `const a = foo(x, 0);
const b = foo(x, 1);`,
                    `const a = bar(x);
const b = baz(x, 1);`
                ),
            );
        });

        test('chains three rules with orElse', () => {
            // Rule 1: Match subtraction
            const rule1 = rewrite(() => ({
                before: pattern`${expr('a')} - ${expr('b')}`,
                after: template`subtract(${expr('a')}, ${expr('b')})`
            }));

            // Rule 2: Match multiplication
            const rule2 = rewrite(() => ({
                before: pattern`${expr('a')} * ${expr('b')}`,
                after: template`multiply(${expr('a')}, ${expr('b')})`
            }));

            // Rule 3: Match addition
            const rule3 = rewrite(() => ({
                before: pattern`${expr('a')} + ${expr('b')}`,
                after: template`add(${expr('a')}, ${expr('b')})`
            }));

            const combined = rule1.orElse(rule2).orElse(rule3);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `const a = x + y;
const b = x - y;
const c = x * y;`,
                    `const a = add(x, y);
const b = subtract(x, y);
const c = multiply(x, y);`
                ),
            );
        });
    });

    describe('combining andThen and orElse', () => {
        test('orElse followed by andThen', () => {
            // Transform foo(x, 0) to bar(x), then wrap in parens
            const specific = rewrite(() => ({
                before: pattern`foo(${expr('x')}, 0)`,
                after: template`bar(${expr('x')})`
            }));

            // Fallback: transform foo(x, y) to baz(x, y), then wrap in parens
            const general = rewrite(() => ({
                before: pattern`foo(${expr('x')}, ${expr('y')})`,
                after: template`baz(${expr('x')}, ${expr('y')})`
            }));

            // Add parentheses
            const addParens = rewrite(() => ({
                before: pattern`${expr('expr')}`,
                after: template`(${expr('expr')})`
            }));

            const combined = specific.orElse(general).andThen(addParens);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(invocation: J.MethodInvocation, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, invocation) || invocation;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `const a = foo(x, 0);
const b = foo(x, 1);`,
                    `const a = (bar(x));
const b = (baz(x, 1));`
                ),
            );
        });

        test('andThen followed by orElse', () => {
            // Rule 1: Transform subtraction to function call, then try to optimize
            const subToFunc = rewrite(() => ({
                before: pattern`${expr('a')} - ${expr('b')}`,
                after: template`subtract(${expr('a')}, ${expr('b')})`
            }));

            // Optimize: subtract(x, 0) -> x
            const optimizeSub = rewrite(() => ({
                before: pattern`subtract(${expr('x')}, 0)`,
                after: template`${expr('x')}`
            }));

            // Rule 2: Transform addition to function call, then try to optimize
            const addToFunc = rewrite(() => ({
                before: pattern`${expr('a')} + ${expr('b')}`,
                after: template`add(${expr('a')}, ${expr('b')})`
            }));

            // Optimize: add(x, 0) -> x
            const optimizeAdd = rewrite(() => ({
                before: pattern`add(${expr('x')}, 0)`,
                after: template`${expr('x')}`
            }));

            const combined = subToFunc.andThen(optimizeSub).orElse(addToFunc.andThen(optimizeAdd));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `const a = x - 0;
const b = x + 0;
const c = x - 1;
const d = x + 1;`,
                    `const a = x;
const b = x;
const c = subtract(x, 1);
const d = add(x, 1);`
                ),
            );
        });
    });
});
