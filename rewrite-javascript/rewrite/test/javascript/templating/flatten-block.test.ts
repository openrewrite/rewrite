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

import {RecipeSpec, fromVisitor} from "../../../src/test";
import {typescript} from "../../../src/javascript";
import {ExecutionContext} from "../../../src";
import {JavaScriptVisitor} from "../../../src/javascript/visitor";
import {J} from "../../../src/java";
import {rewrite, pattern, template, capture, flattenBlock, Template} from "../../../src/javascript/templating";

describe('flattenBlock', () => {
    test('flattens block statements into parent block', async () => {
        const spec = new RecipeSpec();

        const cond = capture('cond');
        const arr = capture('arr');
        const cb = capture('cb');

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitReturn(ret: J.Return, ctx: ExecutionContext): Promise<J | undefined> {
                const result = await rewrite(() => ({
                    before: pattern`return ${cond} || ${arr}.some(${cb})`,
                    after: template`{
                        if (${cond}) return true;
                        for (const item of ${arr}) {
                            if (${cb}(item)) return true;
                        }
                        return false;
                    }`
                })).tryOn(this.cursor, ret);

                if (result && result.kind === J.Kind.Block) {
                    return flattenBlock(this, result as J.Block);
                }
                return result ?? ret;
            }
        }());

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    async function hasScanningRecipe(recipe: Recipe): Promise<boolean> {
                        return recipe instanceof ScanningRecipe || (await recipe.recipeList()).some(hasScanningRecipe);
                    }
                `,
                `
                    async function hasScanningRecipe(recipe: Recipe): Promise<boolean> {
                        if (recipe instanceof ScanningRecipe) return true;
                        for (const item of (await recipe.recipeList())) {
                            if (hasScanningRecipe(item)) return true;
                        }
                        return false;
                    }
                `
            )
        );
    });

    test('does not affect non-matching returns', async () => {
        const spec = new RecipeSpec();

        const cond = capture('cond');
        const arr = capture('arr');
        const cb = capture('cb');

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitReturn(ret: J.Return, ctx: ExecutionContext): Promise<J | undefined> {
                const result = await rewrite(() => ({
                    before: pattern`return ${cond} || ${arr}.some(${cb})`,
                    after: template`{
                        if (${cond}) return true;
                        for (const item of ${arr}) {
                            if (${cb}(item)) return true;
                        }
                        return false;
                    }`
                })).tryOn(this.cursor, ret);

                if (result && result.kind === J.Kind.Block) {
                    return flattenBlock(this, result as J.Block);
                }
                return result ?? ret;
            }
        }());

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    function simpleFunction(): boolean {
                        return true;
                    }
                `
            )
        );
    });

    describe('a block of calls in place of a call', () => {
        const spec = new RecipeSpec();

        function flattenCalls(after: Template) {
            const x = capture('x');
            const rule = rewrite(() => ({before: pattern`foo(${x})`, after}));
            return fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
                override async visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): Promise<J | undefined> {
                    const result = await rule.tryOn(this.cursor, method);
                    return result?.kind === J.Kind.Block ? flattenBlock(this, result as J.Block) : result ?? method;
                }
            }());
        }

        test('flattens into the statements of a file', () => {
            spec.recipe = flattenCalls(template`{
                a(1);
                b(1);
            }`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    foo(1);
                    `,
                    `
                    a(1);
                    b(1);
                    `
                )
            );
        });

        test('flattens into the statements of a case', () => {
            spec.recipe = flattenCalls(template`{
                a(1);
                b(1);
            }`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    switch (k) {
                        case 1:
                            foo(1);
                            break;
                    }
                    `,
                    `
                    switch (k) {
                        case 1:
                            a(1);
                            b(1);
                            break;
                    }
                    `
                )
            );
        });

        test('keeps the block where a name it declares is declared beside it', () => {
            spec.recipe = flattenCalls(template`{
                const tmp = 2;
                a(tmp);
            }`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    function f() {
                        const tmp = 1;
                        foo(tmp);
                    }
                    `,
                    `
                    function f() {
                        const tmp = 1;
                        {
                            const tmp = 2;
                            a(tmp);
                        }
                    }
                    `
                )
            );
        });

        test('keeps the block where a name it declares is read after it', () => {
            spec.recipe = flattenCalls(template`{
                const tmp = 2;
                a(tmp);
            }`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    const tmp = 1;
                    function f() {
                        foo(0);
                        b(tmp);
                    }
                    `,
                    `
                    const tmp = 1;
                    function f() {
                        {
                            const tmp = 2;
                            a(tmp);
                        }
                        b(tmp);
                    }
                    `
                )
            );
        });

        test('keeps the block where a name it declares is a parameter of the function', () => {
            spec.recipe = flattenCalls(template`{
                const tmp = 2;
                a(tmp);
            }`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    function f(tmp: number) {
                        foo(0);
                    }
                    `,
                    `
                    function f(tmp: number) {
                        {
                            const tmp = 2;
                            a(tmp);
                        }
                    }
                    `
                )
            );
        });

        test('flattens where the names it declares are its own', () => {
            spec.recipe = flattenCalls(template`{
                const tmp = 2;
                a(tmp);
            }`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    function f() {
                        foo(0);
                        b();
                    }
                    `,
                    `
                    function f() {
                        const tmp = 2;
                        a(tmp);
                        b();
                    }
                    `
                )
            );
        });

        test('keeps the comment before the first statement', () => {
            spec.recipe = flattenCalls(template`{
                // first
                a(1);
                // second
                b(1);
            }`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    function f() {
                        foo(0);
                    }
                    `,
                    `
                    function f() {
                        // first
                        a(1);
                        // second
                        b(1);
                    }
                    `
                )
            );
        });
    });
});
