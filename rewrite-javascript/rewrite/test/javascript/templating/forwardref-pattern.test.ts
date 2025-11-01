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
import {capture, JavaScriptVisitor, npm, packageJson, pattern, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {withDir} from "tmp-promise";

describe('forwardRef pattern matching', () => {
    const spec = new RecipeSpec();

    test('pattern with function name capture - WITH imports', async () => {
        // Try capturing just the function name - WITH imports configured
        const pat = pattern`forwardRef(function ${capture('name')}(props, ref) { return null; })`
            .configure({
                imports: [`import { forwardRef } from 'react'`]
            });

        const testCode = `
import { forwardRef } from 'react';
const MyComponent = forwardRef(function MyButton(props, ref) { return null; });
        `;

        let matchFound = false;
        let capturedName: any = undefined;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                const m = await pat.match(methodInvocation);
                if (m) {
                    matchFound = true;
                    capturedName = m.get('name');
                }
                return methodInvocation;
            }
        });

        await spec.rewriteRun(
            typescript(testCode)
        );

        expect(matchFound).toBe(true);
        expect(capturedName).toBeDefined();
    });

    test('pattern with arrow function and unconstrained captures - WITH dependencies', async () => {
        await withDir(async (repo) => {
            const tempDir = repo.path;

            // Pattern with unconstrained captures for props, ref, and body
            const pat = pattern`React.forwardRef((${capture('props')}, ${capture('ref')}) => ${capture('body')})`
                .configure({
                    imports: [`import * as React from 'react'`],
                    dependencies: {'@types/react': '^18.0.0'}
                });

            let matchFound = false;
            let capturedProps: any = undefined;
            let capturedRef: any = undefined;
            let capturedBody: any = undefined;

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                    if ((methodInvocation.name as J.Identifier).simpleName === 'forwardRef') {
                        const m = await pat.match(methodInvocation);
                        if (m) {
                            matchFound = true;
                            capturedProps = m.get('props');
                            capturedRef = m.get('ref');
                            capturedBody = m.get('body');
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
                        import * as React from 'react';

                        type UseProps = { use?: React.ReactType; children?: React.ReactNode };
                        const Use = React.forwardRef((props: UseProps, ref) =>
                          render(Object.assign(omit(props, "useNext"), { ref, use: props.useNext }))
                        );
                        `
                    ),
                    //language=json
                    packageJson(
                        `
                        {
                          "name": "test",
                          "version": "1.0.0",
                          "dependencies": {
                            "@types/react": "^18.0.0"
                          }
                        }
                        `
                    )
                )
            );

            // These captures should match ANY AST node in those positions
            expect(matchFound).toBe(true);
            expect(capturedProps).toBeDefined();
            expect(capturedRef).toBeDefined();
            expect(capturedBody).toBeDefined();
        }, {unsafeCleanup: true});
    }, 60000);
});
