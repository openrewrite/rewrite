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
import {fromVisitor, RecipeSpec} from '../../src/test';
import {any, AsyncJavaScriptVisitor, capture, npm, packageJson, pattern, typescript} from '../../src/javascript';
import {J} from '../../src/java';
import {withDir} from 'tmp-promise';

describe('JavaScriptSemanticComparatorVisitor', () => {
    const spec = new RecipeSpec();

    describe('lenient type matching', () => {
        test('matches pattern with dependencies to source with same types', async () => {
            await withDir(async (repo) => {
                const tempDir = repo.path;

                // Pattern with namespace import and dependencies
                const pat = pattern`util.isDate(${capture('arg')})`
                    .configure({
                        context: [`import * as util from 'util'`],
                        dependencies: {'@types/node': '^20.0.0'}
                    });

                const matches: string[] = [];

                spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                    override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                        if ((methodInvocation.name as J.Identifier).simpleName === 'isDate') {
                            const m = await pat.match(methodInvocation, this.cursor);
                            if (m) {
                                const importStyle = methodInvocation.select ? 'namespace' : 'named';
                                matches.push(importStyle);
                            }
                        }
                        return methodInvocation;
                    }
                });

                await spec.rewriteRun(
                    npm(
                        tempDir,
                        //language=typescript
                        typescript(
                            `
                                import { isDate } from 'util';
                                import * as util from 'util';

                                const check1 = isDate(new Date());
                                const check2 = util.isDate(new Date());
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                            {
                              "name": "test",
                              "version": "1.0.0",
                              "dependencies": {
                                "@types/node": "^20.0.0"
                              }
                            }
                            `
                        )
                    )
                );

                // Both import styles should match the pattern
                expect(matches).toContain('named');
                expect(matches).toContain('namespace');
            }, {unsafeCleanup: true});
        }, 60000);

        test('matches pattern without any imports or dependencies to typed source', async () => {
            await withDir(async (repo) => {
                const tempDir = repo.path;

                // Pattern WITHOUT any context or dependencies
                const pat = pattern`util.isArray(${capture('arg')})`;
                // No configuration at all - completely bare pattern

                let matchFound = false;

                spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                    override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                        if ((methodInvocation.name as J.Identifier).simpleName === 'isArray') {
                            const m = await pat.match(methodInvocation, this.cursor);
                            if (m) {
                                matchFound = true;
                            }
                        }
                        return methodInvocation;
                    }
                });

                await spec.rewriteRun(
                    npm(
                        tempDir,
                        //language=typescript
                        typescript(
                            `
                                import * as util from 'util';
                                const x = [1, 2, 3];
                                util.isArray(x);
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                            {
                              "name": "test",
                              "version": "1.0.0",
                              "dependencies": {
                                "@types/node": "^20.0.0"
                              }
                            }
                            `
                        )
                    )
                );

                // Pattern with no imports should still match source with full type attribution
                expect(matchFound).toBe(true);
            }, {unsafeCleanup: true});
        }, 60000);
    });

    describe('redundant parentheses', () => {
        test('matches expression with redundant parentheses', async () => {
            await withDir(async (repo) => {
                const tempDir = repo.path;

                // Pattern without parentheses
                const pat = pattern`x + 1`;

                let matchCount = 0;

                spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                    override async visitBinary(binary: J.Binary, _p: any): Promise<J | undefined> {
                        const m = await pat.match(binary, this.cursor);
                        if (m) {
                            matchCount++;
                        }
                        return binary;
                    }
                });

                await spec.rewriteRun(
                    npm(
                        tempDir,
                        //language=typescript
                        typescript(
                            `
                                const x = 5;
                                const a = x + 1;
                                const b = (x + 1);
                                const c = ((x + 1));
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                            {
                              "name": "test",
                              "version": "1.0.0"
                            }
                            `
                        )
                    )
                );

                // All three expressions should match
                expect(matchCount).toBe(3);
            }, {unsafeCleanup: true});
        });

        test('matches simple identifier with and without parentheses', async () => {
            await withDir(async (repo) => {
                const tempDir = repo.path;

                // Pattern: simple identifier
                const pat = pattern`x`;

                let matchCount = 0;

                spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                    override async visitIdentifier(identifier: J.Identifier, _p: any): Promise<J | undefined> {
                        if (identifier.simpleName === 'x') {
                            const m = await pat.match(identifier, this.cursor);
                            if (m) {
                                matchCount++;
                            }
                        }
                        return identifier;
                    }
                });

                await spec.rewriteRun(
                    npm(
                        tempDir,
                        //language=typescript
                        typescript(
                            `
                                const x = 5;
                                const a = x;
                                const b = (x);
                                const c = ((x));
                            `
                        ),
                        //language=json
                        packageJson(
                            `
                            {
                              "name": "test",
                              "version": "1.0.0"
                            }
                            `
                        )
                    )
                );

                // All three expressions AND declaration should match
                expect(matchCount).toBe(4);
            }, {unsafeCleanup: true});
        });
    });

    describe('arrow function equivalence', () => {
        test('matches expression body with block containing single return', async () => {
            // Pattern: expression body
            const pat = pattern`x => x + 1`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitArrowFunction(arrow: any, _p: any): Promise<any> {
                    const m = await pat.match(arrow, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return arrow;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = x => x + 1;
                        const b = x => { return x + 1; };
                    `
                )
            );

            // Both should match
            expect(matchCount).toBe(2);
        });

        test('matches block with return to expression body', async () => {
            // Pattern: block with return
            const pat = pattern`(x, y) => { return x + y; }`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitArrowFunction(arrow: any, _p: any): Promise<any> {
                    const m = await pat.match(arrow, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return arrow;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = (x, y) => x + y;
                        const b = (x, y) => { return x + y; };
                    `
                )
            );

            // Both should match
            expect(matchCount).toBe(2);
        });

        test('does not match block with multiple statements', async () => {
            // Pattern: simple expression body
            const pat = pattern`x => x + 1`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitArrowFunction(arrow: any, _p: any): Promise<any> {
                    const m = await pat.match(arrow, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return arrow;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = x => x + 1;
                        const b = x => {
                            console.log(x);
                            return x + 1;
                        };
                    `
                )
            );

            // Only the first one should match
            expect(matchCount).toBe(1);
        });
    });

    describe('object property shorthand equivalence', () => {
        test('matches shorthand with longhand when names match', async () => {
            // Pattern: shorthand
            const pat = pattern`const ${any()} = { x, y }`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitVariableDeclarations(vd: any, _p: any): Promise<any> {
                    const m = await pat.match(vd, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return vd;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const x = 1;
                        const y = 2;
                        const a = { x, y };
                        const b = { x: x, y: y };
                    `
                )
            );

            // Both should match
            expect(matchCount).toBe(2);
        });

        test('matches longhand with shorthand when names match', async () => {
            // Pattern: longhand
            const pat = pattern`const ${any()} = { x: x, y: y }`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitVariableDeclarations(vd: any, _p: any): Promise<any> {
                    const m = await pat.match(vd, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return vd;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const x = 1;
                        const y = 2;
                        const a = { x, y };
                        const b = { x: x, y: y };
                    `
                )
            );

            // Both should match
            expect(matchCount).toBe(2);
        });

        test('does not match when property value is different', async () => {
            // Pattern: shorthand
            const pat = pattern`const ${any()} = { x }`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitVariableDeclarations(vd: any, _p: any): Promise<any> {
                    const m = await pat.match(vd, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return vd;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const x = 1;
                        const y = 2;
                        const a = { x };
                        const b = { x: y };
                    `
                )
            );

            // Only the first one should match
            expect(matchCount).toBe(1);
        });
    });

    describe('arrow function parameter parentheses equivalence', () => {
        test('matches single parameter with and without parentheses', async () => {
            // Pattern: without parentheses
            const pat = pattern`x => x + 1`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitArrowFunction(arrow: any, _p: any): Promise<any> {
                    const m = await pat.match(arrow, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return arrow;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = x => x + 1;
                        const b = (x) => x + 1;
                    `
                )
            );

            // Both should match
            expect(matchCount).toBe(2);
        });

        test('matches with parentheses pattern to without parentheses', async () => {
            // Pattern: with parentheses
            const pat = pattern`(x) => x + 1`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitArrowFunction(arrow: any, _p: any): Promise<any> {
                    const m = await pat.match(arrow, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return arrow;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = x => x + 1;
                        const b = (x) => x + 1;
                    `
                )
            );

            // Both should match
            expect(matchCount).toBe(2);
        });

        test('matches multi-parameter functions regardless of parentheses', async () => {
            // Pattern: multi-parameter (always has parentheses)
            const pat = pattern`(x, y) => x + y`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitArrowFunction(arrow: any, _p: any): Promise<any> {
                    const m = await pat.match(arrow, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return arrow;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = (x, y) => x + y;
                    `
                )
            );

            // Should match
            expect(matchCount).toBe(1);
        });
    });

    describe('void expression equivalence', () => {
        test('matches undefined with void expressions', async () => {
            // Pattern: undefined
            const pat = pattern`undefined`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitIdentifier(identifier: J.Identifier, _p: any): Promise<J.Identifier | undefined> {
                    if (identifier.simpleName === 'undefined') {
                        const m = await pat.match(identifier, this.cursor);
                        if (m) {
                            matchCount++;
                        }
                    }
                    return identifier;
                }

                protected async visitVoid(voidExpr: any, _p: any): Promise<any> {
                    const m = await pat.match(voidExpr, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return voidExpr;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = undefined;
                        const b = void 0;
                        const c = void(0);
                        const d = void 1;
                    `
                )
            );

            // All four should match
            expect(matchCount).toBe(4);
        });

        test('matches void expression with undefined', async () => {
            // Pattern: void 0
            const pat = pattern`void 0`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitIdentifier(identifier: J.Identifier, _p: any): Promise<J.Identifier | undefined> {
                    if (identifier.simpleName === 'undefined') {
                        const m = await pat.match(identifier, this.cursor);
                        if (m) {
                            matchCount++;
                        }
                    }
                    return identifier;
                }

                protected async visitVoid(voidExpr: any, _p: any): Promise<any> {
                    const m = await pat.match(voidExpr, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return voidExpr;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = undefined;
                        const b = void 0;
                        const c = void(0);
                    `
                )
            );

            // All three should match
            expect(matchCount).toBe(3);
        });
    });

    describe('numeric literal equivalence', () => {
        test('matches different representations of same numeric value', async () => {
            // Pattern: 255
            const pat = pattern`255`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitLiteral(literal: J.Literal, _p: any): Promise<J.Literal | undefined> {
                    const m = await pat.match(literal, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return literal;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = 255;
                        const b = 0xFF;
                        const c = 0o377;
                        const d = 0b11111111;
                    `
                )
            );

            // All four should match
            expect(matchCount).toBe(4);
        });

        test('matches scientific notation with integer', async () => {
            // Pattern: 1000
            const pat = pattern`1000`;

            let matchCount = 0;

            spec.recipe = fromVisitor(new class extends AsyncJavaScriptVisitor<any> {
                override async visitLiteral(literal: J.Literal, _p: any): Promise<J.Literal | undefined> {
                    const m = await pat.match(literal, this.cursor);
                    if (m) {
                        matchCount++;
                    }
                    return literal;
                }
            });

            await spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                        const a = 1000;
                        const b = 1e3;
                        const c = 1E3;
                    `
                )
            );

            // All three should match
            expect(matchCount).toBe(3);
        });
    });
});
