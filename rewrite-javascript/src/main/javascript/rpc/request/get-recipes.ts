import * as rpc from "vscode-jsonrpc/node";
import {RecipeDescriptor, RecipeRegistry} from "../../recipe";

export class GetRecipes {
    static handle(connection: rpc.MessageConnection): void {
        connection.onRequest(new rpc.RequestType0<({ name: string } & RecipeDescriptor)[], Error>("GetRecipes"), () => {
            const recipes = [];
            for (const [name, recipe] of RecipeRegistry.all.entries()) {
                recipes.push({
                    name: name,
                    ...new recipe().descriptor
                });
            }
            return recipes;
        });
    }
}
