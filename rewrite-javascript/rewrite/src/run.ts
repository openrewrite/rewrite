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
import {ExecutionContext} from "./execution";
import {rootCursor, SourceFile} from "./tree";
import {createTwoFilesPatch} from "diff";
import {TreePrinters} from "./print";
import {Recipe, ScanningRecipe} from "./recipe";

export interface RecipeRun {
    changeset: Result[]
}

export class Result {
    constructor(public readonly before?: SourceFile, public readonly after?: SourceFile) {
    }

    async diff(): Promise<string> {
        return createTwoFilesPatch(
            this.before?.sourcePath ?? "",
            this.after?.sourcePath ?? "",
            this.before ? await TreePrinters.print(this.before) : "",
            this.after ? await TreePrinters.print(this.after) : "",
            "",
            "",
            {context: 3}
        );
    }
}

async function hasScanningRecipe(recipe: Recipe): Promise<boolean> {
    return recipe instanceof ScanningRecipe || (await recipe.recipeList()).some(hasScanningRecipe);
}

async function recurseRecipeList<T>(recipe: Recipe, initial: T, fn: (recipe: Recipe, t: T) => Promise<T | undefined>): Promise<T | undefined> {
    let t: T | undefined = await fn(recipe, initial);
    for (const subRecipe of await recipe.recipeList()) {
        if (t === undefined) {
            return undefined;
        }
        t = await recurseRecipeList(subRecipe, t, fn);
    }
    return t;
}

async function recursiveOnComplete(recipe: Recipe, ctx: ExecutionContext): Promise<void> {
    await recipe.onComplete(ctx);
    (await recipe.recipeList()).forEach(r => recursiveOnComplete(r, ctx));
}

export async function scheduleRun(recipe: Recipe, before: SourceFile[], ctx: ExecutionContext): Promise<RecipeRun> {
    const cursor = rootCursor();
    if (await hasScanningRecipe(recipe)) {
        for (const b of before) {
            await recurseRecipeList(recipe, b, async (recipe, b2) => {
                if (recipe instanceof ScanningRecipe) {
                    return (await recipe.scanner(recipe.accumulator(cursor, ctx))).visit(b2, ctx, cursor)
                }
            });
        }
    }

    const generated = (await recurseRecipeList(recipe, [] as SourceFile[], async (recipe, generated) => {
        if (recipe instanceof ScanningRecipe) {
            generated.push(...await recipe.generate(recipe.accumulator(cursor, ctx), ctx));
        }
        return generated;
    }))!;

    const changeset: Result[] = [];

    for (const b of before) {
        const editedB = await recurseRecipeList(recipe, b, async (recipe, b2) => (await recipe.editor()).visit(b2, ctx));
        if (editedB !== b) {
            changeset.push(new Result(b, editedB));
        }
    }

    for (const g of generated) {
        const editedG = await recurseRecipeList(recipe, g, async (recipe, g2) => (await recipe.editor()).visit(g2, ctx));
        if (editedG) {
            changeset.push(new Result(undefined, editedG));
        }
    }

    await recursiveOnComplete(recipe, ctx);

    return {
        changeset: changeset
    };
}
