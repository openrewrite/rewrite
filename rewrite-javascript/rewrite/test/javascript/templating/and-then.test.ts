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
import {capture, JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('RewriteRule andThen', () => {
    const spec = new RecipeSpec();

    test('chains two rules that both match', () => {
        // Rule 1: Swap operands of addition
        const rule1 = rewrite(() => ({
            before: pattern`${capture('a')} + ${capture('b')}`,
            after: template`${capture('b')} + ${capture('a')}`
        }));

        // Rule 2: Change '1 + x' to '2 + x'
        const rule2 = rewrite(() => ({
            before: pattern`1 + ${capture('x')}`,
            after: template`2 + ${capture('x')}`
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
            before: pattern`${capture('a')} + ${capture('b')}`,
            after: template`${capture('b')} + ${capture('a')}`
        }));

        // Rule 2: Change 'foo + x' to 'bar + x' (will not match after swap)
        const rule2 = rewrite(() => ({
            before: pattern`foo + ${capture('x')}`,
            after: template`bar + ${capture('x')}`
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
            before: pattern`${capture('a')} - ${capture('b')}`,
            after: template`${capture('b')} - ${capture('a')}`
        }));

        // Rule 2: This should never be called
        const rule2 = rewrite(() => ({
            before: pattern`${capture('x')} + ${capture('y')}`,
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
            before: pattern`${capture('a')} + ${capture('b')}`,
            after: template`${capture('b')} + ${capture('a')}`
        }));

        // Rule 2: Change '1 + x' to '2 + x'
        const rule2 = rewrite(() => ({
            before: pattern`1 + ${capture('x')}`,
            after: template`2 + ${capture('x')}`
        }));

        // Rule 3: Change '2 + x' to '3 + x'
        const rule3 = rewrite(() => ({
            before: pattern`2 + ${capture('x')}`,
            after: template`3 + ${capture('x')}`
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
            before: pattern`${capture('a')} - ${capture('b')}`,
            after: template`${capture('b')} - ${capture('a')}`
        }));

        // Rule 2: Match multiplication
        const rule2 = rewrite(() => ({
            before: pattern`${capture('a')} * ${capture('b')}`,
            after: template`${capture('b')} * ${capture('a')}`
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
