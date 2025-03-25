import * as rpc from "vscode-jsonrpc/node";
import {MessageConnection} from "vscode-jsonrpc/node";
import {Recipe, RecipeDescriptor, RecipeRegistry, ScanningRecipe} from "../../recipe";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";

export class PrepareRecipe {
    constructor(private readonly id: string, private readonly options?: any) {
    }

    static handle(connection: MessageConnection,
                  preparedRecipes: Map<String, Recipe>) {
        const snowflake = SnowflakeId();
        connection.onRequest(new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("PrepareRecipe"), (params) => {
            const id = snowflake.generate();
            const recipeCtor = RecipeRegistry.all.get(params.id);
            if (!recipeCtor) {
                throw new Error(`Could not find recipe with id ${params.id}`);
            }
            const recipe = new recipeCtor(params.options);
            preparedRecipes.set(id, recipe);
            return {
                id: id,
                descriptor: recipe.descriptor,
                editVisitor: `edit:${id}`,
                scanVisitor: recipe instanceof ScanningRecipe ? `scan:${id}` : undefined
            }
        });
    }
}

export interface PrepareRecipeResponse {
    id: string
    descriptor: RecipeDescriptor
    editVisitor: string
    scanVisitor?: string
}
