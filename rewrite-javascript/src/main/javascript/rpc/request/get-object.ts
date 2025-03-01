import * as rpc from "vscode-jsonrpc/node";

export class GetObject {
    id: string

    constructor(id: string) {
        this.id = id
    }

    static handle(connection: rpc.MessageConnection,
                  remoteObjects: Map<string, any>,
                  localObjects: Map<string, any>): void {
        connection.onRequest(new rpc.RequestType<GetObject, any, Error>("GetObject"), (request) => {
            return localObjects.get(request.id);
        });
    }
}
