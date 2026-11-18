/*
 * Copyright 2026 the original author or authors.
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
import {Minutes, Recipe, RecipeDescriptor, ScanningRecipe} from "./recipe";
import {RecipeRef} from "./preconditions";
import {TreeVisitor} from "./visitor";
import {Cursor, isSourceFile, SourceFile, Tree} from "./tree";
import {ExecutionContext} from "./execution";
import {DataTableDescriptor, suppressDataTables} from "./data-table";
import {mapAsync} from "./util";

/**
 * Prepares a recipe tree for a run in this process so that the {@link ScanningRecipe#editPreconditions edit
 * preconditions} of its recipes limit which source files they and their recipe lists edit. Mirrors Java's
 * `org.openrewrite.internal.EditPreconditions`, which decides instead when the Java host runs the recipe.
 *
 * Call it once per run: the decorators it returns keep the decisions and accumulators of that run.
 */
export async function prepareEditPreconditions(recipe: Recipe): Promise<Recipe> {
    return guard(recipe, []);
}

/**
 * The recipe list to run after `recipe`, which {@link prepareEditPreconditions} returned or which is in a recipe
 * list this function returned.
 */
export async function editPreconditionsRecipeList(recipe: Recipe): Promise<Recipe[]> {
    return guardAll(await recipe.recipeList(), []);
}

/**
 * The recipe a {@link RecipeRef} condition runs as in this process.
 */
export async function recipeRefCondition(ref: RecipeRef): Promise<Recipe> {
    if (ref.localVisitor) {
        return new LocalCondition(ref.recipeName, ref.localVisitor);
    }
    const {DelegatingRecipe} = await import("./rpc/java-recipe.js");
    return new DelegatingRecipe(ref.recipeName, {...ref.options});
}

async function guardAll(recipes: Recipe[], guards: Guarded[]): Promise<Recipe[]> {
    return mapAsync(recipes, recipe => guard(recipe, guards));
}

async function guard(recipe: Recipe, guards: Guarded[]): Promise<Recipe> {
    if (recipe instanceof ScanningRecipe) {
        const conditions = await recipe.editPreconditions();
        if (conditions.length > 0) {
            return Guarded.create(recipe, conditions, guards);
        }
        return guards.length === 0 ? recipe : new GatedScanningRecipe(recipe, guards);
    }
    return guards.length === 0 ? recipe : new GatedRecipe(recipe, guards);
}

function allMatch(guards: Guarded[], sourceFile: SourceFile): boolean {
    return guards.every(guard => guard.matched(sourceFile));
}

class LocalCondition extends Recipe {
    readonly displayName: string = this.name;
    readonly description: string = `Evaluates \`${this.name}\` with a local visitor.`;

    constructor(readonly name: string, private readonly visitor: TreeVisitor<any, ExecutionContext>) {
        super();
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return this.visitor;
    }
}

interface Accumulator {
    readonly delegate: any
    readonly conditions: any[]
}

class Condition {
    private constructor(readonly recipe: Recipe,
                        readonly slot: number,
                        readonly conditions: Condition[],
                        readonly recipeList: Condition[]) {
    }

    static async of(recipes: (Recipe | RecipeRef)[], scanning: Condition[]): Promise<Condition[]> {
        const conditions: Condition[] = [];
        for (const r of recipes) {
            const recipe = r instanceof Recipe ? r : await recipeRefCondition(r);
            const isScanning = recipe instanceof ScanningRecipe;
            const ownConditions = await Condition.of(isScanning ? await recipe.editPreconditions() : [], scanning);
            const recipeList = await Condition.of(await recipe.recipeList(), scanning);
            const condition = new Condition(recipe, isScanning ? scanning.length : -1, ownConditions, recipeList);
            if (isScanning) {
                scanning.push(condition);
            }
            conditions.push(condition);
        }
        return conditions;
    }

    get scanningRecipe(): ScanningRecipe<any> {
        return this.recipe as ScanningRecipe<any>;
    }

    static async allMatch(conditions: Condition[], sourceFile: SourceFile, ctx: ExecutionContext,
                          accumulators: any[]): Promise<boolean> {
        for (const condition of conditions) {
            if (!await condition.matches(sourceFile, ctx, accumulators)) {
                return false;
            }
        }
        return true;
    }

