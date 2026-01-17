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
import {capture, JavaScriptParser, JS, pattern} from "../../../src/javascript";

describe('variadic pattern matching against real code', () => {
    const parser = new JavaScriptParser();
    const parseCache = new Map<string, any>();

    async function parseExpr(code: string) {
        // Check cache first
        if (parseCache.has(code)) {
            return parseCache.get(code)!;
        }

        // Parse and cache
        const parseGen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await parseGen.next()).value as JS.CompilationUnit;
        const result = cu.statements[0];
        parseCache.set(code, result);
        return result;
    }

    test('variadic capture matches 0, 1, or many arguments', async () => {
        const args = capture({ variadic: true });
        const pat = pattern`foo(${args})`;

        // Zero arguments
        const result0 = await pat.match(await parseExpr('foo()'), undefined!);
        expect(result0).toBeDefined();
        const captured0 = result0!.get(args);
        expect(Array.isArray(captured0)).toBe(true);
        expect((captured0 as any[]).length).toBe(0);

        // Single argument
        const result1 = await pat.match(await parseExpr('foo(42)'), undefined!);
        expect(result1).toBeDefined();
        const captured1 = result1!.get(args);
        expect(Array.isArray(captured1)).toBe(true);
        expect((captured1 as any[]).length).toBe(1);

        // Multiple arguments
        const result3 = await pat.match(await parseExpr('foo(1, 2, 3)'), undefined!);
        expect(result3).toBeDefined();
        const captured3 = result3!.get(args);
        expect(Array.isArray(captured3)).toBe(true);
        expect((captured3 as any[]).length).toBe(3);

        // Should NOT match different method name
        expect(await pat.match(await parseExpr('bar(1, 2, 3)'), undefined!)).toBeUndefined();
    });

    test('required first argument + variadic rest', async () => {
        const first = capture('first');
        const rest = capture({ variadic: true });
        const pat = pattern`foo(${first}, ${rest})`;

        // Should NOT match foo() - missing required first
        expect(await pat.match(await parseExpr('foo()'), undefined!)).toBeUndefined();

        // Should match foo(1) - first=1, rest=[]
        const result1 = await pat.match(await parseExpr('foo(1)'), undefined!);
        expect(result1).toBeDefined();
        expect(result1!.get(first)).toBeDefined();
        const rest1 = result1!.get(rest);
        expect(Array.isArray(rest1)).toBe(true);
        expect((rest1 as any[]).length).toBe(0);

        // Should match foo(1, 2, 3) - first=1, rest=[2, 3]
        const result3 = await pat.match(await parseExpr('foo(1, 2, 3)'), undefined!);
        expect(result3).toBeDefined();
        const rest3 = result3!.get(rest);
        expect(Array.isArray(rest3)).toBe(true);
        expect((rest3 as any[]).length).toBe(2);
    });

    test('variadic with min, max, and min+max constraints', async () => {
        // Test 1: min constraint
        const args1 = capture({ variadic: { min: 2 } });
        const pat1 = pattern`foo(${args1})`;

        expect(await pat1.match(await parseExpr('foo()'), undefined!)).toBeUndefined();  // min not satisfied
        expect(await pat1.match(await parseExpr('foo(1)'), undefined!)).toBeUndefined();  // min not satisfied
        expect(await pat1.match(await parseExpr('foo(1, 2)'), undefined!)).toBeDefined();    // exactly min
        expect(await pat1.match(await parseExpr('foo(1, 2, 3)'), undefined!)).toBeDefined();    // more than min

        // Test 2: max constraint
        const args2 = capture({ variadic: { max: 2 } });
        const pat2 = pattern`foo(${args2})`;

        expect(await pat2.match(await parseExpr('foo()'), undefined!)).toBeDefined();    // within max
        expect(await pat2.match(await parseExpr('foo(1, 2)'), undefined!)).toBeDefined();    // exactly max
        expect(await pat2.match(await parseExpr('foo(1, 2, 3)'), undefined!)).toBeUndefined();  // exceeds max

        // Test 3: min and max constraints
        const args3 = capture({ variadic: { min: 1, max: 2 } });
        const pat3 = pattern`foo(${args3})`;

        expect(await pat3.match(await parseExpr('foo()'), undefined!)).toBeUndefined();  // below min
        expect(await pat3.match(await parseExpr('foo(1)'), undefined!)).toBeDefined();    // within range
        expect(await pat3.match(await parseExpr('foo(1, 2)'), undefined!)).toBeDefined();    // within range
        expect(await pat3.match(await parseExpr('foo(1, 2, 3)'), undefined!)).toBeUndefined();  // exceeds max
    });

    test('pattern with regular captures and variadic', async () => {
        const first = capture('first');
        const middle = capture({ variadic: true });
        const last = capture('last');
        const pat = pattern`foo(${first}, ${middle}, ${last})`;

        // Should match foo(1, 2) - first=1, middle=[], last=2
        const result2 = await pat.match(await parseExpr('foo(1, 2)'), undefined!);
        expect(result2).toBeDefined();
        const middle2 = result2!.get(middle);
        expect(Array.isArray(middle2)).toBe(true);
        expect((middle2 as any[]).length).toBe(0);

        // Should match foo(1, 2, 3) - first=1, middle=[2], last=3
        const result3 = await pat.match(await parseExpr('foo(1, 2, 3)'), undefined!);
        expect(result3).toBeDefined();
        const middle3 = result3!.get(middle);
        expect(Array.isArray(middle3)).toBe(true);
        expect((middle3 as any[]).length).toBe(1);

        // Should match foo(1, 2, 3, 4, 5) - first=1, middle=[2, 3, 4], last=5
        const result5 = await pat.match(await parseExpr('foo(1, 2, 3, 4, 5)'), undefined!);
        expect(result5).toBeDefined();
        const middle5 = result5!.get(middle);
        expect(Array.isArray(middle5)).toBe(true);
        expect((middle5 as any[]).length).toBe(3);
    });
});
