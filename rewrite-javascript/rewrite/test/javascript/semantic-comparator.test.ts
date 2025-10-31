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
import {JavaScriptSemanticComparatorVisitor} from '../../src/javascript/comparator';
import {J} from '../../src/java';
import {JavaScriptParser} from '../../src/javascript';
import {DependencyWorkspace} from '../../src/javascript/dependency-workspace';

describe('JavaScriptSemanticComparatorVisitor', () => {
    const comparator = new JavaScriptSemanticComparatorVisitor();

    // Helper function to parse code and get the AST
    async function parse(code: string, dependencies?: Record<string, string>): Promise<J> {
        let workspaceDir: string | undefined;
        if (dependencies) {
            workspaceDir = await DependencyWorkspace.getOrCreateWorkspace(dependencies);
        }

        const parser = new JavaScriptParser(workspaceDir ? {relativeTo: workspaceDir} : undefined);
        const parseGenerator = parser.parse({text: code, sourcePath: 'test.ts'});
        return (await parseGenerator.next()).value as J;
    }

    // Helper function to collect all method invocations by name from the AST
    async function getMethodInvocations(ast: J, methodName: string): Promise<J.MethodInvocation[]> {
        const found: J.MethodInvocation[] = [];

        const visitor = new class extends JavaScriptSemanticComparatorVisitor {
            override async visitMethodInvocation(method: J.MethodInvocation, _p: any): Promise<J | undefined> {
                if (method.name.simpleName === methodName) {
                    found.push(method);
                }
                return super.visitMethodInvocation(method, _p);
            }
        };

        await visitor.visit(ast, ast);
        return found;
    }

    describe('method invocations with module vs namespace imports', () => {
        test('matches forwardRef() and React.forwardRef() when types are identical', async () => {
            //language=typescript
            const code = `
                import { forwardRef } from 'react';
                import * as React from 'react';

                const c1 = forwardRef(() => null);
                const c2 = React.forwardRef(() => null);
            `;

            const ast = await parse(code, { '@types/react': '^18.0.0' });

            const methods = await getMethodInvocations(ast, 'forwardRef');

            expect(methods.length).toBe(2);

            // Both should match each other
            expect(await comparator.compare(methods[0], methods[1])).toBe(true);
        }, 60000);

        test('matches isDate() and util.isDate() when types are identical', async () => {
            //language=typescript
            const code = `
                import { isDate } from 'util';
                import * as util from 'util';

                const check1 = isDate(new Date());
                const check2 = util.isDate(new Date());
            `;

            const ast = await parse(code, { '@types/node': '^20.0.0' });

            const methods = await getMethodInvocations(ast, 'isDate');

            expect(methods.length).toBe(2);

            // Both should match each other
            expect(await comparator.compare(methods[0], methods[1])).toBe(true);
        }, 60000);

        test('does not match methods from different modules with case-insensitive names', async () => {
            //language=typescript
            const code = `
                import { isDate } from 'util';

                class Custom {
                    isDate(value: any) {
                        return typeof value === 'string';
                    }
                }

                const validator = new Custom();
                const check1 = isDate(new Date());
                const check2 = validator.isDate(new Date());
            `;

            const ast = await parse(code, { '@types/node': '^20.0.0' });

            const methods = await getMethodInvocations(ast, 'isDate');

            expect(methods.length).toBe(2);

            // Should NOT match - different declaring types
            expect(await comparator.compare(methods[0], methods[1])).toBe(false);
        }, 60000);
    });

    describe('method invocations without type attribution', () => {
        test('falls back to structural comparison when no types present', async () => {
            //language=typescript
            const code = `
                const obj1 = { foo: () => 'hello' };
                const obj2 = { foo: () => 'world' };

                obj1.foo();
                obj2.foo();
            `;

            const ast = await parse(code);

            const methods = await getMethodInvocations(ast, 'foo');

            expect(methods.length).toBe(2);

            // Should NOT match - different receivers (obj1 vs obj2)
            expect(await comparator.compare(methods[0], methods[1])).toBe(false);
        });
    });

    describe('method signature validation', () => {
        test('matches same method from same module despite different arguments', async () => {
            //language=typescript
            const code1 = `
                import { useState } from 'react';
                const [state, setState] = useState('hello');
            `;

            //language=typescript
            const code2 = `
                import { useState } from 'react';
                const [state, setState] = useState('hello');
            `;

            const ast1 = await parse(code1, { '@types/react': '^18.0.0' });
            const ast2 = await parse(code2, { '@types/react': '^18.0.0' });

            const methods1 = await getMethodInvocations(ast1, 'useState');
            const methods2 = await getMethodInvocations(ast2, 'useState');

            expect(methods1.length).toBe(1);
            expect(methods2.length).toBe(1);

            // Should match - both are useState from react with same arguments
            expect(await comparator.compare(methods1[0], methods2[0])).toBe(true);
        }, 60000);
    });

    describe('receiver (select) comparison', () => {
        test('does not match methods with different object receivers despite same method type', async () => {
            //language=typescript
            const code = `
                const array1 = [1, 2, 3];
                const array2 = [4, 5, 6];

                array1.push(7);
                array2.push(7);
            `;

            const ast = await parse(code);

            const methods = await getMethodInvocations(ast, 'push');

            expect(methods.length).toBe(2);

            // Should NOT match - different receivers (array1 vs array2)
            expect(await comparator.compare(methods[0], methods[1])).toBe(false);
        });

        test('does not match methods with different expressions as receivers', async () => {
            //language=typescript
            const code = `
                function getArray1() { return [1, 2]; }
                function getArray2() { return [3, 4]; }

                getArray1().push(5);
                getArray2().push(5);
            `;

            const ast = await parse(code);

            const methods = await getMethodInvocations(ast, 'push');

            expect(methods.length).toBe(2);

            // Should NOT match - different receivers (getArray1() vs getArray2())
            expect(await comparator.compare(methods[0], methods[1])).toBe(false);
        });
    });

    describe('edge cases', () => {
        test('handles methods with no declaring type', async () => {
            //language=typescript
            const code = `
                function foo() { return 1; }
                foo();
                foo();
            `;

            const ast = await parse(code);

            const methods = await getMethodInvocations(ast, 'foo');

            expect(methods.length).toBe(2);

            // Should match - both have no type information
            expect(await comparator.compare(methods[0], methods[1])).toBe(true);
        });

        test('does not match when only one has type attribution', async () => {
            //language=typescript
            const code = `
                import { isDate } from 'util';

                function isDate2(value: any) { return value instanceof Date; }

                isDate(new Date());
                isDate2(new Date());
            `;

            const ast = await parse(code, { '@types/node': '^20.0.0' });

            const methods1 = await getMethodInvocations(ast, 'isDate');
            const methods2 = await getMethodInvocations(ast, 'isDate2');

            expect(methods1.length).toBe(1);
            expect(methods2.length).toBe(1);

            // Should NOT match - one has type attribution, the other doesn't
            expect(await comparator.compare(methods1[0], methods2[0])).toBe(false);
        });
    });
});
