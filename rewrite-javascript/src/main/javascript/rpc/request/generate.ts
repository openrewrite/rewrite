import * as rpc from "vscode-jsonrpc/node";
import {Recipe, ScanningRecipe} from "../../recipe";
import {Cursor, rootCursor} from "../../tree";
import {ExecutionContext} from "../../execution";
import {UUID} from "node:crypto";

export class Generate {
    constructor(private readonly id: string, private readonly p: string) {
    }

    static handle(connection: rpc.MessageConnection,
                  localObjects: Map<string, any>,
                  preparedRecipes: Map<String, Recipe>,
                  recipeCursors: WeakMap<Recipe, Cursor>,
                  getObject: (id: string) => any): void {
        connection.onRequest(new rpc.RequestType<Generate, UUID[], Error>("Generate"), async (request) => {
            const recipe = preparedRecipes.get(request.id);
            if (recipe && recipe instanceof ScanningRecipe) {
                let cursor = recipeCursors.get(recipe);
                if (!cursor) {
                    cursor = rootCursor();
                    recipeCursors.set(recipe, cursor);
                }
                const ctx = getObject(request.p) as ExecutionContext;
                const acc = recipe.accumulator(cursor, ctx);
                const generated = await recipe.generate(acc, ctx)
                return generated.map(g => {
                    localObjects.set(g.id.toString(), g);
                    return g.id;
                })
            }
            return []
        });
    }
}
