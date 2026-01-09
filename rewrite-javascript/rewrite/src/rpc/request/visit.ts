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
import * as rpc from "vscode-jsonrpc/node";
import {Recipe, RecipeVisitor, ScanningRecipe} from "../../recipe";
import {Cursor, rootCursor, SourceFile, Tree} from "../../tree";
import {AsyncTreeVisitor} from "../../visitor";
import {ExecutionContext} from "../../execution";
import {withMetrics, extractSourcePath} from "./metrics";

export interface VisitResponse {
    modified: boolean
}

// Tracks the last phase (scan or edit) for each recipe to detect cycle transitions
type RecipePhase = 'scan' | 'edit';
const recipePhases: WeakMap<Recipe, RecipePhase> = new WeakMap();

export class Visit {
    constructor(private readonly visitor: string,
                private readonly sourceFileType: string,
                private readonly visitorOptions: Map<string, any> | undefined,
                private readonly treeId: string,
                private readonly p: string,
                private readonly cursor: string[] | undefined) {
    }

    static handle(connection: rpc.MessageConnection,
                  localObjects: Map<string, any>,
                  preparedRecipes: Map<String, Recipe>,
                  recipeCursors: WeakMap<Recipe, Cursor>,
                  getObject: (id: string, sourceFileType?: string) => any,
                  getCursor: (cursorIds: string[] | undefined, sourceFileType?: string) => Promise<Cursor>,
                  metricsCsv?: string): void {
        connection.onRequest(
            new rpc.RequestType<Visit, VisitResponse, Error>("Visit"),
            withMetrics<Visit, VisitResponse>(
                "Visit",
                metricsCsv,
                (context) => async (request) => {
                    const p = await getObject(request.p, undefined);
                    const before: Tree = await getObject(request.treeId, request.sourceFileType);
                    const cursor = await getCursor(request.cursor, request.sourceFileType);
                    context.target = extractSourcePath(before, cursor);
                    localObjects.set(before.id.toString(), before);

                    const visitor = await Visit.instantiateVisitor(request, preparedRecipes, recipeCursors, p);
                    const after = await visitor.visit(before, p, cursor);
                    if (!after) {
                        localObjects.delete(before.id.toString());
                    } else if (after !== before) {
                        localObjects.set(after.id.toString(), after);
                    }

                    return {modified: before !== after};
                }
            )
        );
    }

    private static async instantiateVisitor(request: Visit,
                                      preparedRecipes: Map<String, Recipe>,
                                      recipeCursors: WeakMap<Recipe, Cursor>,
                                      p: any): Promise<RecipeVisitor<any>> {
        const visitorName = request.visitor;
        if (visitorName.startsWith("scan:")) {
            const recipeKey = visitorName.substring("scan:".length);
            const recipe = preparedRecipes.get(recipeKey) as ScanningRecipe<any>;
            if (!recipe) {
                throw new Error(`No scanning recipe found for key: ${recipeKey}`);
            }
            // If we're transitioning from edit back to scan, this is a new cycle.
            // Clear the cursor so a fresh accumulator is created.
            if (recipePhases.get(recipe) === 'edit') {
                recipeCursors.delete(recipe);
            }
            recipePhases.set(recipe, 'scan');

            let cursor = recipeCursors.get(recipe);
            if (!cursor) {
                cursor = rootCursor();
                recipeCursors.set(recipe, cursor);
            }
            const acc = recipe.accumulator(cursor, p);

            return new class extends AsyncTreeVisitor<any, ExecutionContext> {
                // Delegate isAcceptable to the scanner visitor
                // This ensures we only process source files the scanner can handle
                async isAcceptable(sourceFile: SourceFile, ctx: ExecutionContext): Promise<boolean> {
                    return (await recipe.scanner(acc)).isAcceptable(sourceFile, ctx);
                }

                protected async preVisit(tree: any, ctx: ExecutionContext): Promise<any> {
                    await (await recipe.scanner(acc)).visit(tree, ctx);
                    this.stopAfterPreVisit();
                    return tree;
                }
            }
        } else if (visitorName.startsWith("edit:")) {
            const recipeKey = visitorName.substring("edit:".length);
            const recipe = preparedRecipes.get(recipeKey) as Recipe;
            if (!recipe) {
                throw new Error(`No editing recipe found for key: ${recipeKey}`);
            }
            recipePhases.set(recipe, 'edit');

            // For ScanningRecipe, we need to use the same cursor that was used during scanning
            // to retrieve the accumulator that was stored there
            if (recipe instanceof ScanningRecipe) {
                let cursor = recipeCursors.get(recipe);
                if (!cursor) {
                    cursor = rootCursor();
                    recipeCursors.set(recipe, cursor);
                }
                const acc = recipe.accumulator(cursor, p);
                return recipe.editorWithData(acc);
            }
            return await recipe.editor();
        } else {
            return Reflect.construct(
                // "as any" bypasses strict type checking
                (globalThis as any)[visitorName],
                request.visitorOptions ? Array.from(request.visitorOptions.values()) : [])
        }
    }
}
