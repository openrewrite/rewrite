/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.rpc.request;

import lombok.RequiredArgsConstructor;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class GetRef implements RpcRequest {
    private final String refId;

    public GetRef(String refId) {
        this.refId = refId;
    }

    public String getRefId() {
        return refId;
    }

    @RequiredArgsConstructor
    public static class Handler extends io.moderne.jsonrpc.JsonRpcMethod<GetRef> {
        private final Map<Object, Integer> localRefs;

        @Override
        public List<RpcObjectData> handle(GetRef request) {
            try {
                Integer refIdInt = Integer.parseInt(request.getRefId());
                Object refObject = localRefs.entrySet().stream()
                        .filter(e -> e.getValue().equals(refIdInt))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
                
                if (refObject == null) {
                    // Return DELETE + END_OF_OBJECT like GetObject does
                    List<RpcObjectData> result = new ArrayList<>(2);
                    result.add(new RpcObjectData(RpcObjectData.State.DELETE, null, null, null, null));
                    result.add(new RpcObjectData(RpcObjectData.State.END_OF_OBJECT, null, null, null, null));
                    return result;
                }
                
                // Use RpcSendQueue to serialize the object properly with batching
                List<RpcObjectData> batch = new ArrayList<>();
                RpcSendQueue sendQueue = new RpcSendQueue(1, batch::addAll, new IdentityHashMap<>(), false);
                sendQueue.send(refObject, null, null);
                sendQueue.put(new RpcObjectData(RpcObjectData.State.END_OF_OBJECT, null, null, null, null));
                sendQueue.flush();
                
                return batch;
            } catch (NumberFormatException e) {
                // Return DELETE + END_OF_OBJECT for invalid refId
                List<RpcObjectData> result = new ArrayList<>(2);
                result.add(new RpcObjectData(RpcObjectData.State.DELETE, null, null, null, null));
                result.add(new RpcObjectData(RpcObjectData.State.END_OF_OBJECT, null, null, null, null));
                return result;
            }
        }
    }
}