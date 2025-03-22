import * as rpc from "vscode-jsonrpc/node";
import {Recipe, ScanningRecipe} from "../../recipe";
import {Cursor, rootCursor, Tree} from "../../tree";
import {TreeVisitor} from "../../visitor";

export interface VisitResponse {
    modified: boolean
}

export class Visit {
    private readonly visitor: string
    private readonly visitorOptions: Map<string, any> | undefined
    private readonly treeId: string
    private readonly p: string
    private readonly cursor: string[] | undefined

    constructor(visitor: string, visitorOptions: Map<string, any> | undefined, treeId: string, p: string, cursor: string[] | undefined) {
        this.visitor = visitor
        this.visitorOptions = visitorOptions
        this.treeId = treeId
        this.p = p
        this.cursor = cursor
    }

    static handle(connection: rpc.MessageConnection,
                  localObjects: Map<string, any>,
                  preparedRecipes: Map<String, Recipe>,
                  recipeCursors: WeakMap<Recipe, Cursor>,
                  getObject: (id: string) => any,
                  getCursor: (cursorIds: string[] | undefined) => Promise<Cursor>): void {
        connection.onRequest(new rpc.RequestType<Visit, VisitResponse, Error>("Visit"), async (request) => {
            const p = getObject(request.p);
            const before: Tree = getObject(request.treeId);
            localObjects.set(before.id.toString(), before);

            const visitor = Visit.instantiateVisitor(request, preparedRecipes, recipeCursors, p);
            const after = await visitor.visit(before, p, await getCursor(request.cursor));
            if (!after) {
                localObjects.delete(before.id.toString());
            } else {
                localObjects.set(after.id.toString(), after);
            }

            return {modified: before !== after}
        });
    }

    private static instantiateVisitor(request: Visit,
                                      preparedRecipes: Map<String, Recipe>,
                                      recipeCursors: WeakMap<Recipe, Cursor>,
                                      p: any): TreeVisitor<any, any> {
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
            return recipe.scanner(acc);
        } else if (visitorName.startsWith("edit:")) {
            const recipeKey = visitorName.substring("edit:".length);
            const recipe = preparedRecipes.get(recipeKey) as Recipe;
            if (!recipe) {
                throw new Error(`No editing recipe found for key: ${recipeKey}`);
            }
            return recipe.editor;
        } else {
            return Reflect.construct(
                // "as any" bypasses strict type checking
                (globalThis as any)[visitorName],
                request.visitorOptions ? Array.from(request.visitorOptions.values()) : [])
        }
    }

}
