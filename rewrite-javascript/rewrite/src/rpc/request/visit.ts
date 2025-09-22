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
import {Recipe, ScanningRecipe} from "../../recipe";
import {Cursor, rootCursor, Tree} from "../../tree";
import {TreeVisitor} from "../../visitor";
import {ExecutionContext} from "../../execution";

export interface VisitResponse {
    modified: boolean
}

export class Visit {
    constructor(private readonly visitor: string,
                private readonly visitorOptions: Map<string, any> | undefined,
                private readonly treeId: string,
                private readonly p: string,
                private readonly cursor: string[] | undefined) {
    }

    static handle(connection: rpc.MessageConnection,
                  localObjects: Map<string, any>,
                  preparedRecipes: Map<String, Recipe>,
                  recipeCursors: WeakMap<Recipe, Cursor>,
                  getObject: (id: string) => any,
                  getCursor: (cursorIds: string[] | undefined) => Promise<Cursor>): void {
        connection.onRequest(new rpc.RequestType<Visit, VisitResponse, Error>("Visit"), async (request) => {
            const p = await getObject(request.p);
            const before: Tree = await getObject(request.treeId);
            localObjects.set(before.id.toString(), before);

            const visitor = await Visit.instantiateVisitor(request, preparedRecipes, recipeCursors, p);
            const after = await visitor.visit(before, p, await getCursor(request.cursor));
            if (!after) {
                localObjects.delete(before.id.toString());
            } else if (after !== before) {
                localObjects.set(after.id.toString(), after);
            }

            return {modified: before !== after};
        });
    }

    private static async instantiateVisitor(request: Visit,
                                      preparedRecipes: Map<String, Recipe>,
                                      recipeCursors: WeakMap<Recipe, Cursor>,
                                      p: any): Promise<TreeVisitor<any, any>> {
        const visitorName = request.visitor;
        if (visitorName.startsWith("scan:")) {
            const recipeKey = visitorName.substring("scan:".length);
            const recipe = preparedRecipes.get(recipeKey) as ScanningRecipe<any>;
            if (!recipe) {
                throw new Error(`No scanning recipe found for key: ${recipeKey}`);
            }
            let cursor = recipeCursors.get(recipe);
            if (!cursor) {
                cursor = rootCursor();
                recipeCursors.set(recipe, cursor);
            }
            const acc = recipe.accumulator(cursor, p);
            return new class extends TreeVisitor<any, ExecutionContext> {
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
            return await recipe.editor();
        } else {
            return Reflect.construct(
                // "as any" bypasses strict type checking
                (globalThis as any)[visitorName],
                request.visitorOptions ? Array.from(request.visitorOptions.values()) : [])
        }
    }
}
