import * as rpc from "vscode-jsonrpc/node";
import {Cursor, Tree} from "../../tree";
import {printer} from "../../print";
import {UUID} from "../../uuid";

export class Print {
    constructor(private readonly id: UUID,
                private readonly cursor?: string[]) {
    }

    static handle(connection: rpc.MessageConnection,
                  getObject: (id: string) => any,
                  getCursor: (cursorIds: string[] | undefined) => Promise<Cursor>): void {
        connection.onRequest(new rpc.RequestType<Print, string, Error>("Print"), async request => {
            const tree: Tree = getObject(request.id.toString());
            const cursor = await getCursor(request.cursor);
            return printer(cursor).print(tree)
        });
    }
}
