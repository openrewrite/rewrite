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
import { Cursor } from "../../../src";
import {
    any,
    capture,
    JavaScriptParser,
    JavaScriptVisitor,
    pattern,
    rewrite,
    template,
    typescript
} from "../../../src/javascript";
import { Expression, J } from "../../../src/java";
import { fromVisitor, RecipeSpec } from "../../../src/test";

describe('Non-Capturing any() Function', () => {
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

    describe('Basic non-capturing matching', () => {
        test('matches any single argument', async () => {
            const pat = pattern`foo(${any()})`;

            const expr = await parseExpression('foo(42)');
            const match = await pat.match(expr, new Cursor(expr, undefined));

            expect(match).toBeDefined();
            // Verify that no binding was stored (it's anonymous)
            expect(match?.get('anon_1')).toBeUndefined();
        });

        test('matches different expressions without capturing', async () => {
            const pat = pattern`bar(${any()})`;

            // Match with number
            const expr1 = await parseExpression('bar(100)');
            const match1 = await pat.match(expr1, new Cursor(expr1, undefined));
            expect(match1).toBeDefined();

            // Match with string
            const expr2 = await parseExpression('bar("text")');
            const match2 = await pat.match(expr2, new Cursor(expr2, undefined));
            expect(match2).toBeDefined();

            // Match with identifier
            const expr3 = await parseExpression('bar(x)');
            const match3 = await pat.match(expr3, new Cursor(expr3, undefined));
            expect(match3).toBeDefined();
        });

        test('matches in binary expression', async () => {
            const pat = pattern`${any()} + ${any()}`;

            const expr = await parseExpression('10 + 20');
            const match = await pat.match(expr, new Cursor(expr, undefined));

            expect(match).toBeDefined();
        });
    });

    describe('any() with constraints', () => {
        test('matches when constraint returns true', async () => {
            const numericArg = any<J.Literal>({
                constraint: (node) => typeof node.value === 'number' && node.value > 10
            });
            const pat = pattern`process(${numericArg})`;

            const expr = await parseExpression('process(50)');
            const match = await pat.match(expr, new Cursor(expr, undefined));

            expect(match).toBeDefined();
        });

        test('does not match when constraint returns false', async () => {
            const numericArg = any<J.Literal>({
                constraint: (node) => typeof node.value === 'number' && node.value > 10
            });
            const pat = pattern`process(${numericArg})`;

            const expr = await parseExpression('process(5)');
            const match = await pat.match(expr, new Cursor(expr, undefined));

            expect(match).toBeUndefined();
        });

        test('matches string with constraint', async () => {
            const stringArg = any<J.Literal>({
                constraint: (node) => typeof node.value === 'string' && node.value.startsWith('hello')
            });
            const pat = pattern`log(${stringArg})`;

            const expr = await parseExpression('log("hello world")');
            const match = await pat.match(expr, new Cursor(expr, undefined));

            expect(match).toBeDefined();
        });
    });

    describe('Variadic any()', () => {
        test('matches zero arguments', async () => {
            const rest = any({ variadic: true });
            const pat = pattern`foo(${rest})`;

            const expr = await parseExpression('foo()');
            const match = await pat.match(expr, new Cursor(expr, undefined));

            expect(match).toBeDefined();
        });

        test('matches multiple arguments without capturing', async () => {
            const rest = any({ variadic: true });
            const pat = pattern`foo(${rest})`;

            const expr = await parseExpression('foo(1, 2, 3)');
            const match = await pat.match(expr, new Cursor(expr, undefined));

            expect(match).toBeDefined();
            // Verify that no binding was stored
            expect(match?.get('anon_1')).toBeUndefined();
        });

        test('matches variadic with min/max constraints', async () => {
            const rest = any({
                variadic: { min: 1, max: 3 }
            });
            const pat = pattern`bar(${rest})`;

            // Should match: 2 arguments within range
            const expr1 = await parseExpression('bar(1, 2)');
            const match1 = await pat.match(expr1, new Cursor(expr1, undefined));
            expect(match1).toBeDefined();

            // Should not match: 0 arguments (below min)
            const expr2 = await parseExpression('bar()');
            const match2 = await pat.match(expr2, new Cursor(expr2, undefined));
            expect(match2).toBeUndefined();

            // Should not match: 4 arguments (above max)
            const expr3 = await parseExpression('bar(1, 2, 3, 4)');
            const match3 = await pat.match(expr3, new Cursor(expr3, undefined));
            expect(match3).toBeUndefined();
        });
    });

    describe('Mixed captures and any()', () => {
        test('captures important value, ignores others', async () => {
            const important = capture('important');
            const pat = pattern`compute(${any()}, ${important}, ${any()})`;

            const expr = await parseExpression('compute(10, 20, 30)');
            const match = await pat.match(expr, new Cursor(expr, undefined));

            expect(match).toBeDefined();
            // Only the middle value should be captured
            const capturedValue = match?.get('important') as J.Literal;
            expect(capturedValue?.value).toBe(20);

            // First and last arguments should not be captured
            expect(match?.get('anon_1')).toBeUndefined();
            expect(match?.get('anon_2')).toBeUndefined();
        });

        test('first capture, rest any()', async () => {
            const first = capture('first');
            const rest = any({ variadic: true });
            const pat = pattern`foo(${first}, ${rest})`;

            const expr = await parseExpression('foo(1, 2, 3, 4)');
            const match = await pat.match(expr, new Cursor(expr, undefined));

            expect(match).toBeDefined();
            // First should be captured
            const capturedFirst = match?.get('first') as J.Literal;
            expect(capturedFirst?.value).toBe(1);

            // Rest should not be captured
            expect(match?.get('anon_1')).toBeUndefined();
        });
    });

    describe('any() in rewrite rules', () => {
        const spec = new RecipeSpec();

        test('can use any() in pattern without capturing', async () => {
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    return await rewrite(() => ({
                        before: pattern`oldFunc(${any()})`,
                        after: template`newFunc(42)`
                    })).tryOn(this.cursor, method) || super.visitMethodInvocation(method, p);
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(`
                    oldFunc(100)
                `, `
                    newFunc(42)
                `)
            );
        });

        test('mix captures and any() in rewrite', async () => {
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    const value = capture<Expression>('value');
                    return await rewrite(() => ({
                        before: pattern`process(${any<Expression>()}, ${value})`,
                        after: template`process(${value})`
                    })).tryOn(this.cursor, method) || super.visitMethodInvocation(method, p);
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(`
                    process(ignored, important)
                `, `
                    process(important)
                `)
            );
        });
    });

    describe('any() with variadic constraints', () => {
        test('applies constraint to all matched nodes', async () => {
            const numericArgs = any<J.Literal>({
                variadic: true,
                constraint: (nodes) => nodes.every(n => typeof n.value === 'number')
            });
            const pat = pattern`sum(${numericArgs})`;

            // Should match: all numbers
            const expr1 = await parseExpression('sum(1, 2, 3)');
            const match1 = await pat.match(expr1, new Cursor(expr1, undefined));
            expect(match1).toBeDefined();

            // Should not match: contains string
            const expr2 = await parseExpression('sum(1, "text", 3)');
            const match2 = await pat.match(expr2, new Cursor(expr2, undefined));
            expect(match2).toBeUndefined();
        });
    });
});
