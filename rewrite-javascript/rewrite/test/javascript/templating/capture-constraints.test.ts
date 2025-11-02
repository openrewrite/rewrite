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
import {and, capture, not, or, pattern} from "../../../src/javascript";
import {JavaScriptParser} from "../../../src/javascript/parser";
import {J} from "../../../src/java";

describe('Capture Constraints', () => {
    let parser: JavaScriptParser;

    beforeEach(() => {
        parser = new JavaScriptParser();
    });

    async function parseExpression(code: string): Promise<J> {
        const gen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value;
        // @ts-ignore
        const statement = cu.statements[0].element;
        // Handle expression statements
        if (statement.expression) {
            return statement.expression;
        }
        // Return statement itself for other cases
        return statement;
    }

    describe('Simple constraints', () => {
        test('matches when constraint returns true', async () => {
            const value = capture<J.Literal>('value', {
                constraint: (node) => typeof node.value === 'number' && node.value > 100
            });
            const pat = pattern`${value}`;

            const expr = await parseExpression('150');
            const match = await pat.match(expr);

            expect(match).toBeDefined();
            expect(match?.get('value')).toBeDefined();
        });

        test('does not match when constraint returns false', async () => {
            const value = capture<J.Literal>('value', {
                constraint: (node) => typeof node.value === 'number' && node.value > 100
            });
            const pat = pattern`${value}`;

            const expr = await parseExpression('50');
            const match = await pat.match(expr);

            expect(match).toBeUndefined();
        });

        test('matches string literals with constraint', async () => {
            const text = capture<J.Literal>('text', {
                constraint: (node) => typeof node.value === 'string' && node.value.startsWith('hello')
            });
            const pat = pattern`${text}`;

            const expr = await parseExpression('"hello world"');
            const match = await pat.match(expr);

            expect(match).toBeDefined();
            expect((match?.get('text') as J.Literal)?.value).toBe('hello world');
        });

        test('does not match string when constraint fails', async () => {
            const text = capture<J.Literal>('text', {
                constraint: (node) => typeof node.value === 'string' && node.value.startsWith('hello')
            });
            const pat = pattern`${text}`;

            const expr = await parseExpression('"goodbye world"');
            const match = await pat.match(expr);

            expect(match).toBeUndefined();
        });
    });

    describe('and() composition', () => {
        test('matches when all constraints pass', async () => {
            const value = capture<J.Literal>('value', {
                constraint: and(
                    (node) => typeof node.value === 'number',
                    (node) => (node.value as number) > 50,
                    (node) => (node.value as number) < 200,
                    (node) => (node.value as number) % 2 === 0
                )
            });
            const pat = pattern`${value}`;

            const expr = await parseExpression('100');
            const match = await pat.match(expr);

            expect(match).toBeDefined();
            expect((match?.get('value') as J.Literal)?.value).toBe(100);
        });

        test('does not match when any constraint fails', async () => {
            const value = capture<J.Literal>('value', {
                constraint: and(
                    (node) => typeof node.value === 'number',
                    (node) => (node.value as number) > 50,
                    (node) => (node.value as number) < 200,
                    (node) => (node.value as number) % 2 === 0 // This will fail for 99
                )
            });
            const pat = pattern`${value}`;

            const expr = await parseExpression('99');
            const match = await pat.match(expr);

            expect(match).toBeUndefined();
        });
    });

    describe('or() composition', () => {
        test('matches when at least one constraint passes', async () => {
            const value = capture<J.Literal>('value', {
                constraint: or(
                    (node) => typeof node.value === 'string',
                    (node) => typeof node.value === 'number' && node.value > 1000
                )
            });
            const pat = pattern`${value}`;

            // Match with string
            const expr1 = await parseExpression('"text"');
            const match1 = await pat.match(expr1);
            expect(match1).toBeDefined();

            // Match with large number
            const expr2 = await parseExpression('2000');
            const match2 = await pat.match(expr2);
            expect(match2).toBeDefined();
        });

        test('does not match when all constraints fail', async () => {
            const value = capture<J.Literal>('value', {
                constraint: or(
                    (node) => typeof node.value === 'string',
                    (node) => typeof node.value === 'number' && node.value > 1000
                )
            });
            const pat = pattern`${value}`;

            // Small number - both constraints fail
            const expr = await parseExpression('100');
            const match = await pat.match(expr);

            expect(match).toBeUndefined();
        });
    });

    describe('not() composition', () => {
        test('matches when constraint is negated', async () => {
            const value = capture<J.Literal>('value', {
                constraint: not((node) => typeof node.value === 'string')
            });
            const pat = pattern`${value}`;

            // Number should match (not a string)
            const expr = await parseExpression('42');
            const match = await pat.match(expr);

            expect(match).toBeDefined();
        });

        test('does not match when negated constraint would pass', async () => {
            const value = capture<J.Literal>('value', {
                constraint: not((node) => typeof node.value === 'string')
            });
            const pat = pattern`${value}`;

            // String should not match
            const expr = await parseExpression('"text"');
            const match = await pat.match(expr);

            expect(match).toBeUndefined();
        });
    });

    describe('Complex compositions', () => {
        test('combines and, or, not', async () => {
            // Match numbers > 50 that are either even OR > 200
            // But NOT divisible by 10
            const value = capture<J.Literal>('value', {
                constraint: and(
                    (node) => typeof node.value === 'number',
                    (node) => (node.value as number) > 50,
                    or(
                        (node) => (node.value as number) % 2 === 0,
                        (node) => (node.value as number) > 200
                    ),
                    not((node) => (node.value as number) % 10 === 0)
                )
            });
            const pat = pattern`${value}`;

            // 52: > 50, even, not divisible by 10 ✓
            const match1 = await pat.match(await parseExpression('52'));
            expect(match1).toBeDefined();

            // 202: > 50, > 200, even, not divisible by 10 ✓
            const match2 = await pat.match(await parseExpression('202'));
            expect(match2).toBeDefined();

            // 60: > 50, even, but divisible by 10 ✗
            const match3 = await pat.match(await parseExpression('60'));
            expect(match3).toBeUndefined();

            // 45: not > 50 ✗
            const match4 = await pat.match(await parseExpression('45'));
            expect(match4).toBeUndefined();
        });
    });

    describe('Constraints on identifiers', () => {
        test('matches identifier names with constraint', async () => {
            const name = capture<J.Identifier>('name', {
                constraint: (node) => node.simpleName.startsWith('get') && !node.simpleName.includes('_')
            });
            const pat = pattern`${name}`;

            const expr = await parseExpression('getData');
            const match = await pat.match(expr);

            expect(match).toBeDefined();
            expect((match?.get('name') as J.Identifier)?.simpleName).toBe('getData');
        });

        test('does not match identifier when constraint fails', async () => {
            const name = capture<J.Identifier>('name', {
                constraint: (node) => node.simpleName.startsWith('get') && !node.simpleName.includes('_')
            });
            const pat = pattern`${name}`;

            const expr = await parseExpression('get_data');
            const match = await pat.match(expr);

            expect(match).toBeUndefined();
        });
    });

    describe('Constraints in complex patterns', () => {
        test('applies constraint in method invocation', async () => {
            const arg = capture<J.Literal>('arg', {
                constraint: (node) => typeof node.value === 'number' && node.value > 10
            });
            const pat = pattern`foo(${arg})`;

            // Should match
            const expr1 = await parseExpression('foo(20)');
            const match1 = await pat.match(expr1);
            expect(match1).toBeDefined();

            // Should not match
            const expr2 = await parseExpression('foo(5)');
            const match2 = await pat.match(expr2);
            expect(match2).toBeUndefined();
        });

        test('multiple captures with different constraints', async () => {
            const left = capture<J.Literal>('left', {
                constraint: (node) => typeof node.value === 'number' && node.value > 5
            });
            const right = capture<J.Literal>('right', {
                constraint: (node) => typeof node.value === 'number' && node.value < 5
            });
            const pat = pattern`${left} + ${right}`;

            // Should match: 10 + 2 (10 > 5 and 2 < 5)
            const expr1 = await parseExpression('10 + 2');
            const match1 = await pat.match(expr1);
            expect(match1).toBeDefined();

            // Should not match: 3 + 10 (3 not > 5)
            const expr2 = await parseExpression('3 + 10');
            const match2 = await pat.match(expr2);
            expect(match2).toBeUndefined();
        });
    });
});
