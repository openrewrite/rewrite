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
    constructor(private readonly refId: string) {
    }

    static handle(
        connection: rpc.MessageConnection,
        localRefs: ReferenceMap,
        trace: boolean
    ): void {
        connection.onRequest("GetRef", async (request: {refId: string}) => {
            const ref = localRefs.getByRefId(request.refId);
            if (ref === undefined) {
                // Return DELETE + END_OF_OBJECT like Java implementation
                return [
                    {state: RpcObjectState.DELETE, valueType: null, value: null, ref: null, trace: null},
                    {state: RpcObjectState.END_OF_OBJECT, valueType: null, value: null, ref: null, trace: null}
                ];
            }
            
            // Use RpcSendQueue to serialize the object properly like Java does
            const tempRefMap = new ReferenceMap();
            const sendQueue = new RpcSendQueue(tempRefMap, trace);
            const batch = await sendQueue.generate(ref, undefined);
            
            return batch;
        });
    }
}