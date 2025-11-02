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
import {capture, JavaScriptVisitor, npm, packageJson, pattern, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {withDir} from "tmp-promise";

describe('lenient type matching in patterns', () => {
    const spec = new RecipeSpec();

    test('untyped pattern matches typed arrow function parameters with dependencies', async () => {
        await withDir(async (repo) => {
            const tempDir = repo.path;

            // Pattern with unconstrained captures for props, ref, and body
            const pat = pattern`React.forwardRef((${capture('props')}, ${capture('ref')}) => ${capture('body')})`
                .configure({
                    context: [`import * as React from 'react'`],
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

    test('untyped function pattern matches typed function with return type', async () => {
        // Pattern with untyped function (no return type) should match typed function
        const pat = pattern`function ${capture('name')}() { return "hello"; }`;

        const testCode = `
function greet(): string { return "hello"; }
        `;

        let matchFound = false;
        let capturedName: any = undefined;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodDeclaration(methodDeclaration: J.MethodDeclaration, _p: any): Promise<J | undefined> {
                const m = await pat.match(methodDeclaration);
                if (m) {
                    matchFound = true;
                    capturedName = m.get('name');
                }
                return methodDeclaration;
            }
        });

        await spec.rewriteRun(
            typescript(testCode)
        );

        expect(matchFound).toBe(true);
        expect(capturedName).toBeDefined();
        expect((capturedName as J.Identifier).simpleName).toBe('greet');
    });

    test('untyped pattern matches and transforms with template replacement', async () => {
        // Pattern without type annotations matches typed code and replaces it with template
        // This demonstrates: matching untyped pattern against typed code + capturing + template replacement
        const pat = pattern`forwardRef(function ${capture('name')}(props, ref) { return null; })`
            .configure({
                context: [`import { forwardRef } from 'react'`]
            });

        // Template that uses the captured name as an identifier
        const tmpl = template`console.log(${capture('name')})`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                const m = await pat.match(methodInvocation);
                if (m) {
                    return await tmpl.apply(this.cursor, methodInvocation, m);
                }
                return methodInvocation;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    import { forwardRef } from 'react';
                    const MyComponent = forwardRef(function MyButton(props, ref) { return null; });
                `,
                `
                    import { forwardRef } from 'react';
                    const MyComponent = console.log(MyButton);
                `
            )
        );
    });

    test('strict type matching mode rejects untyped pattern against typed code', async () => {
        // Pattern with strict type matching (lenientTypeMatching: false) should NOT match typed function
        const pat = pattern`function ${capture('name')}() { return "hello"; }`
            .configure({
                lenientTypeMatching: false
            });

        const testCode = `
function greet(): string { return "hello"; }
        `;

        let matchFound = false;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodDeclaration(methodDeclaration: J.MethodDeclaration, _p: any): Promise<J | undefined> {
                const m = await pat.match(methodDeclaration);
                if (m) {
                    matchFound = true;
                }
                return methodDeclaration;
            }
        });

        await spec.rewriteRun(
            typescript(testCode)
        );

        // Strict mode: pattern without type should NOT match function with return type
        expect(matchFound).toBe(false);
    });

    test('lenient type matching can be explicitly enabled', async () => {
        // Pattern with explicit lenient type matching should match typed function
        const pat = pattern`function ${capture('name')}() { return "hello"; }`
            .configure({
                lenientTypeMatching: true
            });

        const testCode = `
function greet(): string { return "hello"; }
        `;

        let matchFound = false;
        let capturedName: any = undefined;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodDeclaration(methodDeclaration: J.MethodDeclaration, _p: any): Promise<J | undefined> {
                const m = await pat.match(methodDeclaration);
                if (m) {
                    matchFound = true;
                    capturedName = m.get('name');
                }
                return methodDeclaration;
            }
        });

        await spec.rewriteRun(
            typescript(testCode)
        );

        expect(matchFound).toBe(true);
        expect(capturedName).toBeDefined();
        expect((capturedName as J.Identifier).simpleName).toBe('greet');
    });

    test('strict mode with matching types does match', async () => {
        // Pattern with strict type matching and matching return type SHOULD match
        const pat = pattern`function ${capture('name')}(): string { return "hello"; }`
            .configure({
                lenientTypeMatching: false
            });

        const testCode = `
function greet(): string { return "hello"; }
        `;

        let matchFound = false;
        let capturedName: any = undefined;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodDeclaration(methodDeclaration: J.MethodDeclaration, _p: any): Promise<J | undefined> {
                const m = await pat.match(methodDeclaration);
                if (m) {
                    matchFound = true;
                    capturedName = m.get('name');
                }
                return methodDeclaration;
            }
        });

        await spec.rewriteRun(
            typescript(testCode)
        );

        // Strict mode with matching types should succeed
        expect(matchFound).toBe(true);
        expect(capturedName).toBeDefined();
        expect((capturedName as J.Identifier).simpleName).toBe('greet');
    });
});
