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
import {capture, JavaScriptParser, JS, pattern, rewrite, template} from '../../../src/javascript';
import {J} from '../../../src/java';

describe('replace with context', () => {
    let parser: JavaScriptParser;

    beforeEach(() => {
        parser = new JavaScriptParser();
    });

    async function parseExpression(source: string): Promise<J> {
        const parseGenerator = parser.parse({text: source, sourcePath: 'test.ts'});
        const cu: JS.CompilationUnit = (await parseGenerator.next()).value as JS.CompilationUnit;        const stmt = cu.statements[0].element as JS.ExpressionStatement;
        return stmt.expression;
    }

    describe('new replace API', () => {
        test('multiple patterns replacement', async () => {
            const normalizeComparisons = rewrite(() => ({
                before: [
                    pattern`${"left"} == ${"right"}`,
                    pattern`${"left"} != ${"right"}`
                ],
                after: template`${"left"} === ${"right"}`
            }));

            // Test with == operator
            const equalityAst = await parseExpression('a == b');
            const equalityResult = await normalizeComparisons.tryOn({} as any, equalityAst);

            expect(equalityResult).toBeDefined();
            const binaryEquality = equalityResult as J.Binary;
            expect(binaryEquality.operator.element).toBe(JS.Binary.Type.IdentityEquals);

            // Test with != operator
            const inequalityAst = await parseExpression('a != b');
            const inequalityResult = await normalizeComparisons.tryOn({} as any, inequalityAst);

            expect(inequalityResult).toBeDefined();
            const binaryInequality = inequalityResult as J.Binary;
            expect(binaryInequality.operator.element).toBe(JS.Binary.Type.IdentityEquals);

            // Test with unmatched operator (should return undefined)
            const additionAst = await parseExpression('a + b');
            const additionResult = await normalizeComparisons.tryOn({} as any, additionAst);

            expect(additionResult).toBeUndefined();
        });

        test('captures work across patterns and template', async () => {
            const rule = rewrite(() => {
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
            const pattern1Result = await rule.tryOn({} as any, pattern1Ast);

            expect(pattern1Result).toBeDefined();
            expect((pattern1Result as J.Identifier).simpleName).toBe('someCondition');

            // Test second pattern: false || expr
            const pattern2Ast = await parseExpression('false || someCondition');
            const pattern2Result = await rule.tryOn({} as any, pattern2Ast);

            expect(pattern2Result).toBeDefined();
            expect((pattern2Result as J.Identifier).simpleName).toBe('someCondition');
        });

        test('error handling for missing properties', () => {
            expect(() => {
                rewrite(() => {
                    return {} as any;
                });
            }).toThrow('Builder function must return an object with before and after properties');
        });
    });
});
