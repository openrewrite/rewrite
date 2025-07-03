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
import {ReferenceMap} from "../reference";

export class GetObject {
    constructor(private readonly id: string, private readonly lastKnownId?: string) {
    }

    static handle(
        connection: rpc.MessageConnection,
        remoteObjects: Map<string, any>,
        localObjectGenerators: Map<string, (input: string) => any>,
        localObjects: Map<string, any>,
        localRefs: ReferenceMap,
        batchSize: number,
        trace: boolean
    ): void {
        const pendingData = new Map<string, RpcObjectData[]>();

        connection.onRequest(new rpc.RequestType<GetObject, any, Error>("GetObject"), async request => {
            let objId = request.id;
            if (!localObjects.has(objId)) {
                if (localObjectGenerators.has(objId)) {
                    const generator = localObjectGenerators.get(objId)!;
                    let obj = await generator(objId);
                    localObjects.set(objId, obj);
                    localObjectGenerators.delete(request.id);
                } else {
                    return [
                        {state: RpcObjectState.DELETE},
                        {state: RpcObjectState.END_OF_OBJECT}
                    ];
                }
            }

            let allData = pendingData.get(objId);
            if (!allData) {
                const after = localObjects.get(objId);
                
                // Determine what the remote has cached
                let before = undefined;
                if (request.lastKnownId) {
                    before = remoteObjects.get(request.lastKnownId);
                    if (before === undefined) {
                        // Remote had something cached, but we've evicted it - must send full object
                        remoteObjects.delete(request.lastKnownId);
                    }
                }

                allData = await new RpcSendQueue(localRefs, trace).generate(after, before);
                pendingData.set(objId, allData);

                remoteObjects.set(objId, after);
            }

            const batch = allData.splice(0, batchSize);

            // If we've sent all data, remove from pending
            if (allData.length === 0) {
                pendingData.delete(objId);
            }

            return batch;
        });
    }}
