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
import {capture, isExpressionStatement, JavaScriptParser, JS, pattern, sourceFileCache} from '../../../src/javascript';
import {J} from '../../../src/java';
import {Cursor} from '../../../src';
import {AnsiAwarePrintOutputCapture} from '../../../src/javascript/templating/debug';

/**
 * Tests for the debug system.
 * These tests verify the matchWithExplanation() API and debug information collection.
 */
describe('Pattern Debugging', () => {
    const parser = new JavaScriptParser({sourceFileCache});

    async function parseExpression(code: string): Promise<J> {
        const gen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[cu.statements.length - 1].element;
        return isExpressionStatement(statement) ? statement.expression : statement;
    }

    test('successful match returns matched=true with result', async () => {
        const x = capture('x');
        const pat = pattern`console.log(${x})`;
        const node = await parseExpression('console.log(42)');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(true);
        expect(attempt.result).toBeDefined();
        expect(attempt.result!.get('x')).toBeDefined();
        expect(attempt.explanation).toBeUndefined();
        expect(attempt.debugLog).toBeDefined();
    });

    test('failed match returns matched=false with explanation', async () => {
        const pat = pattern`42`;
        const node = await parseExpression('"string"');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.result).toBeUndefined();
        expect(attempt.explanation).toBeDefined();
        // Both are Literal nodes, but with different primitive values
        expect(attempt.explanation!.reason).toBe('value-mismatch');
        expect(attempt.debugLog).toBeDefined();
    });

    test('constraint failure provides detailed explanation', async () => {
        const value = capture({constraint: (node: J) => node.kind === J.Kind.Identifier});
        const pat = pattern`foo(${value})`;
        const node = await parseExpression('foo(42)') as J.MethodInvocation;

        const result = await pat.matchWithExplanation(node, undefined!);

        expect(result.matched).toBe(false);
        expect(result.explanation).toBeDefined();
        expect(result.explanation!.reason).toBe('constraint-failed');
        expect(result.explanation!.expected).toMatch(/^🪝1 satisfying \(node\) => node\.kind === /);
        expect(result.explanation!.actual).toBe('42');
        expect(result.explanation!.targetElement).toBe(node.arguments.elements[0].element);
    });

    test('debug log contains constraint evaluation entries', async () => {
        const value = capture({
            constraint: (node: J) => true
        });
        const pat = pattern`${value}`;
        const node = await parseExpression('42');

        const result = await pat.matchWithExplanation(node, undefined!);

        expect(result.matched).toBe(true);
        expect(result.debugLog).toBeDefined();

        const constraintLogs = result.debugLog!.filter(entry => entry.scope === 'constraint');
        expect(constraintLogs.length).toBeGreaterThan(0);

        // Should have "Evaluating constraint" and "Constraint passed" entries
        const evaluatingLog = constraintLogs.find(log => log.message.includes('Evaluating constraint'));
        const passedLog = constraintLogs.find(log => log.message.includes('Constraint passed'));

        expect(evaluatingLog).toBeDefined();
        expect(passedLog).toBeDefined();
    });

    test('a literal mismatch in a binary expression shows both literals', async () => {
        const pat = pattern`x + 42`;
        const node = await parseExpression('x + "wrong"');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation).toBeDefined();

        // Should explain the type mismatch
        expect(attempt.explanation!.expected).toContain('42');
        expect(attempt.explanation!.actual).toContain('wrong');
    });

    test('explanation captures mismatch details', async () => {
        const pat = pattern`[1, 2, 3]`;
        const node = await parseExpression('[1, 2, "wrong"]');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation).toBeDefined();

        // Should explain the value mismatch
        expect(attempt.explanation!.expected).toContain('3');
        expect(attempt.explanation!.actual).toContain('wrong');
    });

    test('variadic constraint failure is logged and explained', async () => {
        const args = capture({
            variadic: true,
            constraint: (nodes: J[]) => {
                // Require exactly 2 arguments
                return nodes.length === 2;
            }
        });
        const pat = pattern`console.log(${args})`;

        // Try with 1 argument (should fail)
        const node = await parseExpression('console.log(42)');
        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);

        // Debug log should contain variadic constraint evaluation entries
        expect(attempt.debugLog).toBeDefined();
        const variadicLogs = attempt.debugLog!.filter(entry =>
            entry.scope === 'constraint' && entry.message.toLowerCase().includes('variadic')
        );
        expect(variadicLogs.length).toBeGreaterThan(0);

        // Should log that constraint was evaluated
        const evaluatingLog = variadicLogs.find(log => log.message.includes('Evaluating'));
        expect(evaluatingLog).toBeDefined();

        // Should log that constraint failed (case-insensitive)
        const failedLog = variadicLogs.find(log => log.message.toLowerCase().includes('failed'));
        expect(failedLog).toBeDefined();

        expect(attempt.explanation!.reason).toBe('constraint-failed');
    });

    test('deeply nested pattern shows mismatch details', async () => {
        // Use object in a context where it's an expression (as function argument)
        const pat = pattern`foo({a: 1, b: {c: 2}})`;
        const node = await parseExpression('foo({a: 1, b: {c: "wrong"}})');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation).toBeDefined();

        // Should explain the value mismatch in the deeply nested property
        expect(attempt.explanation!.expected).toContain('2');
        expect(attempt.explanation!.actual).toContain('wrong');
    });

    test('debug logging can be selectively disabled', async () => {
        const pat = pattern`console.log(42)`;
        const node = await parseExpression('console.log(42)');

        const result = await pat.matchWithExplanation(node, undefined!, {
            enabled: true,
            logComparison: false,
            logConstraints: false
        });

        expect(result.matched).toBe(true);
        expect(result.debugLog).toBeDefined();

        // Should have no comparison or constraint logs
        const comparisonLogs = result.debugLog!.filter(entry => entry.scope === 'comparison');
        const constraintLogs = result.debugLog!.filter(entry => entry.scope === 'constraint');

        expect(comparisonLogs.length).toBe(0);
        expect(constraintLogs.length).toBe(0);
    });

    test('array length mismatch in object destructuring', async () => {
        const name = capture();
        const pat = pattern`const {${name}} = obj;`;

        // Target has two variables, pattern expects one
        const gen = parser.parse({
            text: 'const {a, b} = obj;',
            sourcePath: 'test.ts'
        });
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[0].element;

        const attempt = await pat.matchWithExplanation(statement, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation).toBeDefined();

        expect(attempt.explanation!.reason).toBe('array-length-mismatch');
        expect(attempt.explanation!.expected).toBe('bindings: 🪝1');
        expect(attempt.explanation!.actual).toBe('bindings: a, b');
    });

    test('a method name mismatch marks the names of both calls', async () => {
        const pat = pattern`console.log(42)`;
        const node = await parseExpression('console.error(42)') as J.MethodInvocation;

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation!.reason).toBe('value-mismatch');
        expect(attempt.explanation!.expected).toBe('"log"');
        expect(attempt.explanation!.actual).toBe('"error"');
        expect((attempt.explanation!.patternElement as J.Identifier).simpleName).toBe('log');
        expect(attempt.explanation!.targetElement).toBe(node.name);
    });

    test('a call declared in another module names both modules', async () => {
        const pat = pattern`isDate(${capture({variadic: true})})`
            .configure({context: [`import {isDate} from 'node:util/types';`]});
        const node = await parseExpression(`
            function isDate(value: unknown): boolean { return false; }
            isDate(new Date());`);

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation!.expected).toMatch(/^declared in .*util\/types$/);
        expect(attempt.explanation!.actual).toMatch(/^declared in test$/);
        expect(attempt.explanation!.details).toContain('the same declaration');
    });

    test('an operator mismatch names both operators', async () => {
        const pat = pattern`${capture('x')} + ${capture('y')}`;
        const node = await parseExpression('a - b');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.explanation!.expected).toBe('operator: +');
        expect(attempt.explanation!.actual).toBe('operator: -');
        expect(attempt.explanation!.targetElement).toBe(node);
    });

    test('a capture named twice shows what it already holds', async () => {
        const r = capture('r');
        const scalar = pattern`${capture('req')}.map(${r} => ${r}.json())`;
        const node = await parseExpression('req.map(res => other.json())');

        const attempt = await scalar.matchWithExplanation(node, undefined!);

        expect(attempt.explanation!.expected).toBe('res (what 🪝r holds already)');
        expect(attempt.explanation!.actual).toBe('other');
        expect((attempt.explanation!.targetElement as J.Identifier).simpleName).toBe('other');

        const args = capture({variadic: true});
        const variadic = pattern`f(${args}) || g(${args})`;
        const variadicAttempt = await variadic.matchWithExplanation(await parseExpression('f(a) || g(b)'), undefined!);

        expect(variadicAttempt.explanation!.expected).toContain('a');
        expect(variadicAttempt.explanation!.actual).toBe('b');
    });

    test('a pattern that writes out type arguments says the source has none', async () => {
        const pat = pattern`useState<number>(${capture('x')})`;
        const node = await parseExpression('useState(0)');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.explanation!.reason).toBe('structural-mismatch');
        expect(attempt.explanation!.expected).toBe('type arguments: number');
        expect(attempt.explanation!.actual).toBe('type arguments: none');
        expect(attempt.explanation!.patternElement!.kind).toBe(J.Kind.Container);
        expect(attempt.explanation!.targetElement).toBe(node);
    });

    test('a property only one side has is named', async () => {
        const pat = pattern`import {${capture('name')}} from 'lodash'`;

        const attempt = await pat.matchWithExplanation(await parseExpression(`import _ from 'lodash';`), undefined!);

        expect(attempt.explanation!.reason).toBe('structural-mismatch');
        expect(attempt.explanation!.expected).toBe('default import: none');
        expect(attempt.explanation!.actual).toBe('default import: _');
    });

    test('a capture that backtracking rebinds does not hide the mismatch after it', async () => {
        const args = capture({variadic: true});
        const pat = pattern`f(${args}) || g(${args}, ${capture('y')}) || h(1)`;

        const attempt = await pat.matchWithExplanation(await parseExpression('f(a) || g(a, b) || h(2)'), undefined!);

        expect(attempt.explanation!.expected).toBe('1');
        expect(attempt.explanation!.actual).toBe('2');
    });

    test('an object literal property count names properties and suggests a variadic capture', async () => {
        const pat = pattern`f({timeout: ${capture('t')}})`;

        const node = await parseExpression('f({timeout: 5, retries: 3})');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.explanation!.expected).toBe('properties: timeout: 🪝t');
        expect(attempt.explanation!.actual).toBe('properties: timeout: 5, retries: 3');
        expect(attempt.explanation!.details).toContain('capture({variadic: true})');
    });

    test('a literal mismatch shows the literals as written', async () => {
        const attempt = await pattern`f('one')`.matchWithExplanation(await parseExpression(`f('two')`), undefined!);

        expect(attempt.explanation!.expected).toBe(`'one'`);
        expect(attempt.explanation!.actual).toBe(`'two'`);
    });

    test('a modifier only one side has is named', async () => {
        const pat = pattern`(${capture('x')}) => ${capture('y')}`;

        const attempt = await pat.matchWithExplanation(await parseExpression('async (a) => a'), undefined!);

        expect(attempt.explanation!.expected).toBe('modifiers: none');
        expect(attempt.explanation!.actual).toBe('modifiers: async');
    });

    test('a parameter count mismatch lists both parameter lists', async () => {
        const attempt = await pattern`(a) => a`.matchWithExplanation(await parseExpression('(a, b) => a'), undefined!);

        expect(attempt.explanation!.reason).toBe('array-length-mismatch');
        expect(attempt.explanation!.expected).toBe('parameters: a');
        expect(attempt.explanation!.actual).toBe('parameters: a, b');
    });

    test('a statement count mismatch lists both statement lists', async () => {
        const pat = pattern`function f() { ${capture({variadic: true})}; foo(); bar(); }`;

        const attempt = await pat.matchWithExplanation(await parseExpression('function f() { foo(); }'), undefined!);

        expect(attempt.explanation!.expected).toBe('statements: 🪝1, foo(), bar()');
        expect(attempt.explanation!.actual).toBe('statements: foo()');
    });

    test('a kind mismatch names both kinds in words', async () => {
        const attempt = await pattern`42`.matchWithExplanation(await parseExpression('x'), undefined!);

        expect(attempt.explanation!.reason).toBe('kind-mismatch');
        expect(attempt.explanation!.expected).toBe('literal (J$Literal)');
        expect(attempt.explanation!.actual).toBe('identifier (J$Identifier)');

        const arrow = pattern`${capture('arr')}.map(${capture('x')} => ${capture('body')})`;
        const functionAttempt = await arrow.matchWithExplanation(
            await parseExpression('arr.map(function (x) { return x * 2; })'), undefined!);

        expect(functionAttempt.explanation!.expected).toBe('arrow function (JS$ArrowFunction)');
        expect(functionAttempt.explanation!.actual).toBe('function expression (J$MethodDeclaration)');
    });

    test('parentheses the pattern lacks mark both trees and say how to match them', async () => {
        const node = await parseExpression('(a.b)');

        const attempt = await pattern`a.b`.matchWithExplanation(node, undefined!);

        expect(attempt.explanation!.patternElement!.kind).toBe(J.Kind.FieldAccess);
        expect(attempt.explanation!.targetElement).toBe(node);
        expect(attempt.explanation!.details).toContain('write them in the pattern');
    });

    test('a receiver written another way says names match as written', async () => {
        const pat = pattern`Object.assign({}, ${capture('a')})`;

        const node = await parseExpression('globalThis.Object.assign({}, a)');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.explanation!.expected).toBe('identifier (J$Identifier)');
        expect(attempt.explanation!.details).toContain('as it is written');
    });

    test('an attempt says where its code is, and reports itself as printed text', async () => {
        const gen = parser.parse({
            text: `foo();
console.error(42);`,
            sourcePath: 'test.ts'
        });
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const node = cu.statements[1].element;

        const attempt = await pattern`console.log(42)`.matchWithExplanation(node, new Cursor(node, new Cursor(cu)));

        expect(attempt.location).toEqual({sourcePath: 'test.ts', line: 2, column: 1});
        expect(attempt.report).toContain('Source tree:');
        expect(attempt.report).toContain('console.👉error👈(42)');
        expect(attempt.report).not.toContain('\x1b');
        expect(attempt.report).not.toContain('[Pattern #');
        // As an assertion message, it ends before the test runner's own text starts
        expect(attempt.report!.endsWith('\n')).toBe(true);
    });

    test('two kinds with the same name in words show their operators', async () => {
        const pat = pattern`${capture('l')} === ${capture('r')}`;

        const attempt = await pat.matchWithExplanation(await parseExpression('x - y'), undefined!);

        expect(attempt.explanation!.expected).toBe('operator: ===');
        expect(attempt.explanation!.actual).toBe('operator: -');
    });

    test('inverse video over several lines starts each line at its code', () => {
        const out = new AnsiAwarePrintOutputCapture();

        out.append('foo\x1b[7m(1,\n    2)\x1b[27m');

        expect(out.out).toBe('foo\x1b[7m(1,\x1b[27m\n    \x1b[7m2)\x1b[27m');
    });

});