    private async matches(sourceFile: SourceFile, ctx: ExecutionContext, accumulators: any[]): Promise<boolean> {
        if (!await Condition.allMatch(this.conditions, sourceFile, ctx, accumulators)) {
            return false;
        }
        const visitor = this.slot < 0 ?
            await this.recipe.editor() :
            await this.scanningRecipe.editorWithData(accumulators[this.slot]);
        if (await visitor.visit(sourceFile, ctx) !== sourceFile) {
            return true;
        }
        for (const condition of this.recipeList) {
            if (await condition.matches(sourceFile, ctx, accumulators)) {
                return true;
            }
        }
        return false;
    }
}

class GatedVisitor extends TreeVisitor<any, ExecutionContext> {
    constructor(private readonly delegate: TreeVisitor<any, ExecutionContext>,
                private readonly matches: (sourceFile: SourceFile, ctx: ExecutionContext) => Promise<boolean>) {
        super();
    }

    async isAcceptable(sourceFile: SourceFile, ctx: ExecutionContext): Promise<boolean> {
        return await this.matches(sourceFile, ctx) && await this.delegate.isAcceptable(sourceFile, ctx);
    }

    async visit<R extends Tree>(tree: Tree, ctx: ExecutionContext, parent?: Cursor): Promise<R | undefined> {
        if (isSourceFile(tree) && !await this.matches(tree, ctx)) {
            return tree as unknown as R;
        }
        return this.delegate.visit<R>(tree, ctx, parent);
    }
}

class Guarded extends ScanningRecipe<Accumulator> {
    readonly name: string = this.delegate.name;
    readonly displayName: string = this.delegate.displayName;
    readonly description: string = this.delegate.description;
    readonly tags: string[] = this.delegate.tags;
    readonly estimatedEffortPerOccurrence: Minutes = this.delegate.estimatedEffortPerOccurrence;
    readonly causesAnotherCycle: boolean = this.delegate.causesAnotherCycle;
    readonly dataTables: DataTableDescriptor[] = this.delegate.dataTables;

    private readonly key = Symbol("org.openrewrite.editPreconditions.acc");
    private readonly guards: Guarded[];
    private decidedFor?: string;
    private lastMatched = false;

    private constructor(readonly delegate: ScanningRecipe<any>,
                        private readonly conditions: Condition[],
                        private readonly scanningConditions: Condition[],
                        private readonly outerGuards: Guarded[]) {
        super();
        this.guards = [...outerGuards, this];
    }

    static async create(delegate: ScanningRecipe<any>, conditions: (Recipe | RecipeRef)[],
                        outerGuards: Guarded[]): Promise<Guarded> {
        const scanningConditions: Condition[] = [];
        return new Guarded(delegate, await Condition.of(conditions, scanningConditions), scanningConditions,
            outerGuards);
    }

    instanceName(): string {
        return this.delegate.instanceName();
    }

    async descriptor(): Promise<RecipeDescriptor> {
        return this.delegate.descriptor();
    }

    initialValue(ctx: ExecutionContext): Accumulator {
        return {
            delegate: this.delegate.initialValue(ctx),
            conditions: this.scanningConditions.map(c => c.scanningRecipe.initialValue(ctx))
        };
    }

    accumulator(cursor: Cursor, ctx: ExecutionContext): Accumulator {
        const ms = cursor.root.messages;
        if (!ms.has(this.key)) {
            ms.set(this.key, {
                delegate: this.delegate.accumulator(cursor, ctx),
                conditions: this.scanningConditions.map(c => c.scanningRecipe.initialValue(ctx))
            });
        }
        return ms.get(this.key);
    }

    async scanner(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        const scanner = await this.delegate.scanner(acc.delegate);
        if (this.scanningConditions.length === 0) {
            return scanner;
        }
        const conditionScanners = await mapAsync(this.scanningConditions, c =>
            c.scanningRecipe.scanner(acc.conditions[c.slot]));
        return new class extends TreeVisitor<any, ExecutionContext> {
            async visit<R extends Tree>(tree: Tree, ctx: ExecutionContext, parent?: Cursor): Promise<R | undefined> {
                const scanned = await scanner.visit<R>(tree, ctx, parent);
                for (const conditionScanner of conditionScanners) {
                    await conditionScanner.visit(tree, ctx, parent);
                }
                return scanned;
            }
        };
    }

