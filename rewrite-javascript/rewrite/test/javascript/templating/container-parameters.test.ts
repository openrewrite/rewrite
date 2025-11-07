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
import {fromVisitor, RecipeSpec} from '../../../src/test';
import {JavaScriptVisitor, MethodMatcher, template, typescript} from '../../../src/javascript';
import {J} from '../../../src/java';
import {ExecutionContext} from '../../../src';

describe('container parameters in templates', () => {
    const spec = new RecipeSpec();

    test('template accepts J.RightPadded as parameter', async () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            sliceMatcher = new MethodMatcher("Buffer slice(..)");

            override async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                if (this.sliceMatcher.matches(method.methodType)) {
                    if (method.select) {
                        const selectExpr = method.select;  // J.RightPadded<Expression>
                        return await template`${selectExpr}.subarray()`.apply(this.cursor, method);
                    }
                }
                return super.visitMethodInvocation(method, p);
            }
        });

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const buf = Buffer.alloc(10);
                buf.slice();
                `,
                `
                const buf = Buffer.alloc(10);
                buf.subarray();
                `
            )
        );
    });

    test('template accepts J.Container as parameter for arguments', async () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            sliceMatcher = new MethodMatcher("Buffer slice(..)");

            override async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                if (this.sliceMatcher.matches(method.methodType)) {
                    if (method.select) {
                        const selectExpr = method.select;  // J.RightPadded<Expression>
                        const args = method.arguments;      // J.Container<Expression>
                        return await template`${selectExpr}.subarray(${args})`.apply(this.cursor, method);
                    }
                }
                return super.visitMethodInvocation(method, p);
            }
        });

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const buf = Buffer.alloc(10);
                buf.slice(0, 5);
                `,
                `
                const buf = Buffer.alloc(10);
                buf.subarray(0, 5);
                `
            )
        );
    });

    test('template accepts array of J.RightPadded as parameter', async () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                if ((method.name as J.Identifier).simpleName === 'foo') {
                    const args = method.arguments.elements;  // J.RightPadded<Expression>[]
                    return await template`bar(${args})`.apply(this.cursor, method);
                }
                return super.visitMethodInvocation(method, p);
            }
        });

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                foo(1, 2, 3);
                `,
                `
                bar(1, 2, 3);
                `
            )
        );
    });
});
