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
import {capture, JavaScriptParser, JS, pattern, replace, template} from '../../../src/javascript';
import {J} from '../../../src/java';

describe('replace with context', () => {
    let parser: JavaScriptParser;

    beforeEach(() => {
        parser = new JavaScriptParser();
    });

    async function parseExpression(source: string): Promise<J> {
        const results = await parser.parse({text: source, sourcePath: 'test.ts'});
        const cu = results[0] as JS.CompilationUnit;
        const stmt = cu.statements[0].element as JS.ExpressionStatement;
        return stmt.expression;
    }

    describe('new replace API', () => {
        test('single pattern replacement with object destructuring', async () => {
            const swapOperands = replace<J.Binary>(() => {
                const {left, right} = {left: capture(), right: capture()};
                return {
                    before: pattern`${left} + ${right}`,
                    after: template`${right} + ${left}`
                };
            });

            const ast = await parseExpression('a + b');
            const result = await swapOperands.tryOn(ast, {} as any, {tree: ast});

            expect(result).toBeDefined();
            const binaryResult = result as J.Binary;
            expect((binaryResult.left as J.Identifier).simpleName).toBe('b');
            expect((binaryResult.right as J.Identifier).simpleName).toBe('a');
        });

        test('single pattern replacement with individual captures', async () => {
            const swapOperands = replace<J.Binary>(() => {
                const left = capture();
                const right = capture();
                return {
                    before: pattern`${left} + ${right}`,
                    after: template`${right} + ${left}`
                };
            });

            const ast = await parseExpression('a + b');
            const result = await swapOperands.tryOn(ast, {} as any, {tree: ast});

            expect(result).toBeDefined();
            const binaryResult = result as J.Binary;
            expect((binaryResult.left as J.Identifier).simpleName).toBe('b');
            expect((binaryResult.right as J.Identifier).simpleName).toBe('a');
        });

        test('multiple patterns replacement', async () => {
            const normalizeComparisons = replace<J.Binary>(() => {
                const {left, right} = {left: capture(), right: capture()};
                return {
                    before: [
                        pattern`${left} == ${right}`,
                        pattern`${left} != ${right}`
                    ],
                    after: template`${left} === ${right}`
                };
            });

            // Test with == operator
            const equalityAst = await parseExpression('a == b');
            const equalityResult = await normalizeComparisons.tryOn(equalityAst, {} as any, {tree: equalityAst});

            expect(equalityResult).toBeDefined();
            const binaryEquality = equalityResult as J.Binary;
            expect(binaryEquality.operator.element).toBe(JS.Binary.Type.IdentityEquals);

            // Test with != operator
            const inequalityAst = await parseExpression('a != b');
            const inequalityResult = await normalizeComparisons.tryOn(inequalityAst, {} as any, {tree: inequalityAst});

            expect(inequalityResult).toBeDefined();
            const binaryInequality = inequalityResult as J.Binary;
            expect(binaryInequality.operator.element).toBe(JS.Binary.Type.IdentityEquals);

            // Test with unmatched operator (should return undefined)
            const additionAst = await parseExpression('a + b');
            const additionResult = await normalizeComparisons.tryOn(additionAst, {} as any, {tree: additionAst});

            expect(additionResult).toBeUndefined();
        });

        test('captures work across patterns and template', async () => {
            const rule = replace<J.Binary>(() => {
                const {expr} = {expr: capture()};
                return {
                    before: [
                        pattern`${expr} || false`,
                        pattern`false || ${expr}`
                    ],
                    after: template`${expr}`
                };
            });

            // Test first pattern: expr || false
            const pattern1Ast = await parseExpression('someCondition || false');
            const pattern1Result = await rule.tryOn(pattern1Ast, {} as any, {tree: pattern1Ast});

            expect(pattern1Result).toBeDefined();
            expect((pattern1Result as J.Identifier).simpleName).toBe('someCondition');

            // Test second pattern: false || expr
            const pattern2Ast = await parseExpression('false || someCondition');
            const pattern2Result = await rule.tryOn(pattern2Ast, {} as any, {tree: pattern2Ast});

            expect(pattern2Result).toBeDefined();
            expect((pattern2Result as J.Identifier).simpleName).toBe('someCondition');
        });

        test('error handling for missing properties', () => {
            expect(() => {
                replace<J.Binary>(() => {
                    return {} as any;
                });
            }).toThrow('Builder function must return an object with before and after properties');
        });
    });

    describe('compatibility with existing rewrite API', () => {
        test('old rewrite API still works', async () => {
            const swapOperandsOld = replace(() => {
                const left = capture();
                const right = capture();
                return {
                    before: pattern`${left} + ${right}`,
                    after: template`${right} + ${left}`
                };
            });

            const ast = await parseExpression('a + b');
            const result = await swapOperandsOld.tryOn(ast, {} as any, {tree: ast});

            expect(result).toBeDefined();
            const binaryResult = result as J.Binary;
            expect((binaryResult.left as J.Identifier).simpleName).toBe('b');
            expect((binaryResult.right as J.Identifier).simpleName).toBe('a');
        });

        test('both APIs can be used in the same codebase', () => {
            // New API
            const newRule = replace<J.Binary>(() => {
                const {left, right} = {left: capture(), right: capture()};
                return {
                    before: pattern`${left} + ${right}`,
                    after: template`${right} + ${left}`
                };
            });

            // Old API
            const oldRule = replace(() => {
                const left = capture();
                const right = capture();
                return {
                    before: pattern`${left} + ${right}`,
                    after: template`${right} + ${left}`
                };
            });

            expect(newRule).toBeDefined();
            expect(oldRule).toBeDefined();
            expect(typeof newRule.tryOn).toBe('function');
            expect(typeof oldRule.tryOn).toBe('function');
        });
    });
});
