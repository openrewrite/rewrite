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
import {and, capture, JavaScriptParser, not, or, pattern} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('Capture Constraints', () => {
    let parser: JavaScriptParser;
    const parseCache = new Map<string, J>();

    beforeEach(() => {
        parser = new JavaScriptParser();
    });

    async function parseExpression(code: string): Promise<J> {
        // Check cache first
        if (parseCache.has(code)) {
            return parseCache.get(code)!;
        }

        // Parse and cache
        const gen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value;
        // @ts-ignore
        const statement = cu.statements[0].element;
        const result = statement.expression || statement;
        parseCache.set(code, result);
        return result;
    }

    describe('Simple constraints', () => {
        test('number constraint with both success and failure cases', async () => {
            const value = capture<J.Literal>({
                constraint: (node) => typeof node.value === 'number' && node.value > 100
            });
            const pat = pattern`${value}`;

            // Should match: 150 > 100
            const match1 = await pat.match(await parseExpression('150'));
            expect(match1).toBeDefined();
            expect(match1?.get(value)).toBeDefined();

            // Should not match: 50 <= 100
            const match2 = await pat.match(await parseExpression('50'));
            expect(match2).toBeUndefined();
        });

        test('string constraint with both success and failure cases', async () => {
            const text = capture<J.Literal>({
                constraint: (node) => typeof node.value === 'string' && node.value.startsWith('hello')
            });
            const pat = pattern`${text}`;

            // Should match: starts with "hello"
            const match1 = await pat.match(await parseExpression('"hello world"'));
            expect(match1).toBeDefined();
            expect((match1?.get(text) as J.Literal)?.value).toBe('hello world');

            // Should not match: doesn't start with "hello"
            const match2 = await pat.match(await parseExpression('"goodbye world"'));
            expect(match2).toBeUndefined();
        });
    });

    describe('and() composition', () => {
        test('validates all constraints must pass', async () => {
            const value = capture<J.Literal>({
                constraint: and(
                    (node) => typeof node.value === 'number',
                    (node) => (node.value as number) > 50,
                    (node) => (node.value as number) < 200,
                    (node) => (node.value as number) % 2 === 0
                )
            });
            const pat = pattern`${value}`;

            // Should match: 100 satisfies all constraints
            const match1 = await pat.match(await parseExpression('100'));
            expect(match1).toBeDefined();
            expect((match1?.get(value) as J.Literal)?.value).toBe(100);

            // Should not match: 99 fails even constraint
            const match2 = await pat.match(await parseExpression('99'));
            expect(match2).toBeUndefined();
        });
    });

    describe('or() composition', () => {
        test('validates at least one constraint must pass', async () => {
            const value = capture<J.Literal>({
                constraint: or(
                    (node) => typeof node.value === 'string',
                    (node) => typeof node.value === 'number' && node.value > 1000
                )
            });
            const pat = pattern`${value}`;

            // Should match: is a string
            const match1 = await pat.match(await parseExpression('"text"'));
            expect(match1).toBeDefined();

            // Should match: number > 1000
            const match2 = await pat.match(await parseExpression('2000'));
            expect(match2).toBeDefined();

            // Should not match: number <= 1000 and not a string
            const match3 = await pat.match(await parseExpression('100'));
            expect(match3).toBeUndefined();
        });
    });

    describe('not() composition', () => {
        test('inverts constraint result', async () => {
            const value = capture<J.Literal>({
                constraint: not((node) => typeof node.value === 'string')
            });
            const pat = pattern`${value}`;

            // Should match: number (not a string)
            const match1 = await pat.match(await parseExpression('42'));
            expect(match1).toBeDefined();

            // Should not match: is a string
            const match2 = await pat.match(await parseExpression('"text"'));
            expect(match2).toBeUndefined();
        });
    });

    describe('Complex compositions', () => {
        test('combines and, or, not', async () => {
            // Match numbers > 50 that are either even OR > 200
            // But NOT divisible by 10
            const value = capture<J.Literal>({
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

            // Parse test cases inline (only used once each)
            const expr52 = await parseExpression('52');
            const expr202 = await parseExpression('202');
            const expr60 = await parseExpression('60');
            const expr45 = await parseExpression('45');

            // 52: > 50, even, not divisible by 10 ✓
            expect(await pat.match(expr52)).toBeDefined();

            // 202: > 50, > 200, even, not divisible by 10 ✓
            expect(await pat.match(expr202)).toBeDefined();

            // 60: > 50, even, but divisible by 10 ✗
            expect(await pat.match(expr60)).toBeUndefined();

            // 45: not > 50 ✗
            expect(await pat.match(expr45)).toBeUndefined();
        });
    });

    describe('Constraints on identifiers', () => {
        test('validates identifier names with both success and failure', async () => {
            const name = capture<J.Identifier>({
                constraint: (node) => node.simpleName.startsWith('get') && !node.simpleName.includes('_')
            });
            const pat = pattern`${name}`;

            // Should match: starts with 'get' and no underscore
            const match1 = await pat.match(await parseExpression('getData'));
            expect(match1).toBeDefined();
            expect((match1?.get(name) as J.Identifier)?.simpleName).toBe('getData');

            // Should not match: contains underscore
            const match2 = await pat.match(await parseExpression('get_data'));
            expect(match2).toBeUndefined();
        });
    });

    describe('Constraints in complex patterns', () => {
        test('applies constraint in method invocation', async () => {
            const arg = capture<J.Literal>({
                constraint: (node) => typeof node.value === 'number' && node.value > 10
            });
            const pat = pattern`foo(${arg})`;

            // Should match: 20 > 10
            const match1 = await pat.match(await parseExpression('foo(20)'));
            expect(match1).toBeDefined();

            // Should not match: 5 <= 10
            const match2 = await pat.match(await parseExpression('foo(5)'));
            expect(match2).toBeUndefined();
        });

        test('multiple captures with different constraints', async () => {
            const left = capture<J.Literal>({
                constraint: (node) => typeof node.value === 'number' && node.value > 5
            });
            const right = capture<J.Literal>({
                constraint: (node) => typeof node.value === 'number' && node.value < 5
            });
            const pat = pattern`${left} + ${right}`;

            // Should match: 10 > 5 and 2 < 5
            const match1 = await pat.match(await parseExpression('10 + 2'));
            expect(match1).toBeDefined();

            // Should not match: 3 <= 5
            const match2 = await pat.match(await parseExpression('3 + 10'));
            expect(match2).toBeUndefined();
        });
    });

    describe('Context-aware constraints with cursor', () => {
        test('constraint with cursor parameter can access context', async () => {
            // Test that cursor parameter allows navigation up the tree
            let constraintCalled = false;
            let parentIsBinary = false;

            const left = capture<J.Literal>({
                constraint: (node, cursor) => {
                    constraintCalled = true;
                    // Check that cursor can navigate to parent
                    const parent = cursor?.parent?.value;
                    if (parent && parent.kind === J.Kind.Binary) {
                        parentIsBinary = true;
                    }
                    return typeof node.value === 'number';
                }
            });
            const pat = pattern`${left} + 20`;

            // Match against a binary expression
            const match = await pat.match(await parseExpression('10 + 20') as J.Binary);

            expect(match).toBeDefined();
            expect(constraintCalled).toBe(true);
            // Verify cursor can navigate to parent Binary node
            expect(parentIsBinary).toBe(true);
        });

        test('cursor behavior with composition functions', async () => {
            // Test 1: Cursor at ast root when not explicitly provided
            let cursorReceived: any = 'not-called';
            let astNode: J | undefined;
            const value1 = capture<J.Literal>({
                constraint: (node, cursor) => {
                    cursorReceived = cursor;
                    astNode = node;
                    return typeof node.value === 'number';
                }
            });
            const pat1 = pattern`${value1}`;

            const match1 = await pat1.match(await parseExpression('42'));
            expect(match1).toBeDefined();
            expect(cursorReceived).toBeDefined();
            expect(cursorReceived.value).toBe(astNode); // Cursor positioned at captured node
            expect(cursorReceived.parent?.value).toBe(await parseExpression('42'));
            expect(cursorReceived.parent?.parent).toBeUndefined(); // Root has no parent

            // Test 2: Composition functions forward cursor
            let cursorReceivedInAnd: any = 'not-called';
            const value2 = capture<J.Literal>({
                constraint: and(
                    (node) => typeof node.value === 'number',
                    (node, cursor) => {
                        cursorReceivedInAnd = cursor;
                        return (node.value as number) > 10;
                    }
                )
            });
            const pat2 = pattern`${value2}`;

            const match2 = await pat2.match(await parseExpression('50'));
            expect(match2).toBeDefined();
            expect(cursorReceivedInAnd).toBeDefined();
            expect(cursorReceivedInAnd.parent?.value).toBe(await parseExpression('50'));

            // Test 3: or composition with cursor-aware constraints
            const value3 = capture<J.Literal>({
                constraint: or(
                    (node) => typeof node.value === 'string',
                    (node, cursor) => {
                        // Accept numbers only if cursor has a grandparent (i.e., not at root)
                        return cursor?.parent?.parent !== undefined && typeof node.value === 'number';
                    }
                )
            });
            const pat3 = pattern`${value3}`;

            // String should match regardless
            const match3a = await pat3.match(await parseExpression('"text"'));
            expect(match3a).toBeDefined();

            // Number at root should not match (second constraint needs grandparent in cursor chain)
            const match3b = await pat3.match(await parseExpression('42'));
            expect(match3b).toBeUndefined();

            // Test 4: not composition with cursor-aware constraint
            const value4 = capture<J.Literal>({
                constraint: not((node, cursor) => {
                    // Reject if cursor has a grandparent (i.e., not at root)
                    return cursor?.parent?.parent !== undefined;
                })
            });
            const pat4 = pattern`${value4}`;

            // At root level, constraint should pass (not(false) = true), so match succeeds
            const match4 = await pat4.match(await parseExpression('42'));
            expect(match4).toBeDefined();
        });
    });

    describe('Cursor positioning verification', () => {
        test('cursor is positioned at captured node, not root', async () => {
            // Create a pattern that captures the left operand
            let capturedNode: J | undefined;
            let cursorValue: J | undefined;

            const left = capture<J.Literal>({
                constraint: (node, cursor) => {
                    capturedNode = node;
                    cursorValue = cursor?.value as J;
                    return true;
                }
            });
            const pat = pattern`${left} + 20`;

            // Match against the expression
            const match = await pat.match(await parseExpression('10 + 20') as J.Binary);

            expect(match).toBeDefined();
            expect(capturedNode).toBeDefined();
            expect(cursorValue).toBeDefined();

            // Verify the cursor is positioned at the captured node itself
            expect(cursorValue).toBe(capturedNode);

            // Verify the captured node is the left operand (10)
            expect((capturedNode as J.Literal)?.value).toBe(10);
        });

        test('cursor parent provides access to containing expression', async () => {
            let capturedArg: J | undefined;
            let parentKind: typeof J.Kind | undefined;

            const arg = capture<J.Literal>({
                constraint: (node, cursor) => {
                    capturedArg = node;
                    // The cursor's parent should be at a higher level in the tree
                    const parent = cursor?.parent;
                    if (parent) {
                        parentKind = parent.value?.kind;
                    }
                    return true;
                }
            });
            const pat = pattern`foo(${arg})`;

            const match = await pat.match(await parseExpression('foo(42)') as J.MethodInvocation);

            expect(match).toBeDefined();
            expect(capturedArg).toBeDefined();
            expect((capturedArg as J.Literal)?.value).toBe(42);

            // The parent should be something higher up (not the literal itself)
            // This verifies the cursor is at the captured node, not elsewhere
            expect(parentKind).toBeDefined();
            expect(parentKind).not.toBe(J.Kind.Literal);
        });
    });
});
