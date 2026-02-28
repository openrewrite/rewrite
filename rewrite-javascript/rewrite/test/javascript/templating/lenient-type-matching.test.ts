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
import {capture, JavaScriptVisitor, npm, packageJson, pattern, template, tsx, typescript} from "../../../src/javascript";
import {J, Type} from "../../../src/java";
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
                        const m = await pat.match(methodInvocation, this.cursor);
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
                const m = await pat.match(methodDeclaration, this.cursor);
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
                const m = await pat.match(methodInvocation, this.cursor);
                if (m) {
                    return await tmpl.apply(methodInvocation, this.cursor, { values: m });
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
                const m = await pat.match(methodDeclaration, this.cursor);
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
                const m = await pat.match(methodDeclaration, this.cursor);
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
                const m = await pat.match(methodDeclaration, this.cursor);
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

    test('strict mode with aliased import should match based on type', async () => {
        await withDir(async (repo) => {
            const tempDir = repo.path;

            // Pattern uses the original import name
            const pat = pattern`isDate(${capture('arg')})`
                .configure({
                    context: [`import { isDate } from 'node:util/types'`],
                    lenientTypeMatching: false, // Strict type matching
                    dependencies: {'@types/node': '^20.0.0'}
                });

            let matchFound = false;
            let capturedArg: any = undefined;

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                    const m = await pat.match(methodInvocation, this.cursor);
                    if (m) {
                        matchFound = true;
                        capturedArg = m.get('arg');
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
                        import { isDate as isDateFn } from 'node:util/types';

                        const result = isDateFn(new Date());
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

            // With strict type matching, aliased imports should still match if types match
            expect(matchFound).toBe(true);
            expect(capturedArg).toBeDefined();
        }, {unsafeCleanup: true});
    }, 60000);

    test('lenient mode with aliased import also matches based on type', async () => {
        await withDir(async (repo) => {
            const tempDir = repo.path;

            // Pattern uses the original import name, with lenient mode (default)
            const pat = pattern`isDate(${capture('arg')})`
                .configure({
                    context: [`import { isDate } from 'node:util/types'`],
                    // lenientTypeMatching defaults to true
                    dependencies: {'@types/node': '^20.0.0'}
                });

            let matchFound = false;
            let capturedArg: any = undefined;

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                    const m = await pat.match(methodInvocation, this.cursor);
                    if (m) {
                        matchFound = true;
                        capturedArg = m.get('arg');
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
                        import { isDate as checkDate } from 'node:util/types';

                        const result = checkDate(new Date());
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

            // With lenient type matching, if types exist and match, aliased imports should also match
            expect(matchFound).toBe(true);
            expect(capturedArg).toBeDefined();
        }, {unsafeCleanup: true});
    }, 60000);

    test('aliased import matching without type attribution (import-based resolution)', async () => {
        // Pattern uses the original import name
        // Testing if parser tracks import origins (module + original name) without type attribution
        const pat = pattern`isDate(${capture('arg')})`
            .configure({
                context: [`import { isDate } from 'node:util/types'`]
                // Note: NO dependencies - pattern won't have type attribution
            });

        let matchFound = false;
        let capturedArg: any = undefined;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                const m = await pat.match(methodInvocation, this.cursor);
                if (m) {
                    matchFound = true;
                    capturedArg = m.get('arg');
                }
                return methodInvocation;
            }
        });

        // Test code also has NO type attribution (plain typescript() without npm/dependencies)
        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                import { isDate as checkDate } from 'node:util/types';

                const result = checkDate(new Date());
                `
            )
        );

        // If this matches, it means the parser tracks import metadata (module + original export name)
        // even without full type resolution, allowing import-based alias resolution
        // If this fails, we currently require full type attribution for alias matching
        expect(matchFound).toBe(true);
        expect(capturedArg).toBeDefined();
    });

    test.skip('pattern matches both named and namespace imports (react vs React)', async () => {
        await withDir(async (repo) => {
            const tempDir = repo.path;

            // Pattern uses named import: forwardRef()
            const pat = pattern`forwardRef(${capture('fn')})`
                .configure({
                    context: [`import { forwardRef } from 'react'`],
                    dependencies: {'@types/react': '^18.0.0'}
                });

            let namedImportMatched = false;
            let namespaceImportMatched = false;

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                    const m = await pat.match(methodInvocation, this.cursor);
                    if (m) {
                        const select = methodInvocation.select;
                        if (!select) {
                            namedImportMatched = true; // forwardRef(...)
                        } else if (select.kind === J.Kind.Identifier) {
                            namespaceImportMatched = true; // React.forwardRef(...)
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
                        import { forwardRef } from 'react';
                        import * as React from 'react';

                        // Named import (module: 'react')
                        const A = forwardRef(() => null);

                        // Namespace import (class/interface: 'React')
                        const B = React.forwardRef(() => null);
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

            // Pattern should match both forms (case-insensitive FQN: 'react' vs 'React')
            expect(namedImportMatched).toBe(true);
            expect(namespaceImportMatched).toBe(true);
        }, {unsafeCleanup: true});
    }, 60000);

    test('namespace imports result in consistent type attribution FQN', async () => {
        await withDir(async (repo) => {
            const tempDir = repo.path;

            let namedImportDeclaringType: string | undefined;
            let namespaceImportDeclaringType: string | undefined;

            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                    const methodName = (methodInvocation.name as J.Identifier).simpleName;
                    if (methodName === 'forwardRef' && methodInvocation.methodType) {
                        const declaringType = methodInvocation.methodType.declaringType;
                        if (declaringType && Type.isFullyQualified(declaringType)) {
                            const fqn = Type.FullyQualified.getFullyQualifiedName(declaringType);

                            const select = methodInvocation.select;
                            if (!select) {
                                // Named import: forwardRef(...)
                                namedImportDeclaringType = fqn;
                            } else if (select.kind === J.Kind.Identifier) {
                                // Namespace import: React.forwardRef(...)
                                namespaceImportDeclaringType = fqn;
                            }
                        }
                    }
                    return methodInvocation;
                }
            });

            await spec.rewriteRun(
                npm(
                    tempDir,
                    //language=tsx
                    tsx(
                        `
                        import {forwardRef} from 'react';
                        import * as React from 'react';

                        const c1 = forwardRef((props, ref) => <div ref={ref} />);
                        const c2 = React.forwardRef((props, ref) => <div ref={ref} />);
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

            // Both import forms should have the same declaring type FQN: 'React' (uppercase)
            // This tests that the namespace-aware type mapping correctly identifies both as the React namespace
            expect(namedImportDeclaringType).toBe('React');
            expect(namespaceImportDeclaringType).toBe('React');
        }, {unsafeCleanup: true});
    }, 60000);
});
