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
import {capture, JavaScriptParser, pattern} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('Variadic Capture Constraints', () => {
    let parser: JavaScriptParser;

    beforeEach(() => {
        parser = new JavaScriptParser();
    });

    async function parseExpression(code: string): Promise<J> {
        const gen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value;
        // @ts-ignore
        const statement = cu.statements[0];
        // Handle expression statements
        if (statement.expression) {
            return statement.expression;
        }
        // Return statement itself for other cases
        return statement;
    }

    describe('Array-level validation', () => {
        test('constraint validates entire array', async () => {
            const args = capture<J.Literal>({
                variadic: true,
                constraint: (nodes) => nodes.every(n => typeof n.value === 'number')
            });
            const pat = pattern`sum(${args})`;

            // Should match - all numbers
            const expr1 = await parseExpression('sum(1, 2, 3)');
            const match1 = await pat.match(expr1, undefined!);
            expect(match1).toBeDefined();
            expect((match1?.get(args) as unknown as J.Literal[]).length).toBe(3);

            // Should not match - contains string
            const expr2 = await parseExpression('sum(1, "x", 3)');
            const match2 = await pat.match(expr2, undefined!);
            expect(match2).toBeUndefined();
        });

        test('constraint works with empty array', async () => {
            const args = capture<J.Literal>({
                variadic: true,
                constraint: (nodes) => nodes.length === 0 || nodes.every(n => typeof n.value === 'number')
            });
            const pat = pattern`foo(${args})`;

            // Should match - empty array
            const expr = await parseExpression('foo()');
            const match = await pat.match(expr, undefined!);
            expect(match).toBeDefined();
            expect((match?.get(args) as unknown as J[]).length).toBe(0);
        });

        test('constraint can check array length', async () => {
            const args = capture({
                variadic: true,
                constraint: (nodes) => nodes.length >= 2
            });
            const pat = pattern`process(${args})`;

            // Should match - 3 args
            const expr1 = await parseExpression('process(1, 2, 3)');
            const match1 = await pat.match(expr1, undefined!);
            expect(match1).toBeDefined();

            // Should not match - only 1 arg
            const expr2 = await parseExpression('process(1)');
            const match2 = await pat.match(expr2, undefined!);
            expect(match2).toBeUndefined();

            // Should not match - 0 args
            const expr3 = await parseExpression('process()');
            const match3 = await pat.match(expr3, undefined!);
            expect(match3).toBeUndefined();
        });
    });

    describe('Relationship validation', () => {
        test('constraint can validate relationships between elements', async () => {
            const args = capture<J.Literal>({
                variadic: true,
                constraint: (nodes) => {
                    // All must be numbers and in ascending order
                    if (nodes.length < 2) return true;
                    for (let i = 1; i < nodes.length; i++) {
                        if (typeof nodes[i-1].value !== 'number' || typeof nodes[i].value !== 'number') {
                            return false;
                        }
                        if ((nodes[i-1].value as number) > (nodes[i].value as number)) {
                            return false;
                        }
                    }
                    return true;
                }
            });
            const pat = pattern`sorted(${args})`;

            // Should match - ascending
            const expr1 = await parseExpression('sorted(1, 2, 3)');
            const match1 = await pat.match(expr1, undefined!);
            expect(match1).toBeDefined();

            // Should match - equal values allowed
            const expr2 = await parseExpression('sorted(1, 1, 2)');
            const match2 = await pat.match(expr2, undefined!);
            expect(match2).toBeDefined();

            // Should not match - descending
            const expr3 = await parseExpression('sorted(3, 1, 2)');
            const match3 = await pat.match(expr3, undefined!);
            expect(match3).toBeUndefined();

            // Should not match - contains non-number
            const expr4 = await parseExpression('sorted(1, "x", 3)');
            const match4 = await pat.match(expr4, undefined!);
            expect(match4).toBeUndefined();
        });

        test('constraint can check first/last elements', async () => {
            const args = capture<J.Literal>({
                variadic: true,
                constraint: (nodes) => {
                    // First and last must be numbers
                    if (nodes.length === 0) return true;
                    const first = nodes[0];
                    const last = nodes[nodes.length - 1];
                    return typeof first.value === 'number' && typeof last.value === 'number';
                }
            });
            const pat = pattern`wrap(${args})`;

            // Should match - first and last are numbers
            const expr1 = await parseExpression('wrap(1, "x", 3)');
            const match1 = await pat.match(expr1, undefined!);
            expect(match1).toBeDefined();

            // Should not match - first is string
            const expr2 = await parseExpression('wrap("x", 1, 3)');
            const match2 = await pat.match(expr2, undefined!);
            expect(match2).toBeUndefined();

            // Should not match - last is string
            const expr3 = await parseExpression('wrap(1, 2, "x")');
            const match3 = await pat.match(expr3, undefined!);
            expect(match3).toBeUndefined();
        });
    });

    describe('Combined with min/max', () => {
        test('constraint works together with min/max bounds', async () => {
            const args = capture<J.Literal>({
                variadic: { min: 2, max: 4 },
                constraint: (nodes) => nodes.every(n => typeof n.value === 'number')
            });
            const pat = pattern`range(${args})`;

            // Should match - 3 numeric args
            const expr1 = await parseExpression('range(1, 2, 3)');
            const match1 = await pat.match(expr1, undefined!);
            expect(match1).toBeDefined();

            // Should not match - only 1 arg (below min)
            const expr2 = await parseExpression('range(1)');
            const match2 = await pat.match(expr2, undefined!);
            expect(match2).toBeUndefined();

            // Should not match - 5 args (above max)
            const expr3 = await parseExpression('range(1, 2, 3, 4, 5)');
            const match3 = await pat.match(expr3, undefined!);
            expect(match3).toBeUndefined();

            // Should not match - 3 args but contains string
            const expr4 = await parseExpression('range(1, "x", 3)');
            const match4 = await pat.match(expr4, undefined!);
            expect(match4).toBeUndefined();
        });
    });
});
