import * as rpc from "vscode-jsonrpc/node";
import {Cursor, isSourceFile, Tree} from "../../tree";
import {printer} from "../../print";
import {UUID} from "../../uuid";

export class Print {
    constructor(private readonly treeId: UUID, private readonly cursor?: string[]) {
    }

    static handle(connection: rpc.MessageConnection,
                  getObject: (id: string) => any,
                  getCursor: (cursorIds: string[] | undefined) => Promise<Cursor>): void {
        connection.onRequest(new rpc.RequestType<Print, string, Error>("Print"), async request => {
            try {
                const tree: Tree = await getObject(request.treeId.toString());
                if (isSourceFile(tree)) {
                    return printer(tree).print(tree);
                } else {
                    const cursor = await getCursor(request.cursor);
                    return printer(cursor).print(tree)
                }
            } catch (e: any) {
                console.log(e.stack);
                throw e;
            }
        });
    }
}
