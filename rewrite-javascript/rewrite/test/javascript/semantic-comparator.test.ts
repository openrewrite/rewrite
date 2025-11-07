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
import {capture, JavaScriptVisitor, npm, packageJson, pattern, typescript} from '../../src/javascript';
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

                spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                    override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                        if ((methodInvocation.name as J.Identifier).simpleName === 'isDate') {
                            const m = await pat.match(methodInvocation);
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

                spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                    override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                        if ((methodInvocation.name as J.Identifier).simpleName === 'isArray') {
                            const m = await pat.match(methodInvocation);
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
});
