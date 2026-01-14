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
import {RpcObjectState, RpcRawMessage, RpcSendQueue} from "../queue";
import {ReferenceMap} from "../../reference";
import {extractSourcePath, withMetrics} from "./metrics";

interface PendingObjectData {
    data: RpcRawMessage[];
    index: number;
}

export class GetObject {
    constructor(private readonly id: string,
                private readonly sourceFileType?: string) {
    }

    static handle(
        connection: rpc.MessageConnection,
        remoteObjects: Map<string, any>,
        localObjects: Map<string, any | ((input: string) => any)>,
        localRefs: ReferenceMap,
        batchSize: number,
        trace: () => boolean,
        metricsCsv?: string,
    ): void {
        const pendingData = new Map<string, PendingObjectData>();

        connection.onRequest(
            new rpc.RequestType<GetObject, any, Error>("GetObject"),
            withMetrics<GetObject, any>(
                "GetObject",
                metricsCsv,
                (context) => async request => {
                    const objId = request.id;
                    if (!localObjects.has(objId)) {
                        context.target = '';
                        // Return compact array format for "not found" case
                        return [
                            [RpcObjectState.DELETE, null, null],
                            [RpcObjectState.END_OF_OBJECT, null, null]
                        ] as RpcRawMessage[];
                    }

                    const objectOrGenerator = localObjects.get(objId)!;
                    if (typeof objectOrGenerator === 'function') {
                        const obj = await objectOrGenerator(objId);
                        localObjects.set(objId, obj);
                    }

                    const obj = localObjects.get(objId);
                    context.target = extractSourcePath(obj);

                    let pending = pendingData.get(objId);
                    if (!pending) {
                        const after = obj;
                        const before = remoteObjects.get(objId);

                        const data = await new RpcSendQueue(localRefs, request.sourceFileType, trace())
                            .generate(after, before);
                        pending = {data, index: 0};
                        pendingData.set(objId, pending);

                        remoteObjects.set(objId, after);
                    }

                    // Use index-based slicing instead of splice to avoid O(n²) behavior
                    const startIndex = pending.index;
                    const endIndex = Math.min(startIndex + batchSize, pending.data.length);
                    const batch = pending.data.slice(startIndex, endIndex);
                    pending.index = endIndex;

                    // If we've sent all data, remove from pending
                    if (pending.index >= pending.data.length) {
                        pendingData.delete(objId);
                    }

                    return batch;
                }
            )
        );
    }
}
