import * as rpc from "vscode-jsonrpc/node";
import {MessageConnection} from "vscode-jsonrpc/node";
import {Recipe, RecipeDescriptor} from "../../recipe";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";

export class PrepareRecipe {
    constructor(private readonly id: string, private readonly options?: any) {
    }

    static handle(connection: MessageConnection,
                  preparedRecipes: Map<String, Recipe>) {
        const snowflake = SnowflakeId();
        connection.onRequest(new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("Visit"), (params: any) => {
            const id = snowflake.generate();
            throw new Error("Implement me!");
        });
    }
}

export interface PrepareRecipeResponse {
    id: string
    descriptor: RecipeDescriptor
    editVisitor: string
    scanVisitor?: string
}