    async generate(acc: Accumulator, ctx: ExecutionContext): Promise<SourceFile[]> {
        return this.delegate.generate(acc.delegate, ctx);
    }

    async editorWithData(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        let decidedFor: string | undefined;
        let matched = false;
        return new GatedVisitor(await this.delegate.editorWithData(acc.delegate), async (sourceFile, ctx) => {
            if (sourceFile.id !== decidedFor) {
                matched = await this.decide(sourceFile, ctx, acc);
                decidedFor = sourceFile.id;
            }
            return matched;
        });
    }

    private async decide(sourceFile: SourceFile, ctx: ExecutionContext, acc: Accumulator): Promise<boolean> {
        this.decidedFor = undefined;
        const matched = allMatch(this.outerGuards, sourceFile) &&
            await Condition.allMatch(this.conditions, sourceFile, suppressDataTables(ctx), acc.conditions);
        this.lastMatched = matched;
        this.decidedFor = sourceFile.id;
        return matched;
    }

    matched(sourceFile: SourceFile): boolean {
        return this.lastMatched && sourceFile.id === this.decidedFor;
    }

    async recipeList(): Promise<Recipe[]> {
        return guardAll(await this.delegate.recipeList(), this.guards);
    }

    async onComplete(ctx: ExecutionContext): Promise<void> {
        await this.delegate.onComplete(ctx);
    }
}

class GatedScanningRecipe extends ScanningRecipe<any> {
    readonly name: string = this.delegate.name;
    readonly displayName: string = this.delegate.displayName;
    readonly description: string = this.delegate.description;
    readonly tags: string[] = this.delegate.tags;
    readonly estimatedEffortPerOccurrence: Minutes = this.delegate.estimatedEffortPerOccurrence;
    readonly causesAnotherCycle: boolean = this.delegate.causesAnotherCycle;
    readonly dataTables: DataTableDescriptor[] = this.delegate.dataTables;

    constructor(readonly delegate: ScanningRecipe<any>, private readonly guards: Guarded[]) {
        super();
    }

    instanceName(): string {
        return this.delegate.instanceName();
    }

    async descriptor(): Promise<RecipeDescriptor> {
        return this.delegate.descriptor();
    }

    initialValue(ctx: ExecutionContext): any {
        return this.delegate.initialValue(ctx);
    }

    accumulator(cursor: Cursor, ctx: ExecutionContext): any {
        return this.delegate.accumulator(cursor, ctx);
    }

    async scanner(acc: any): Promise<TreeVisitor<any, ExecutionContext>> {
        return this.delegate.scanner(acc);
    }

    async generate(acc: any, ctx: ExecutionContext): Promise<SourceFile[]> {
        return this.delegate.generate(acc, ctx);
    }

    async editorWithData(acc: any): Promise<TreeVisitor<any, ExecutionContext>> {
        return new GatedVisitor(await this.delegate.editorWithData(acc), async sourceFile =>
            allMatch(this.guards, sourceFile));
    }

    async recipeList(): Promise<Recipe[]> {
        return guardAll(await this.delegate.recipeList(), this.guards);
    }

    async onComplete(ctx: ExecutionContext): Promise<void> {
        await this.delegate.onComplete(ctx);
    }
}

class GatedRecipe extends Recipe {
    readonly name: string = this.delegate.name;
    readonly displayName: string = this.delegate.displayName;
    readonly description: string = this.delegate.description;
    readonly tags: string[] = this.delegate.tags;
    readonly estimatedEffortPerOccurrence: Minutes = this.delegate.estimatedEffortPerOccurrence;
    readonly causesAnotherCycle: boolean = this.delegate.causesAnotherCycle;
    readonly dataTables: DataTableDescriptor[] = this.delegate.dataTables;

    constructor(readonly delegate: Recipe, private readonly guards: Guarded[]) {
        super();
    }

    instanceName(): string {
        return this.delegate.instanceName();
    }

    async descriptor(): Promise<RecipeDescriptor> {
        return this.delegate.descriptor();
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new GatedVisitor(await this.delegate.editor(), async sourceFile => allMatch(this.guards, sourceFile));
    }

    async recipeList(): Promise<Recipe[]> {
        return guardAll(await this.delegate.recipeList(), this.guards);
    }

    async onComplete(ctx: ExecutionContext): Promise<void> {
        await this.delegate.onComplete(ctx);
    }
}
