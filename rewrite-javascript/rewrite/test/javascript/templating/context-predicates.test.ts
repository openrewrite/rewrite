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
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {_, JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {isIf, isMethodDeclaration, J} from "../../../src/java";

describe('Context Predicates on RewriteRule', () => {
    const spec = new RecipeSpec();

    describe('where predicate', () => {
        test('only applies transformation in matching context', () => {
            const rule = rewrite(() => ({
                before: pattern`console.log(${_('msg')})`,
                after: template`logger.info(${_('msg')})`,
                where: (node, cursor) => {
                    // Only apply inside functions named 'handleError'
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName === 'handleError';
                }
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, method) || method;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        function handleError(err) {
                            console.log(err);
                        }

                        function otherFunction() {
                            console.log("test");
                        }
                    `,
                    `
                        function handleError(err) {
                            logger.info(err);
                        }

                        function otherFunction() {
                            console.log("test");
                        }
                    `
                )
            );
        });

        test('does not apply when where predicate returns false', () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')} + ${_('y')}`,
                after: template`${_('y')} + ${_('x')}`,
                where: (node, cursor) => {
                    // Never apply
                    return false;
                }
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript('const a = x + y'), // No change expected
            );
        });
    });

    describe('whereNot predicate', () => {
        test('excludes transformation in excluded context', () => {
            const rule = rewrite(() => ({
                before: pattern`console.log(${_('msg')})`,
                after: template`logger.info(${_('msg')})`,
                whereNot: (node, cursor) => {
                    // Don't apply inside 'debugFunction'
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName === 'debugFunction';
                }
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, method) || method;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        function normalFunction() {
                            console.log("normal");
                        }

                        function debugFunction() {
                            console.log("debug");
                        }
                    `,
                    `
                        function normalFunction() {
                            logger.info("normal");
                        }

                        function debugFunction() {
                            console.log("debug");
                        }
                    `
                )
            );
        });

        test('applies when whereNot predicate returns false', () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')} + ${_('y')}`,
                after: template`${_('y')} + ${_('x')}`,
                whereNot: (node, cursor) => {
                    // Always apply (never exclude)
                    return false;
                }
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript('const a = x + y', 'const a = y + x'),
            );
        });
    });

    describe('where and whereNot together', () => {
        test('applies only when where is true AND whereNot is false', () => {
            const rule = rewrite(() => ({
                before: pattern`console.log(${_('msg')})`,
                after: template`logger.info(${_('msg')})`,
                where: (node, cursor) => {
                    // Apply inside functions starting with 'handle'
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName.startsWith('handle') || false;
                },
                whereNot: (node, cursor) => {
                    // But not inside 'handleDebug'
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName === 'handleDebug';
                }
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, method) || method;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        function handleError(err) {
                            console.log(err);
                        }

                        function handleDebug(msg) {
                            console.log(msg);
                        }

                        function otherFunction() {
                            console.log("test");
                        }
                    `,
                    `
                        function handleError(err) {
                            logger.info(err);
                        }

                        function handleDebug(msg) {
                            console.log(msg);
                        }

                        function otherFunction() {
                            console.log("test");
                        }
                    `
                )
            );
        });
    });

    describe('context checking with cursor API', () => {
        test('checks for ancestor node types', () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')}`,
                after: template`wrapped(${_('x')})`,
                where: (node, cursor) => {
                    // Only apply inside if statements
                    return cursor.firstEnclosing(isIf) !== undefined;
                }
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitIdentifier(ident: J.Identifier, p: any): Promise<J | undefined> {
                    if (ident.simpleName === 'x') {
                        return await rule.tryOn(this.cursor, ident) || ident;
                    }
                    return ident;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const y = x;
                        if (condition) {
                            const z = x;
                        }
                    `,
                    `
                        const y = x;
                        if (condition) {
                            const z = wrapped(x);
                        }
                    `
                )
            );
        });
    });

    describe('multiple patterns with context predicates', () => {
        test('tries each pattern with its context until one succeeds', () => {
            const rule = rewrite(() => ({
                before: [
                    pattern`console.log(${_('msg')})`,
                    pattern`console.error(${_('msg')})`
                ],
                after: template`logger.info(${_('msg')})`,
                where: (node, cursor) => {
                    // Only inside 'safeFunction'
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName === 'safeFunction';
                }
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, method) || method;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        function safeFunction() {
                            console.log("info");
                            console.error("error");
                        }

                        function otherFunction() {
                            console.log("info");
                            console.error("error");
                        }
                    `,
                    `
                        function safeFunction() {
                            logger.info("info");
                            logger.info("error");
                        }

                        function otherFunction() {
                            console.log("info");
                            console.error("error");
                        }
                    `
                )
            );
        });
    });

    describe('composition with andThen', () => {
        test('context predicates on first rule are respected', () => {
            const rule1 = rewrite(() => ({
                before: pattern`${_('a')} + ${_('b')}`,
                after: template`${_('b')} + ${_('a')}`,
                where: (node, cursor) => {
                    // Only swap in 'swapFunction'
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName === 'swapFunction';
                }
            }));

            const rule2 = rewrite(() => ({
                before: pattern`x + ${_('other')}`,
                after: template`y + ${_('other')}`
            }));

            const combined = rule1.andThen(rule2);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        function swapFunction() {
                            const a = x + z;
                        }

                        function otherFunction() {
                            const b = x + z;
                        }
                    `,
                    `
                        function swapFunction() {
                            const a = z + x;
                        }

                        function otherFunction() {
                            const b = x + z;
                        }
                    `
                )
            );
        });
    });

    describe('composition with orElse', () => {
        test('tries second rule with its context when first rule context fails', () => {
            const specificRule = rewrite(() => ({
                before: pattern`console.log(${_('msg')})`,
                after: template`logger.debug(${_('msg')})`,
                where: (node, cursor) => {
                    // Only in 'debugFunction'
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName === 'debugFunction';
                }
            }));

            const generalRule = rewrite(() => ({
                before: pattern`console.log(${_('msg')})`,
                after: template`logger.info(${_('msg')})`,
                where: (node, cursor) => {
                    // In all other functions
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName !== 'debugFunction';
                }
            }));

            const combined = specificRule.orElse(generalRule);

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                    return await combined.tryOn(this.cursor, method) || method;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        function debugFunction() {
                            console.log("debug");
                        }

                        function normalFunction() {
                            console.log("normal");
                        }
                    `,
                    `
                        function debugFunction() {
                            logger.debug("debug");
                        }

                        function normalFunction() {
                            logger.info("normal");
                        }
                    `
                )
            );
        });
    });

    describe('async predicates', () => {
        test('supports async where predicate', async () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')} + ${_('y')}`,
                after: template`${_('y')} + ${_('x')}`,
                where: async (node, cursor) => {
                    // Simulate async operation
                    await Promise.resolve();
                    return true;
                }
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript('const a = x + y', 'const a = y + x'),
            );
        });

        test('supports async whereNot predicate', async () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')} + ${_('y')}`,
                after: template`${_('y')} + ${_('x')}`,
                whereNot: async (node, cursor) => {
                    // Simulate async operation
                    await Promise.resolve();
                    return false;
                }
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript('const a = x + y', 'const a = y + x'),
            );
        });
    });
});
