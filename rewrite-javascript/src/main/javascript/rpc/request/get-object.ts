import * as rpc from "vscode-jsonrpc/node";
import {RpcObjectData, RpcObjectState, RpcSendQueue} from "../queue";

export class GetObject {
    id: string;

    constructor(id: string) {
        this.id = id;
    }

    static handle(
        connection: rpc.MessageConnection,
        remoteObjects: Map<string, any>,
        localObjects: Map<string, any>,
        batchSize: number
    ): void {
        const generators = new Map<string, AsyncGenerator<RpcObjectData>>();
        const localRefs = new Map<any, number>();

        connection.onRequest(new rpc.RequestType<GetObject, any, Error>("GetObject"), async (request) => {
            if (!localObjects.has(request.id)) {
                return [
                    {state: RpcObjectState.DELETE},
                    {state: RpcObjectState.END_OF_OBJECT}
                ];
            }

            const after = localObjects.get(request.id);
            const before = remoteObjects.get(request.id);

            let generator = generators.get(request.id);
            if (!generator) {
                generator = new RpcSendQueue(localRefs).generate(after, before);
                generators.set(request.id, generator);
            }

            const batch: RpcObjectData[] = [];
            for (let i = 0; i < batchSize; i++) {
                const {value, done} = await generator.next();
                if (done) {
                    break;
                }
                batch.push(value);
            }

            if (batch[-1].state === RpcObjectState.END_OF_OBJECT) {
                generators.delete(request.id);
                remoteObjects.set(request.id, after);
            }
            return batch;
        });
    }
}
