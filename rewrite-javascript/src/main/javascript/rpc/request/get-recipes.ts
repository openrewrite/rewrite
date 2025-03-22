import * as rpc from "vscode-jsonrpc/node";
import {Recipe} from "../../recipe";

export function getRecipes(connection: rpc.MessageConnection): void {
    connection.onRequest(new rpc.RequestType0<Recipe[], Error>("GetRecipes"), () => {
        // TODO implement me!
        return []
    });
}
