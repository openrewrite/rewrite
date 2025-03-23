import * as rpc from "vscode-jsonrpc/node";
import {Cursor, Tree} from "../../tree";
import {printer} from "../../print";
import {UUID} from "../../uuid";

export class Print {
    id: UUID
    cursor?: string[]

    constructor(id: UUID, cursor?: string[]) {
        this.id = id
        this.cursor = cursor
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
