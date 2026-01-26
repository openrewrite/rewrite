import {capture, isExpressionStatement, JavaScriptParser, JS, pattern} from '../../../src/javascript';
import {J} from '../../../src/java';

describe('Pattern Debug Logging', () => {
    let consoleErrorSpy: jest.SpyInstance;
    let parser: JavaScriptParser;

    async function parseExpression(code: string): Promise<J> {
        const gen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[0];
        return isExpressionStatement(statement) ? statement.expression : statement;
    }

    beforeEach(() => {
        parser = new JavaScriptParser();
        // Spy on console.error to capture debug output
        consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
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
        expect(calls.some(c => c.includes("Captured 'value'"))).toBe(true);
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
        calls.forEach((call, i) => console.log(`${i}: ${call}`));
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
        // First line should show pattern source with ID
        expect(calls.some(c => c.match(/\[Pattern #\d+\] foo\(\$\{x\}, \$\{y\}\)/))).toBe(true);
    });

    test('variadic captures show array format', async () => {
        const args = capture({ variadic: true });
        const pat = pattern({ debug: true })`console.log(${args})`;
        const node = await parseExpression('console.log(1, 2, 3)');

        const match = await pat.match(node, undefined!);

        expect(match).toBeDefined();

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);
        const capturedLine = calls.find(c => c.includes("Captured"));
        expect(capturedLine).toContain('[');
        expect(capturedLine).toContain(']');
    });

    test('shows path for nested mismatch', async () => {
        const x = capture('x');
        const y = capture('y');
        const pat = pattern({ debug: true })`${x} + ${y}`;
        // Pattern expects addition, but we provide subtraction
        const node = await parseExpression('a - b');

        const match = await pat.match(node, undefined!);

        expect(match).toBeUndefined();
        expect(consoleErrorSpy).toHaveBeenCalled();

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);
        calls.forEach((call, i) => console.log(`${i}: ${call}`));

        // Should show path and operator mismatch
        expect(calls.some(c => c.includes('At path:'))).toBe(true);
        expect(calls.some(c => c.includes('Reason:'))).toBe(true);
    });
});
