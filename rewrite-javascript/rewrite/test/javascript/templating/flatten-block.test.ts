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
import {
    capture,
    flattenBlock,
    JavaScriptVisitor,
    pattern,
    rewrite,
    template,
    typescript
} from "../../../src/javascript";
import {ExecutionContext} from "../../../src";
import {J} from "../../../src/java";

describe('flattenBlock', () => {
    test('flattens block statements into parent block', async () => {
        const spec = new RecipeSpec();

        const cond = capture('cond');
        const arr = capture('arr');
        const cb = capture('cb');

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override visitReturn(ret: J.Return, ctx: ExecutionContext): J | undefined {
                const result = rewrite(() => ({
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
                // Note: indentation comes from the template literal - extra indent is expected
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
            override visitReturn(ret: J.Return, ctx: ExecutionContext): J | undefined {
                const result = rewrite(() => ({
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
});
