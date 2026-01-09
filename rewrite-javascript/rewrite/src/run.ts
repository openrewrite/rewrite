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
            this.before ? TreePrinters.print(this.before) : "",
            this.after ? TreePrinters.print(this.after) : "",
            "",
            "",
            {context: 3}
        );
    }
}

async function hasScanningRecipe(recipe: Recipe): Promise<boolean> {
    if (recipe instanceof ScanningRecipe) return true;
    for (const item of (await recipe.recipeList())) {
        if (await hasScanningRecipe(item)) return true;
    }
    return false;
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
    const changeset: Result[] = [];
    for await (const result of scheduleRunStreaming(recipe, before, ctx)) {
        changeset.push(result);
    }
    return { changeset };
}

export type ProgressCallback = (phase: 'parsing' | 'scanning' | 'processing', current: number, total: number, sourcePath: string) => void;

/**
 * Streaming version of scheduleRun that yields results as soon as each file is processed.
 * This allows callers to print diffs immediately and free memory earlier.
 *
 * Accepts either an array or an async iterable of source files. Files are processed
 * immediately as they're yielded from the iterable, avoiding the need to collect all
 * files into memory before starting work.
 *
 * For scanning recipes, each file is scanned immediately as it's pulled from the generator,
 * then stored for the edit phase. The scan phase completes before any results are yielded.
 *
 * @param onProgress Optional callback for progress updates during scanning and processing phases.
 */
export async function* scheduleRunStreaming(
    recipe: Recipe,
    before: SourceFile[] | AsyncIterable<SourceFile>,
    ctx: ExecutionContext,
    onProgress?: ProgressCallback
): AsyncGenerator<Result, void, undefined> {
    const cursor = rootCursor();
    const isScanning = await hasScanningRecipe(recipe);

    if (isScanning) {
        // For scanning recipes, pull files from the generator and scan them immediately.
        // Files are stored for the later edit phase.
        const files: SourceFile[] = [];
        const iterable = Array.isArray(before) ? before : before;
        const knownTotal = Array.isArray(before) ? before.length : -1; // -1 = unknown total

        // Phase 1: Pull files from generator and scan each immediately
        let scanCount = 0;
        for await (const b of iterable) {
            files.push(b);
            scanCount++;
            onProgress?.('scanning', scanCount, knownTotal, b.sourcePath);

            // Scan this file immediately
            await recurseRecipeList(recipe, b, async (recipe, b2) => {
                if (recipe instanceof ScanningRecipe) {
                    return (await recipe.scanner(recipe.accumulator(cursor, ctx))).visit(b2, ctx, cursor)
                }
            });
        }

        const totalFiles = files.length;

        // Phase 2: Collect generated files
        const generated = (await recurseRecipeList(recipe, [] as SourceFile[], async (recipe, generated) => {
            if (recipe instanceof ScanningRecipe) {
                generated.push(...await recipe.generate(recipe.accumulator(cursor, ctx), ctx));
            }
            return generated;
        }))!;

        // Phase 3: Edit existing files and yield results immediately
        for (let i = 0; i < files.length; i++) {
            const b = files[i];
            onProgress?.('processing', i + 1, totalFiles, b.sourcePath);
            const editedB = await recurseRecipeList(recipe, b, async (recipe, b2) => (await recipe.editor()).visit(b2, ctx, cursor));
            // Always yield a result so the caller knows when each file is processed
            yield new Result(b, editedB !== b ? editedB : b);
            // Clear array entry to allow GC to free memory for this file
            (files as any)[i] = null;
        }

        // Phase 4: Edit generated files and yield results
        for (const g of generated) {
            const editedG = await recurseRecipeList(recipe, g, async (recipe, g2) => (await recipe.editor()).visit(g2, ctx, cursor));
            if (editedG) {
                yield new Result(undefined, editedG);
            }
        }
    } else {
        // For non-scanning recipes, process files immediately as they come in
        const iterable = Array.isArray(before) ? before : before;
        const knownTotal = Array.isArray(before) ? before.length : -1; // -1 = unknown total
        let processCount = 0;
        for await (const b of iterable) {
            processCount++;
            onProgress?.('processing', processCount, knownTotal, b.sourcePath);
            const editedB = await recurseRecipeList(recipe, b, async (recipe, b2) => (await recipe.editor()).visit(b2, ctx, cursor));
            // Always yield a result so the caller knows when each file is processed
            yield new Result(b, editedB !== b ? editedB : b);
        }
    }

    await recursiveOnComplete(recipe, ctx);
}
