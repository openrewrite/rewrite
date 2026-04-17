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
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {_, JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {isIf, isMethodDeclaration, J} from "../../../src/java";

describe('preMatch and postMatch Predicates on RewriteRule', () => {
    const spec = new RecipeSpec();

    describe('preMatch predicate', () => {
        test('filters before pattern matching based on context', () => {
            const rule = rewrite(() => ({
                before: pattern`console.log(${_('msg')})`,
                after: template`logger.info(${_('msg')})`,
                preMatch: (node, {cursor}) => {
                    // Only attempt matching inside functions named 'handleError'
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

        test('skips pattern matching when preMatch returns false', () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')} + ${_('y')}`,
                after: template`${_('y')} + ${_('x')}`,
                preMatch: () => false // Never match
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

        test('excludes specific contexts with negated condition', () => {
            const rule = rewrite(() => ({
                before: pattern`console.log(${_('msg')})`,
                after: template`logger.info(${_('msg')})`,
                preMatch: (node, {cursor}) => {
                    // Don't apply inside 'debugFunction'
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName !== 'debugFunction';
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

        test('checks for ancestor node types', () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')}`,
                after: template`wrapped(${_('x')})`,
                preMatch: (node, {cursor}) => {
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

        test('supports async preMatch predicate', async () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')} + ${_('y')}`,
                after: template`${_('y')} + ${_('x')}`,
                preMatch: async () => {
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
    });

    describe('postMatch predicate', () => {
        test('filters after pattern matching based on captures', () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')} + ${_('y')}`,
                after: template`${_('y')} + ${_('x')}`,
                postMatch: (node, {captures}) => {
                    // Only swap if 'x' is the identifier 'a'
                    const x = captures.get('x') as J.Identifier;
                    return x?.simpleName === 'a';
                }
            }));

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                    return await rule.tryOn(this.cursor, binary) || binary;
                }
            });

            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const r1 = a + b;
                        const r2 = x + y;
                    `,
                    `
                        const r1 = b + a;
                        const r2 = x + y;
                    `
                )
            );
        });

        test('skips transformation when postMatch returns false', () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')} + ${_('y')}`,
                after: template`${_('y')} + ${_('x')}`,
                postMatch: () => false // Never apply
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

        test('supports async postMatch predicate', async () => {
            const rule = rewrite(() => ({
                before: pattern`${_('x')} + ${_('y')}`,
                after: template`${_('y')} + ${_('x')}`,
                postMatch: async () => {
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
    });

    describe('preMatch and postMatch together', () => {
        test('applies preMatch first, then postMatch after pattern match', () => {
            const rule = rewrite(() => ({
                before: pattern`console.log(${_('msg')})`,
                after: template`logger.info(${_('msg')})`,
                preMatch: (node, {cursor}) => {
                    // Only attempt matching inside functions starting with 'handle'
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName.startsWith('handle') || false;
                },
                postMatch: (node, {captures}) => {
                    // Only apply if the message is a string literal
                    const msg = captures.get('msg') as J.Literal;
                    return typeof msg?.value === 'string';
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
                            console.log("error message");
                            console.log(err);
                        }

                        function otherFunction() {
                            console.log("test");
                        }
                    `,
                    `
                        function handleError(err) {
                            logger.info("error message");
                            console.log(err);
                        }

                        function otherFunction() {
                            console.log("test");
                        }
                    `
                )
            );
        });
    });

    describe('multiple patterns with preMatch', () => {
        test('preMatch is evaluated once, then each pattern is tried', () => {
            const rule = rewrite(() => ({
                before: [
                    pattern`console.log(${_('msg')})`,
                    pattern`console.error(${_('msg')})`
                ],
                after: template`logger.info(${_('msg')})`,
                preMatch: (node, {cursor}) => {
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
        test('preMatch on first rule is respected', () => {
            const rule1 = rewrite(() => ({
                before: pattern`${_('a')} + ${_('b')}`,
                after: template`${_('b')} + ${_('a')}`,
                preMatch: (node, {cursor}) => {
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
        test('tries second rule when first rule preMatch fails', () => {
            const specificRule = rewrite(() => ({
                before: pattern`console.log(${_('msg')})`,
                after: template`logger.debug(${_('msg')})`,
                preMatch: (node, {cursor}) => {
                    // Only in 'debugFunction'
                    const method = cursor.firstEnclosing(isMethodDeclaration);
                    return method?.name.simpleName === 'debugFunction';
                }
            }));

            const generalRule = rewrite(() => ({
                before: pattern`console.log(${_('msg')})`,
                after: template`logger.info(${_('msg')})`,
                preMatch: (node, {cursor}) => {
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
});
