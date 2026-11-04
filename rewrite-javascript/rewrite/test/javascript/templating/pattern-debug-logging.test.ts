import {type MockInstance} from 'vitest';
import {capture, isExpressionStatement, JavaScriptParser, JS, pattern, sourceFileCache} from '../../../src/javascript';
import {J} from '../../../src/java';
import {Cursor} from '../../../src';

describe('Pattern Debug Logging', () => {
    let consoleErrorSpy: MockInstance;
    const parser = new JavaScriptParser({sourceFileCache});

    async function parseExpression(code: string): Promise<J> {
        const gen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[0].element;
        return isExpressionStatement(statement) ? statement.expression : statement;
    }

    beforeEach(() => {
        // Spy on console.error to capture debug output
        consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        // Restore console.error
        consoleErrorSpy.mockRestore();
        // Clear environment variable
        delete process.env.PATTERN_DEBUG;
    });

    test('no debug logging by default', async () => {
        const value = capture('value');
        const pat = pattern`console.log(${value})`;
        const node = await parseExpression('console.log(42)');

        const match = await pat.match(node, undefined!);

        expect(match).toBeDefined();
        expect(consoleErrorSpy).not.toHaveBeenCalled();
    });

    test('call-level debug: { debug: true }', async () => {
        const value = capture('value');
        const pat = pattern`console.log(${value})`;
        const node = await parseExpression('console.log(42)');

        const match = await pat.match(node, undefined!, { debug: true });

        expect(match).toBeDefined();
        expect(consoleErrorSpy).toHaveBeenCalled();

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);
        expect(calls.some(c => c.includes('✅ SUCCESS matching against'))).toBe(true);
        expect(calls.some(c => c.includes("Captures:"))).toBe(true);
        expect(calls.some(c => c.includes("= 🪝value"))).toBe(true);
    });

    test('pattern-level debug: pattern({ debug: true })', async () => {
        const value = capture('value');
        const pat = pattern({ debug: true })`console.log(${value})`;
        const node = await parseExpression('console.log(42)');

        const match = await pat.match(node, undefined!);

        expect(match).toBeDefined();
        expect(consoleErrorSpy).toHaveBeenCalled();

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);
        expect(calls.some(c => c.includes('✅ SUCCESS matching against'))).toBe(true);
    });

    test('global debug: PATTERN_DEBUG=true', async () => {
        process.env.PATTERN_DEBUG = 'true';

        const value = capture('value');
        const pat = pattern`console.log(${value})`;
        const node = await parseExpression('console.log(42)');

        const match = await pat.match(node, undefined!);

        expect(match).toBeDefined();
        expect(consoleErrorSpy).toHaveBeenCalled();

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);
        expect(calls.some(c => c.includes('✅ SUCCESS matching against'))).toBe(true);
    });

    test('precedence: call > pattern > global', async () => {
        process.env.PATTERN_DEBUG = 'true';

        const value = capture('value');
        // Pattern has debug: false, but global is true
        const pat = pattern({ debug: false })`console.log(${value})`;
        const node = await parseExpression('console.log(42)');

        // Call with debug: true overrides pattern debug: false
        const match = await pat.match(node, undefined!, { debug: true });

        expect(match).toBeDefined();
        expect(consoleErrorSpy).toHaveBeenCalled();
    });

    test('explicit debug: false disables when global is true', async () => {
        process.env.PATTERN_DEBUG = 'true';

        const value = capture('value');
        const pat = pattern`console.log(${value})`;
        const node = await parseExpression('console.log(42)');

        // Explicitly disable debug at call level
        const match = await pat.match(node, undefined!, { debug: false });

        expect(match).toBeDefined();
        expect(consoleErrorSpy).not.toHaveBeenCalled();
    });

    test('logs failure with path and explanation', async () => {
        const value = capture('value');
        const pat = pattern`console.log(${value})`;
        // Use console.error instead - should not match
        const node = await parseExpression('console.error(42)');

        const match = await pat.match(node, undefined!, { debug: true });

        expect(match).toBeUndefined();
        expect(consoleErrorSpy).toHaveBeenCalled();

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);
        expect(calls.some(c => c.includes('❌ FAILED matching against'))).toBe(true);
        // At path may or may not appear depending on where mismatch occurred
        expect(calls.some(c => c.includes('Reason:'))).toBe(true);
        expect(calls.some(c => c.includes('Expected:'))).toBe(true);
        expect(calls.some(c => c.includes('Actual:'))).toBe(true);
    });

    test('pattern source includes capture names', async () => {
        const x = capture('x');
        const y = capture('y');
        const pat = pattern({ debug: true })`foo(${x}, ${y})`;
        const node = await parseExpression('foo(1, 2)');

        await pat.match(node, undefined!);

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);
        // First line should show pattern source with ID (captures prefixed with 🪝)
        expect(calls.some(c => c.match(/\[Pattern #\d+\] foo\(\$\{🪝x\}, \$\{🪝y\}\)/))).toBe(true);
    });

    test('details show the marked part of each tree as it is written', async () => {
        const pat = pattern`useState<number>(${capture('x')})`;

        await pat.match(await parseExpression('useState(0)'), undefined!, {debug: true});

        const lines: string[] = consoleErrorSpy.mock.calls.flatMap(c => c[0].split('\n'));
        expect(lines.some(line => line.endsWith('Pattern:  <number>'))).toBe(true);
        expect(lines.some(line => line.endsWith('Matched:  useState(0)'))).toBe(true);
    });

    test('details keep the lines of a marked part where they stand relative to its first', async () => {
        await pattern`foo(${capture('a')})`.match(await parseExpression('foo(1,\n    2)'), undefined!, {debug: true});

        const lines: string[] = consoleErrorSpy.mock.calls.flatMap(c => c[0].split('\n'));
        const first = lines.findIndex(line => line.endsWith('Matched:  (1,'));
        expect(lines[first + 1]).toMatch(/\] {17}2\)$/);
    });

    test('a debug predicate chooses the attempts that print', async () => {
        const seen: boolean[] = [];
        const pat = pattern({
            debug: (_tree, _cursor, attempt) => {
                seen.push(attempt.matched);
                return !attempt.matched;
            }
        })`console.log(${capture('x')})`;

        await pat.match(await parseExpression('console.log(1)'), undefined!);
        expect(consoleErrorSpy).not.toHaveBeenCalled();

        await pat.match(await parseExpression('console.error(1)'), undefined!);
        expect(consoleErrorSpy).toHaveBeenCalled();
        expect(seen).toEqual([true, false]);
    });

    test('heads each attempt with where the code is and where match() was called', async () => {
        const gen = parser.parse({
            text: `foo();
    console.error(42);`,
            sourcePath: 'test.ts'
        });
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[1].element;
        const node = isExpressionStatement(statement) ? statement.expression : statement;

        await pattern`console.log(42)`.match(node, new Cursor(node, new Cursor(cu)), {debug: true});

        const lines: string[] = consoleErrorSpy.mock.calls.flatMap(c => c[0].split('\n'));
        const heading = lines.findIndex(line => line.includes('FAILED matching against'));
        expect(lines[heading]).toMatch(/ at test\.ts:2:5$/);
        expect(lines[heading + 1]).toMatch(/called from \S*pattern-debug-logging\.test\.ts:\d+:\d+/);
    });

    test('an unnamed capture named twice keeps one number', async () => {
        const args = capture({variadic: true});
        const pat = pattern({debug: true})`f(${args}) || g(${args})`;

        await pat.match(await parseExpression('f(a) || g(a)'), undefined!);

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);
        expect(calls).toContain('[Pattern #' + calls[0].match(/#(\d+)/)![1] + '] f(${🪝1}) || g(${🪝1})');
    });

    test('variadic captures show array format', async () => {
        const args = capture({ variadic: true });
        const pat = pattern({ debug: true })`console.log(${args})`;
        const node = await parseExpression('console.log(1, 2, 3)');

        const match = await pat.match(node, undefined!);

        expect(match).toBeDefined();

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);
        // With the new format, variadic captures are shown in the legend
        expect(calls.some(c => c.includes("Captures:"))).toBe(true);
    });

    test('shows matched tree with mismatch highlighted', async () => {
        const x = capture('x');
        const y = capture('y');
        const pat = pattern({ debug: true })`${x} + ${y}`;
        // Pattern expects addition, but we provide subtraction
        const node = await parseExpression('a - b');

        const match = await pat.match(node, undefined!);

        expect(match).toBeUndefined();
        expect(consoleErrorSpy).toHaveBeenCalled();

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);

        // Should show matched tree and mismatch reason
        expect(calls.some(c => c.includes('Matched tree'))).toBe(true);
        expect(calls.some(c => c.includes('Reason:'))).toBe(true);
        expect(calls.some(c => c.includes('Expected:'))).toBe(true);
        expect(calls.some(c => c.includes('Actual:'))).toBe(true);
    });

    test('prints the matched tree without the whitespace before it', async () => {
        const gen = parser.parse({text: 'foo();\n    console.error(42);', sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[1].element;
        const node = isExpressionStatement(statement) ? statement.expression : statement;

        await pattern`console.log(42)`.match(node, undefined!, {debug: true});

        const calls: string[] = consoleErrorSpy.mock.calls.map(c => c[0]);
        const lines = calls.flatMap(c => c.split('\n'));
        const header = lines.findIndex(line => line.includes('Matched tree'));
        expect(lines[header + 1]).toMatch(/\] {7}console\./);
    });
});
