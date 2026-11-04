import {type MockInstance} from 'vitest';
import {
    capture,
    isExpressionStatement,
    JavaScriptParser,
    JS,
    pattern,
    rewrite,
    sourceFileCache,
    template
} from '../../../src/javascript';
import {J} from '../../../src/java';
import {Cursor} from '../../../src';
import {shouldUseEmoji} from '../../../src/javascript/templating/debug';

describe('Pattern Debug Logging', () => {
    let consoleErrorSpy: MockInstance;
    const parser = new JavaScriptParser({sourceFileCache});

    async function parseExpression(code: string): Promise<J> {
        const gen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[cu.statements.length - 1].element;
        return isExpressionStatement(statement) ? statement.expression : statement;
    }

    /** The lines of everything printed so far. */
    function printed(): string[] {
        return consoleErrorSpy.mock.calls.flatMap(c => c[0].split('\n'));
    }

    // The 👉…👈 markers, rather than whatever the terminal running the tests supports
    let noColor: string | undefined;

    beforeEach(() => {
        // Spy on console.error to capture debug output
        consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
        noColor = process.env.NO_COLOR;
        process.env.NO_COLOR = '1';
    });

    afterEach(() => {
        // Restore console.error
        consoleErrorSpy.mockRestore();
        // Clear environment variable
        delete process.env.PATTERN_DEBUG;
        if (noColor === undefined) {
            delete process.env.NO_COLOR;
        } else {
            process.env.NO_COLOR = noColor;
        }
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
        expect(calls.some(c => c.includes("🪝value = 42"))).toBe(true);
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

    test('the legend names each capture with the code it holds', async () => {
        const pat = pattern({debug: true})`JSON.stringify(${capture('value')}, ${capture({variadic: true})})`;

        await pat.match(await parseExpression('JSON.stringify(user, null, 2)'), undefined!);

        const lines: string[] = consoleErrorSpy.mock.calls.flatMap(c => c[0].split('\n'));
        expect(lines.some(line => line.endsWith('JSON.stringify(ⒶuserⒶ, ⒷnullⒷ, Ⓑ2Ⓑ)'))).toBe(true);
        expect(lines.some(line => line.endsWith('Ⓐ 🪝value = user'))).toBe(true);
        expect(lines.some(line => line.endsWith('Ⓑ 🪝1 = null, 2'))).toBe(true);
    });

    test('an argument count mismatch suggests a variadic capture', async () => {
        await pattern`foo(${capture('a')})`.match(await parseExpression('foo(1, 2)'), undefined!, {debug: true});

        const lines: string[] = consoleErrorSpy.mock.calls.flatMap(c => c[0].split('\n'));
        expect(lines.some(line => line.includes('Note:') && line.includes('capture({variadic: true})'))).toBe(true);
    });

    test('NO_COLOR asks for markers, and FORCE_COLOR for colours even then', () => {
        const names = ['CLAUDECODE', 'NO_COLOR', 'FORCE_COLOR'];
        const saved = Object.fromEntries(names.map(name => [name, process.env[name]]));
        try {
            delete process.env.CLAUDECODE;
            delete process.env.FORCE_COLOR;
            process.env.NO_COLOR = '1';
            expect(shouldUseEmoji()).toBe(true);

            process.env.FORCE_COLOR = '1';
            expect(shouldUseEmoji()).toBe(false);
        } finally {
            for (const [name, value] of Object.entries(saved)) {
                if (value === undefined) {
                    delete process.env[name];
                } else {
                    process.env[name] = value;
                }
            }
        }
    });

    test('an operator mismatch marks just the operators', async () => {
        const pat = pattern`${capture('x')} + ${capture('y')}`;

        await pat.match(await parseExpression('a - b'), undefined!, {debug: true});

        expect(printed().some(line => line.endsWith('🪝x 👉+👈 🪝y'))).toBe(true);
        expect(printed().some(line => line.endsWith('a 👉-👈 b'))).toBe(true);
    });

    test('a piece only one side has is marked, and where it would go on the other side', async () => {
        const typed = pattern`useState<number>(${capture('x')})`;
        await typed.match(await parseExpression('useState(0)'), undefined!, {debug: true});

        expect(printed().some(line => line.endsWith('useState👉<number>👈(🪝x)'))).toBe(true);
        expect(printed().some(line => line.endsWith('useState👉👈(0)'))).toBe(true);

        consoleErrorSpy.mockClear();
        const arrow = pattern`(${capture('x')}) => ${capture('y')}`;
        await arrow.match(await parseExpression('async (a) => a'), undefined!, {debug: true});

        expect(printed().some(line => line.endsWith('👉👈(🪝x) => 🪝y'))).toBe(true);
        expect(printed().some(line => line.endsWith('👉async👈 (a) => a'))).toBe(true);
    });

    test('a long tree shows the lines around the mismatch', async () => {
        const pat = pattern`class ${capture('name')} extends React.Component {}`;
        const gen = parser.parse({
            text: `
            class Big extends Component {
                render() {
                    const a = 1;
                    const b = 2;
                    return a + b;
                }
            }`,
            sourcePath: 'test.ts'
        });
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const node = cu.statements[0].element;

        await pat.match(node, new Cursor(node, new Cursor(cu)), {debug: true});

        const lines = printed();
        const source = lines.slice(lines.findIndex(line => line.endsWith('Source tree:')) + 1);
        const tree = source.slice(0, source.findIndex(line => line.endsWith(']')));
        expect(tree.map(line => line.replace(/^\[Pattern #\d+] {7}/, ''))).toEqual([
            'class Big extends 👉Component👈 {',
            '    render() {',
            '…'
        ]);
    });

    test('a long marked node shows its first and last lines', async () => {
        const pat = pattern`${capture('arr')}.map(${capture('x')} => ${capture('body')})`;
        const gen = parser.parse({
            text: `
            arr.map(function (x) {
                const a = x + 1;
                const b = a * 2;
                const c = b - 3;
                const d = c / 4;
                return d;
            })`,
            sourcePath: 'test.ts'
        });
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[0].element;
        const node = isExpressionStatement(statement) ? statement.expression : statement;

        await pat.match(node, new Cursor(node, new Cursor(cu)), {debug: true});

        const lines = printed();
        const source = lines.slice(lines.findIndex(line => line.endsWith('Source tree:')) + 1);
        const tree = source.slice(0, source.findIndex(line => line.endsWith(']')));
        expect(tree.map(line => line.replace(/^\[Pattern #\d+] {7}/, ''))).toEqual([
            'arr.map(👉function (x) {',
            '    const a = x + 1;',
            '…',
            '    return d;',
            '}👈)'
        ]);
    });

    test('a rewrite rule prints why it does not apply when asked to', async () => {
        const rule = rewrite(() => ({
            before: pattern`console.log(${capture('x')})`,
            after: template`log(${capture('x')})`
        }));
        const node = await parseExpression('console.error(1)');

        await rule.tryOn(new Cursor(node), node, {debug: true});

        expect(printed().some(line => line.includes('❌ FAILED matching against'))).toBe(true);
    });

    test('a capture of an optional receiver holds its ?', async () => {
        await pattern`${capture('a')}.b()`.match(await parseExpression('a?.b()'), undefined!, {debug: true});

        expect(printed().some(line => line.endsWith('Ⓐa?Ⓐ.b()'))).toBe(true);
    });

    test('an attempt like an earlier one prints one line, and each attempt ends in a blank line', async () => {
        const pat = pattern`console.log(${capture('x')})`;
        const node = await parseExpression('console.error(1)');

        await pat.match(node, undefined!, {debug: true});
        const first = consoleErrorSpy.mock.calls.length;
        await pat.match(node, undefined!, {debug: true});

        const calls: string[] = consoleErrorSpy.mock.calls.map(c => c[0]);
        expect(calls[first - 1].endsWith('\n')).toBe(true);
        expect(calls.slice(first)).toEqual([expect.stringMatching(/❌ FAILED matching against .*, as before\n$/)]);
    });

    test('a constraint that throws names its capture', async () => {
        const pat = pattern`foo(${capture({name: 'n', constraint: (n: any) => n.value.startsWith('a')})})`;

        await expect(pat.match(await parseExpression('foo(x)'), undefined!, {debug: true}))
            .rejects.toThrow(/constraint of capture n threw/);
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
        expect(calls.some(c => c.includes("Captures:"))).toBe(true);
    });

    test('prints a failure with both trees and how they differ', async () => {
        const x = capture('x');
        const y = capture('y');
        const pat = pattern({ debug: true })`${x} + ${y}`;
        // Pattern expects addition, but we provide subtraction
        const node = await parseExpression('a - b');

        const match = await pat.match(node, undefined!);

        expect(match).toBeUndefined();
        expect(consoleErrorSpy).toHaveBeenCalled();

        const calls = consoleErrorSpy.mock.calls.map(c => c[0]);

        const lines: string[] = calls.flatMap(c => c.split('\n'));
        expect(lines.some(line => line.includes('❌ FAILED matching against binary (J$Binary)'))).toBe(true);
        expect(lines.some(line => line.endsWith('Source tree:'))).toBe(true);
        expect(lines.some(line => line.includes('Reason:'))).toBe(false);
        expect(lines.some(line => line.endsWith('Pattern:  operator: +'))).toBe(true);
        expect(lines.some(line => line.endsWith('Source:   operator: -'))).toBe(true);
    });

    test('prints the matched tree without the whitespace before it', async () => {
        const gen = parser.parse({
            text: `foo();
    console.error(42);`,
            sourcePath: 'test.ts'
        });
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[1].element;
        const node = isExpressionStatement(statement) ? statement.expression : statement;

        await pattern`console.log(42)`.match(node, undefined!, {debug: true});

        const calls: string[] = consoleErrorSpy.mock.calls.map(c => c[0]);
        const lines = calls.flatMap(c => c.split('\n'));
        const header = lines.findIndex(line => line.includes('Source tree'));
        expect(lines[header + 1]).toMatch(/\] {7}console\./);
    });
});
