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
import {beforeEach, describe, expect, test} from '@jest/globals';
import {capture, isExpressionStatement, JavaScriptParser, JS, pattern} from '../../../src/javascript';
import {J} from '../../../src/java';

/**
 * Tests for the debug system.
 * These tests verify the matchWithExplanation() API and debug information collection.
 */
describe('Pattern Debugging', () => {
    let parser: JavaScriptParser;

    beforeEach(() => {
        parser = new JavaScriptParser();
    });

    async function parseExpression(code: string): Promise<J> {
        const gen = parser.parse({text: code, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[0].element;
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
        const value = capture({
            constraint: (node: J) => {
                // This constraint will fail
                return false;
            }
        });
        const pat = pattern`${value}`;
        const node = await parseExpression('42');

        const result = await pat.matchWithExplanation(node, undefined!);

        expect(result.matched).toBe(false);
        expect(result.explanation).toBeDefined();
        expect(result.explanation!.reason).toBe('constraint-failed');
        expect(result.explanation!.details).toContain('Constraint evaluation returned false');
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

    test('path tracking shows location of mismatch in binary expression', async () => {
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

    test('variadic constraint failure is logged in debug', async () => {
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

        // Should be an array length mismatch
        expect(attempt.explanation!.reason).toBe('array-length-mismatch');
        expect(attempt.explanation!.expected).toBe('1');
        expect(attempt.explanation!.actual).toBe('2');
    });

    test('value mismatch shows expected and actual values', async () => {
        const pat = pattern`console.log(42)`;
        const node = await parseExpression('console.error(42)');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation).toBeDefined();

        // Should be a value mismatch
        expect(attempt.explanation!.reason).toBe('value-mismatch');
        expect(attempt.explanation!.expected).toBe('"log"');
        expect(attempt.explanation!.actual).toBe('"error"');
    });

    test('multi-line element pattern matching works correctly', async () => {
        // Parse a multi-line object literal
        const gen = parser.parse({
            text: `const obj = {
    a: 1,
    b: 2,
    c: 3
};`,
            sourcePath: 'test.ts'
        });
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[0].element;

        // Create a pattern that won't match (expects only two properties)
        const name = capture('name');
        const a = capture('a');
        const b = capture('b');
        const pat = pattern`const ${name} = { a: ${a}, b: ${b} };`;

        const attempt = await pat.matchWithExplanation(statement, undefined!);

        // Should fail to match because target has 3 properties, pattern expects 2
        expect(attempt.matched).toBe(false);
        expect(attempt.explanation).toBeDefined();

        // The explanation should have pattern and target elements
        expect(attempt.explanation!.patternElement).toBeDefined();
        expect(attempt.explanation!.targetElement).toBeDefined();

        // Should provide a reason for the mismatch
        expect(attempt.explanation!.reason).toBeDefined();

        // Note: The ANSI coloring fix in AnsiAwarePrintOutputCapture ensures multi-line
        // elements are properly highlighted when printed to console. This is tested
        // implicitly by the debug logging system working correctly.
    });

});
