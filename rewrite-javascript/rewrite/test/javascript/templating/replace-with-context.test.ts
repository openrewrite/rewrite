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
import {capture, JavaScriptVisitor, pattern, rewrite, template, typescript} from '../../../src/javascript';
import {RecipeSpec, fromVisitor} from '../../../src/test';
import {J} from '../../../src/java';

describe('replace with context', () => {
    const spec = new RecipeSpec();

    describe('new replace API', () => {
        test('multiple patterns replacement', () => {
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    const rule = rewrite(() => {
                        const left = capture("left");
                        const right = capture("right");
                        return {
                            before: [
                                pattern`${left} == ${right}`,
                                pattern`${left} != ${right}`
                            ],
                            after: template`${left} === ${right}`
                        };
                    });
                    return await rule.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                // Test with == operator
                typescript('a == b', 'a === b'),
                // Test with != operator
                typescript('a != b', 'a === b'),
                // Test with unmatched operator (should not change)
                typescript('a + b')
            );
        });

        test('captures work across patterns and template', () => {
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    const rule = rewrite(() => {
                        const expr = capture();
                        return {
                            before: [
                                pattern`${expr} || false`,
                                pattern`false || ${expr}`
                            ],
                            after: template`${expr}`
                        };
                    });
                    return await rule.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                // Test first pattern: expr || false
                typescript('someCondition || false', 'someCondition'),
                // Test second pattern: false || expr
                typescript('false || someCondition', 'someCondition')
            );
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
