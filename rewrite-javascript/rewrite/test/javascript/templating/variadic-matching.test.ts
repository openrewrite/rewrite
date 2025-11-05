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

    // Pre-parsed expressions to avoid repeated parsing
    let fixtures: {
        foo0: any;
        foo1: any;
        foo42: any;
        foo2: any;
        foo3: any;
        foo5: any;
        bar3: any;
    };

    beforeAll(async () => {
        const parseExpr = async (code: string) => {
            const parseGen = parser.parse({text: code, sourcePath: 'test.ts'});
            const cu = (await parseGen.next()).value as JS.CompilationUnit;
            return cu.statements[0].element;
        };

        // Parse all commonly used expressions once
        fixtures = {
            foo0: await parseExpr('foo()'),
            foo1: await parseExpr('foo(1)'),
            foo42: await parseExpr('foo(42)'),
            foo2: await parseExpr('foo(1, 2)'),
            foo3: await parseExpr('foo(1, 2, 3)'),
            foo5: await parseExpr('foo(1, 2, 3, 4, 5)'),
            bar3: await parseExpr('bar(1, 2, 3)'),
        };
    });

    test('variadic capture matches 0, 1, or many arguments', async () => {
        const args = capture({ variadic: true });
        const pat = pattern`foo(${args})`;

        // Zero arguments
        const result0 = await pat.match(fixtures.foo0);
        expect(result0).toBeDefined();
        const captured0 = result0!.get(args);
        expect(Array.isArray(captured0)).toBe(true);
        expect((captured0 as any[]).length).toBe(0);

        // Single argument
        const result1 = await pat.match(fixtures.foo42);
        expect(result1).toBeDefined();
        const captured1 = result1!.get(args);
        expect(Array.isArray(captured1)).toBe(true);
        expect((captured1 as any[]).length).toBe(1);

        // Multiple arguments
        const result3 = await pat.match(fixtures.foo3);
        expect(result3).toBeDefined();
        const captured3 = result3!.get(args);
        expect(Array.isArray(captured3)).toBe(true);
        expect((captured3 as any[]).length).toBe(3);

        // Should NOT match different method name
        expect(await pat.match(fixtures.bar3)).toBeUndefined();
    });

    test('required first argument + variadic rest', async () => {
        const first = capture('first');
        const rest = capture({ variadic: true });
        const pat = pattern`foo(${first}, ${rest})`;

        // Should NOT match foo() - missing required first
        expect(await pat.match(fixtures.foo0)).toBeUndefined();

        // Should match foo(1) - first=1, rest=[]
        const result1 = await pat.match(fixtures.foo1);
        expect(result1).toBeDefined();
        expect(result1!.get(first)).toBeDefined();
        const rest1 = result1!.get(rest);
        expect(Array.isArray(rest1)).toBe(true);
        expect((rest1 as any[]).length).toBe(0);

        // Should match foo(1, 2, 3) - first=1, rest=[2, 3]
        const result3 = await pat.match(fixtures.foo3);
        expect(result3).toBeDefined();
        const rest3 = result3!.get(rest);
        expect(Array.isArray(rest3)).toBe(true);
        expect((rest3 as any[]).length).toBe(2);
    });

    test('variadic with min, max, and min+max constraints', async () => {
        // Test 1: min constraint
        const args1 = capture({ variadic: { min: 2 } });
        const pat1 = pattern`foo(${args1})`;

        expect(await pat1.match(fixtures.foo0)).toBeUndefined();  // min not satisfied
        expect(await pat1.match(fixtures.foo1)).toBeUndefined();  // min not satisfied
        expect(await pat1.match(fixtures.foo2)).toBeDefined();    // exactly min
        expect(await pat1.match(fixtures.foo3)).toBeDefined();    // more than min

        // Test 2: max constraint
        const args2 = capture({ variadic: { max: 2 } });
        const pat2 = pattern`foo(${args2})`;

        expect(await pat2.match(fixtures.foo0)).toBeDefined();    // within max
        expect(await pat2.match(fixtures.foo2)).toBeDefined();    // exactly max
        expect(await pat2.match(fixtures.foo3)).toBeUndefined();  // exceeds max

        // Test 3: min and max constraints
        const args3 = capture({ variadic: { min: 1, max: 2 } });
        const pat3 = pattern`foo(${args3})`;

        expect(await pat3.match(fixtures.foo0)).toBeUndefined();  // below min
        expect(await pat3.match(fixtures.foo1)).toBeDefined();    // within range
        expect(await pat3.match(fixtures.foo2)).toBeDefined();    // within range
        expect(await pat3.match(fixtures.foo3)).toBeUndefined();  // exceeds max
    });

    test('pattern with regular captures and variadic', async () => {
        const first = capture('first');
        const middle = capture({ variadic: true });
        const last = capture('last');
        const pat = pattern`foo(${first}, ${middle}, ${last})`;

        // Should match foo(1, 2) - first=1, middle=[], last=2
        const result2 = await pat.match(fixtures.foo2);
        expect(result2).toBeDefined();
        const middle2 = result2!.get(middle);
        expect(Array.isArray(middle2)).toBe(true);
        expect((middle2 as any[]).length).toBe(0);

        // Should match foo(1, 2, 3) - first=1, middle=[2], last=3
        const result3 = await pat.match(fixtures.foo3);
        expect(result3).toBeDefined();
        const middle3 = result3!.get(middle);
        expect(Array.isArray(middle3)).toBe(true);
        expect((middle3 as any[]).length).toBe(1);

        // Should match foo(1, 2, 3, 4, 5) - first=1, middle=[2, 3, 4], last=5
        const result5 = await pat.match(fixtures.foo5);
        expect(result5).toBeDefined();
        const middle5 = result5!.get(middle);
        expect(Array.isArray(middle5)).toBe(true);
        expect((middle5 as any[]).length).toBe(3);
    });
});
