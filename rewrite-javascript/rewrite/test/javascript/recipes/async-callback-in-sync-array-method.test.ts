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

import {RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";
import {AsyncCallbackInSyncArrayMethod} from "../../../src/javascript/recipes/async-callback-in-sync-array-method";

describe('AsyncCallbackInSyncArrayMethod', () => {
    test('detects async callback in .some()', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AsyncCallbackInSyncArrayMethod();

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const users = [{ name: 'Alice' }, { name: 'Bob' }];
                    const hasAdmin = users.some(async user => {
                        return await checkPermission(user, 'admin');
                    });
                `,
                (actual: string) => {
                    expect(actual).toContain('/*~~(Async callback passed to .some()');
                    return actual;
                }
            )
        );
    });

    test('detects async callback in .every()', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AsyncCallbackInSyncArrayMethod();

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const items = [1, 2, 3];
                    const allValid = items.every(async item => {
                        return await validate(item);
                    });
                `,
                (actual: string) => {
                    expect(actual).toContain('/*~~(Async callback passed to .every()');
                    return actual;
                }
            )
        );
    });

    test('detects async callback in .filter()', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AsyncCallbackInSyncArrayMethod();

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const numbers = [1, 2, 3, 4, 5];
                    const evenNumbers = numbers.filter(async n => {
                        return await isEven(n);
                    });
                `,
                (actual: string) => {
                    expect(actual).toContain('/*~~(Async callback passed to .filter()');
                    return actual;
                }
            )
        );
    });

    test('detects async callback in .find()', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AsyncCallbackInSyncArrayMethod();

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const items = ['a', 'b', 'c'];
                    const found = items.find(async item => {
                        return await shouldInclude(item);
                    });
                `,
                (actual: string) => {
                    expect(actual).toContain('/*~~(Async callback passed to .find()');
                    return actual;
                }
            )
        );
    });

    test('detects async callback in .forEach()', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AsyncCallbackInSyncArrayMethod();

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const items = [1, 2, 3];
                    items.forEach(async item => {
                        await process(item);
                    });
                `,
                (actual: string) => {
                    expect(actual).toContain('/*~~(Async callback passed to .forEach()');
                    return actual;
                }
            )
        );
    });

    test('does not warn for sync callbacks', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AsyncCallbackInSyncArrayMethod();

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const numbers = [1, 2, 3, 4, 5];
                    const hasEven = numbers.some(n => n % 2 === 0);
                    const allPositive = numbers.every(n => n > 0);
                    const evens = numbers.filter(n => n % 2 === 0);
                `
            )
        );
    });

    test('does not warn for async-safe array methods', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AsyncCallbackInSyncArrayMethod();

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const items = [1, 2, 3];
                    // .map() with async returns Promise[], which can be awaited with Promise.all
                    const results = await Promise.all(items.map(async x => x * 2));
                `
            )
        );
    });

    test('detects async function reference passed to .some()', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AsyncCallbackInSyncArrayMethod();

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    async function isValidAsync(item: number): Promise<boolean> {
                        return item > 0;
                    }
                    const items = [1, 2, 3];
                    const hasValid = items.some(isValidAsync);
                `,
                (actual: string) => {
                    expect(actual).toContain('/*~~(Async callback passed to .some()');
                    return actual;
                }
            )
        );
    });

    test('detects hasScanningRecipe pattern from run.ts', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new AsyncCallbackInSyncArrayMethod();

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    interface Recipe {
                        recipeList(): Promise<Recipe[]>;
                    }

                    class ScanningRecipe {}

                    async function hasScanningRecipe(recipe: Recipe): Promise<boolean> {
                        return recipe instanceof ScanningRecipe || (await recipe.recipeList()).some(hasScanningRecipe);
                    }
                `,
                (actual: string) => {
                    expect(actual).toContain('/*~~(Async callback passed to .some()');
                    return actual;
                }
            )
        );
    });
});
