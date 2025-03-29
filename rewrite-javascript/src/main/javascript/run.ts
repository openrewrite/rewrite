import {ExecutionContext} from "./execution";
import {rootCursor, SourceFile} from "./tree";
import {createTwoFilesPatch} from "diff";
import {TreePrinters} from "./print";
import {DataTable, getRowsByDataTableName} from "./data-table";
import {Recipe, ScanningRecipe} from "./recipe";
import {mapAsync} from "./util";

export interface RecipeRun {
    changeset: Result[],
    dataTables: [DataTable<any>, any[]][]
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
    return recipe instanceof ScanningRecipe || recipe.recipeList.some(hasScanningRecipe);
}

async function recurseRecipeList<T>(recipe: Recipe, initial: T, fn: (recipe: Recipe, t: T) => Promise<T | undefined>): Promise<T | undefined> {
    let t: T | undefined = await fn(recipe, initial);
    for (const subRecipe of recipe.recipeList) {
        if (t === undefined) {
            return undefined;
        }
        t = await recurseRecipeList(subRecipe, t, fn);
    }
    return t;
}

async function recursiveOnComplete(recipe: Recipe, ctx: ExecutionContext): Promise<void> {
    await recipe.onComplete(ctx);
    recipe.recipeList.forEach(r => recursiveOnComplete(r, ctx));
}

export async function scheduleRun(recipe: Recipe, before: SourceFile[], ctx: ExecutionContext): Promise<RecipeRun> {
    const cursor = rootCursor();
    if (await hasScanningRecipe(recipe)) {
        await mapAsync(before, async before => await recurseRecipeList(recipe, before, (recipe, b) =>
            recipe instanceof ScanningRecipe ?
                recipe.scanner(recipe.accumulator(cursor, ctx)).visit(b, ctx, cursor) :
                Promise.resolve(b)));
    }

    const generated = (await recurseRecipeList(recipe, [] as SourceFile[], async (recipe, generated) => {
        if (recipe instanceof ScanningRecipe) {
            generated.push(...recipe.generate(recipe.accumulator(cursor, ctx)));
        }
        return generated;
    }))!;

    const after = await mapAsync(before.concat(generated), async before => await recurseRecipeList(recipe, before, (recipe, b) => recipe.editor.visit(b, ctx)));

    await recursiveOnComplete(recipe, ctx);

    return {
        changeset: before.concat(generated).flatMap((b, i) =>
            after[i] !== b ? [new Result(b, after[i])] : []),
        dataTables: getRowsByDataTableName(ctx).map(([dt, rows]) =>
            [(recipe as any)[dt] as DataTable<any>, rows])
    };
}
