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
import {capture, JavaScriptParser, JS, pattern} from "../../../src/javascript";

describe('variadic pattern matching against real code', () => {
    const parser = new JavaScriptParser();

    // Helper to parse and get the first statement's element (expression or method invocation)
    async function parseExpr(code: string) {
        const parseGen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await parseGen.next()).value as JS.CompilationUnit;
        return cu.statements[0].element;
    }

    test('variadic capture matches zero arguments', async () => {
        const args = capture('args', { variadic: true });
        const pat = pattern`foo(${args})`;

        const expr = await parseExpr('foo()');

        const result = await pat.match(expr);
        expect(result).toBeDefined();

        // The variadic capture should have captured an empty array
        const captured = result!.get(args);
        expect(captured).toBeDefined();
        expect(Array.isArray(captured)).toBe(true);
        expect((captured as any[]).length).toBe(0);
    });

    test('variadic capture matches single argument', async () => {
        const args = capture('args', { variadic: true });
        const pat = pattern`foo(${args})`;

        const expr = await parseExpr('foo(42)');

        const result = await pat.match(expr);
        expect(result).toBeDefined();

        const captured = result!.get(args);
        expect(Array.isArray(captured)).toBe(true);
        expect((captured as any[]).length).toBe(1);
    });

    test('variadic capture matches multiple arguments', async () => {
        const args = capture('args', { variadic: true });
        const pat = pattern`foo(${args})`;

        const expr = await parseExpr('foo(1, 2, 3)');

        const result = await pat.match(expr);
        expect(result).toBeDefined();

        const captured = result!.get(args);
        expect(Array.isArray(captured)).toBe(true);
        expect((captured as any[]).length).toBe(3);
    });

    test('required first argument + variadic rest', async () => {
        const first = capture('first');
        const rest = capture('rest', { variadic: true });
        const pat = pattern`foo(${first}, ${rest})`;

        // Should NOT match foo() - missing required first
        const result1 = await pat.match(await parseExpr('foo()'));
        expect(result1).toBeUndefined();

        // Should match foo(1) - first=1, rest=[]
        const result2 = await pat.match(await parseExpr('foo(1)'));
        expect(result2).toBeDefined();
        expect(result2!.get(first)).toBeDefined();
        const rest2 = result2!.get(rest);
        expect(Array.isArray(rest2)).toBe(true);
        expect((rest2 as any[]).length).toBe(0);

        // Should match foo(1, 2, 3) - first=1, rest=[2, 3]
        const result3 = await pat.match(await parseExpr('foo(1, 2, 3)'));
        expect(result3).toBeDefined();
        const rest3 = result3!.get(rest);
        expect(Array.isArray(rest3)).toBe(true);
        expect((rest3 as any[]).length).toBe(2);
    });

    test('variadic with min constraint', async () => {
        const args = capture('args', { variadic: { min: 2 } });
        const pat = pattern`foo(${args})`;

        // Should NOT match foo() - min not satisfied
        expect(await pat.match(await parseExpr('foo()'))).toBeUndefined();

        // Should NOT match foo(1) - min not satisfied
        expect(await pat.match(await parseExpr('foo(1)'))).toBeUndefined();

        // Should match foo(1, 2) - exactly min
        expect(await pat.match(await parseExpr('foo(1, 2)'))).toBeDefined();

        // Should match foo(1, 2, 3) - more than min
        expect(await pat.match(await parseExpr('foo(1, 2, 3)'))).toBeDefined();
    });

    test('variadic with max constraint', async () => {
        const args = capture('args', { variadic: { max: 2 } });
        const pat = pattern`foo(${args})`;

        // Should match foo() - within max
        expect(await pat.match(await parseExpr('foo()'))).toBeDefined();

        // Should match foo(1, 2) - exactly max
        expect(await pat.match(await parseExpr('foo(1, 2)'))).toBeDefined();

        // Should NOT match foo(1, 2, 3) - exceeds max
        expect(await pat.match(await parseExpr('foo(1, 2, 3)'))).toBeUndefined();
    });

    test('variadic with min and max constraints', async () => {
        const args = capture('args', { variadic: { min: 1, max: 2 } });
        const pat = pattern`foo(${args})`;

        // Should NOT match foo() - below min
        expect(await pat.match(await parseExpr('foo()'))).toBeUndefined();

        // Should match foo(1) - within range
        expect(await pat.match(await parseExpr('foo(1)'))).toBeDefined();

        // Should match foo(1, 2) - within range
        expect(await pat.match(await parseExpr('foo(1, 2)'))).toBeDefined();

        // Should NOT match foo(1, 2, 3) - exceeds max
        expect(await pat.match(await parseExpr('foo(1, 2, 3)'))).toBeUndefined();
    });

    test('pattern with regular captures and variadic', async () => {
        const first = capture('first');
        const middle = capture('middle', { variadic: true });
        const last = capture('last');
        const pat = pattern`foo(${first}, ${middle}, ${last})`;

        // Should match foo(1, 2) - first=1, middle=[], last=2
        const result1 = await pat.match(await parseExpr('foo(1, 2)'));
        expect(result1).toBeDefined();
        const middle1 = result1!.get(middle);
        expect(Array.isArray(middle1)).toBe(true);
        expect((middle1 as any[]).length).toBe(0);

        // Should match foo(1, 2, 3) - first=1, middle=[2], last=3
        const result2 = await pat.match(await parseExpr('foo(1, 2, 3)'));
        expect(result2).toBeDefined();
        const middle2 = result2!.get(middle);
        expect(Array.isArray(middle2)).toBe(true);
        expect((middle2 as any[]).length).toBe(1);

        // Should match foo(1, 2, 3, 4, 5) - first=1, middle=[2, 3, 4], last=5
        const result3 = await pat.match(await parseExpr('foo(1, 2, 3, 4, 5)'));
        expect(result3).toBeDefined();
        const middle3 = result3!.get(middle);
        expect(Array.isArray(middle3)).toBe(true);
        expect((middle3 as any[]).length).toBe(3);
    });

    test('variadic does not match different method name', async () => {
        const args = capture('args', { variadic: true });
        const pat = pattern`foo(${args})`;

        const result = await pat.match(await parseExpr('bar(1, 2, 3)'));
        expect(result).toBeUndefined();
    });
});
