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

export class GetRef {
    constructor(private readonly ref: number) {
    }

    static handle(
        connection: rpc.MessageConnection,
        remoteRefs: Map<number, any>,
        localRefs: ReferenceMap,
        batchSize: number,
        trace: boolean
    ): void {
        const pendingData = new Map<number, RpcObjectData[]>();

        connection.onRequest(new rpc.RequestType<GetRef, any, Error>("GetRef"), async request => {
            const ref = localRefs.getByRefId(request.ref);
            if (ref === undefined) {
                // Return DELETE + END_OF_OBJECT like Java implementation
                return [
                    {state: RpcObjectState.DELETE, valueType: null, value: null, ref: null, trace: null},
                    {state: RpcObjectState.END_OF_OBJECT, valueType: null, value: null, ref: null, trace: null}
                ];
            }

            let allData = pendingData.get(request.ref);
            if (!allData) {
                const after = ref;

                // Determine what the remote has cached
                let before = undefined;

                // TODO not quite right as it will now register it as a new ref
                localRefs.deleteByRefId(request.ref);
                allData = await new RpcSendQueue(localRefs, trace).generate(after, before);
                pendingData.set(request.ref, allData);

                localRefs.set(ref, request.ref);
                remoteRefs.set(request.ref, after);
            }

            const batch = allData.splice(0, batchSize);

            // If we've sent all data, remove from pending
            if (allData.length === 0) {
                pendingData.delete(request.ref);
            }

            return batch;
        });
    }
}