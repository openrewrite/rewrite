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
        const statement = cu.statements[0];
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
        expect(attempt.explanation!.path).toBeDefined();

        // Should have a path showing where the mismatch occurred
        // The path should point to the 'right' property of the binary expression
        expect(attempt.explanation!.path.length).toBeGreaterThan(0);
        expect(attempt.explanation!.path).toContain('J$Binary#right');

        // Should explain the type mismatch
        expect(attempt.explanation!.expected).toContain('42');
        expect(attempt.explanation!.actual).toContain('wrong');
    });

    test('path tracking captures property path to mismatch', async () => {
        const pat = pattern`[1, 2, 3]`;
        const node = await parseExpression('[1, 2, "wrong"]');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation).toBeDefined();
        expect(attempt.explanation!.path).toBeDefined();

        // Path should not be empty - should show which property led to mismatch
        expect(attempt.explanation!.path.length).toBeGreaterThan(0);
        // Array literals use 'initializer' property
        expect(attempt.explanation!.path).toContain('J$NewArray#initializer');

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

    test('deeply nested pattern shows multi-level path', async () => {
        // Use object in a context where it's an expression (as function argument)
        const pat = pattern`foo({a: 1, b: {c: 2}})`;
        const node = await parseExpression('foo({a: 1, b: {c: "wrong"}})');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation).toBeDefined();
        expect(attempt.explanation!.path).toBeDefined();

        // Path should have multiple levels showing the traversal through:
        // arguments[0] -> object body -> statements[1] -> property initializer -> nested object -> value
        expect(attempt.explanation!.path.length).toBeGreaterThan(5);

        // Path should include key elements showing the nested traversal
        // Note: Array indices are now compacted into the previous element during rendering
        expect(attempt.explanation!.path).toEqual([
            "J$MethodInvocation#arguments",
            "0",
            "J$NewClass#body",
            "J$Block#statements",
            "1",
            "JS$PropertyAssignment#initializer",
            "J$NewClass#body",
            "J$Block#statements",
            "0",
            "JS$PropertyAssignment#initializer"
        ]);

        // Should have array indices in the path, including "0" after "arguments"
        const hasNumericIndex = attempt.explanation!.path.some(p => /^\d+$/.test(p));
        expect(hasNumericIndex).toBe(true);

        // Specifically check that arguments is followed by an index
        const argsIndex = attempt.explanation!.path.findIndex(p => p.includes('#arguments'));
        if (argsIndex >= 0 && argsIndex < attempt.explanation!.path.length - 1) {
            const nextElement = attempt.explanation!.path[argsIndex + 1];
            // Index can be either plain "0" or kind-prefixed like "J$NewClass#0"
            expect(nextElement).toMatch(/^(\d+|.+#\d+)$/);
        }

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

    test('path tracking includes intermediate steps for object destructuring', async () => {
        const name = capture();
        const pat = pattern`const {${name}} = obj;`;

        // Target has two variables, pattern expects one
        const gen = parser.parse({
            text: 'const {a, b} = obj;',
            sourcePath: 'test.ts'
        });
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const statement = cu.statements[0];

        const attempt = await pat.matchWithExplanation(statement, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation).toBeDefined();

        // The path should show the full navigation through:
        // J$VariableDeclarations#variables → 0 → J$VariableDeclarations$NamedVariable#name → JS$ObjectBindingPattern#bindings
        expect(attempt.explanation!.path).toEqual([
            'J$VariableDeclarations#variables',
            '0',
            'J$VariableDeclarations$NamedVariable#name',
            'JS$ObjectBindingPattern#bindings'
        ]);

        // Should be an array length mismatch
        expect(attempt.explanation!.reason).toBe('array-length-mismatch');
        expect(attempt.explanation!.expected).toBe('1');
        expect(attempt.explanation!.actual).toBe('2');
    });

    test('path tracking includes property name for value mismatch', async () => {
        const pat = pattern`console.log(42)`;
        const node = await parseExpression('console.error(42)');

        const attempt = await pat.matchWithExplanation(node, undefined!);

        expect(attempt.matched).toBe(false);
        expect(attempt.explanation).toBeDefined();

        // The path should show the property that mismatched with kind information for nested objects
        expect(attempt.explanation!.path).toEqual(['J$MethodInvocation#name', 'J$Identifier#simpleName']);

        // Should be a value mismatch
        expect(attempt.explanation!.reason).toBe('value-mismatch');
        expect(attempt.explanation!.expected).toBe('"log"');
        expect(attempt.explanation!.actual).toBe('"error"');
    });

});
