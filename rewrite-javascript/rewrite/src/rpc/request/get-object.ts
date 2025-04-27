/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as rpc from "vscode-jsonrpc/node";
import {RpcObjectData, RpcObjectState, RpcSendQueue} from "../queue";

export class GetObject {
    constructor(private readonly id: string) {
    }

    static handle(
        connection: rpc.MessageConnection,
        remoteObjects: Map<string, any>,
        localObjects: Map<string, any>,
        batchSize: number,
        trace: boolean
    ): void {
        const generators = new Map<string, AsyncGenerator<RpcObjectData>>();
        const localRefs = new WeakMap<any, number>();

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
                generator = new RpcSendQueue(localRefs, trace).generate(after, before);
                generators.set(request.id, generator);
            }

            const batch: RpcObjectData[] = [];
            for (let i = 0; i < batchSize; i++) {
                const {value, done} = await generator.next();
                batch.push(value);
                if (done) {
                    break;
                }
            }

            if (batch[batch.length - 1].state === RpcObjectState.END_OF_OBJECT) {
                generators.delete(request.id);
                remoteObjects.set(request.id, after);
            }
            return batch;
        });
    }
}
