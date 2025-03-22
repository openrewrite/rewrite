import * as rpc from "vscode-jsonrpc/node";
import {MessageConnection} from "vscode-jsonrpc/node";
import {Recipe} from "../../recipe";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";

export class PrepareRecipe {
    id: string
    options: Map<string, any>

    constructor(id: string, options: Map<string, any>) {
        this.id = id;
        this.options = options;
    }

    static handle(connection: MessageConnection,
                  preparedRecipes: Map<String, Recipe>) {
        const snowflake = SnowflakeId();
        connection.onRequest(new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("Visit"), (params: any) => {
            // TODO implement me!
            const recipe = null
            const id = snowflake.generate();


            return null;
        });
    }
}

export interface PrepareRecipeResponse {
    id: string
    // FIXME create RecipeDescriptor
    descriptor: string//RecipeDescriptor
    editVisitor: string
    scanVisitor: string | null
}
