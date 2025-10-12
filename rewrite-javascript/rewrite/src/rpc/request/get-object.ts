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
import {ReferenceMap} from "../../reference";
import {extractSourcePath, withMetrics} from "./metrics";

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
        const pendingData = new Map<string, RpcObjectData[]>();

        connection.onRequest(
            new rpc.RequestType<GetObject, any, Error>("GetObject"),
            withMetrics<GetObject, any>(
                "GetObject",
                metricsCsv,
                (context) => async request => {
                    const objId = request.id;
                    if (!localObjects.has(objId)) {
                        context.target = '';
                        return [
                            {state: RpcObjectState.DELETE},
                            {state: RpcObjectState.END_OF_OBJECT}
                        ];
                    }

                    const objectOrGenerator = localObjects.get(objId)!;
                    if (typeof objectOrGenerator === 'function') {
                        const obj = await objectOrGenerator(objId);
                        localObjects.set(objId, obj);
                    }

                    const obj = localObjects.get(objId);
                    context.target = extractSourcePath(obj);

                    let allData = pendingData.get(objId);
                    if (!allData) {
                        const after = obj;
                        const before = remoteObjects.get(objId);

                        allData = await new RpcSendQueue(localRefs, request.sourceFileType, trace())
                            .generate(after, before);
                        pendingData.set(objId, allData);

                        remoteObjects.set(objId, after);
                    }

                    const batch = allData.splice(0, batchSize);

                    // If we've sent all data, remove from pending
                    if (allData.length === 0) {
                        pendingData.delete(objId);
                    }

                    return batch;
                }
            )
        );
    }
}
