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

import io.moderne.jsonrpc.JsonRpcMethod;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.openrewrite.rpc.RpcObjectData.State.DELETE;
import static org.openrewrite.rpc.RpcObjectData.State.END_OF_OBJECT;

@Value
public class GetObject implements RpcRequest {
    String id;

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<GetObject> {
        private static final ExecutorService forkJoin = ForkJoinPool.commonPool();

        private final int batchSize;
        private final Map<String, Object> remoteObjects;
        private final Map<String, Object> localObjects;

        private final Map<String, BlockingQueue<List<RpcObjectData>>> inProgressGetRpcObjects = new ConcurrentHashMap<>();

        /**
         * Keeps track of objects that need to be referentially deduplicated, and
         * the ref IDs to look them up by on the remote.
         */
        private final Map<Object, Integer> localRefs = new IdentityHashMap<>();

        @Override
        protected Object handle(GetObject request) throws Exception {
            Object after = localObjects.get(request.getId());

            if (after == null) {
                List<RpcObjectData> deleted = new ArrayList<>(2);
                deleted.add(new RpcObjectData(DELETE, null, null, null));
                deleted.add(new RpcObjectData(END_OF_OBJECT, null, null, null));
                return deleted;
            }

            BlockingQueue<List<RpcObjectData>> q = inProgressGetRpcObjects.computeIfAbsent(request.getId(), id -> {
                BlockingQueue<List<RpcObjectData>> batch = new ArrayBlockingQueue<>(1);
                Object before = remoteObjects.get(id);

                RpcSendQueue sendQueue = new RpcSendQueue(batchSize, batch::put, localRefs);
                forkJoin.submit(() -> {
                    try {
                        Runnable onChange = after instanceof RpcCodec ? () -> {
                            //noinspection unchecked
                            ((RpcCodec<Object>) after).rpcSend(after, sendQueue);
                        } : null;
                        sendQueue.send(after, before, onChange);

                        // All the data has been sent, and the remote should have received
                        // the full tree, so update our understanding of the remote state
                        // of this tree.
                        remoteObjects.put(id, after);
                    } catch (Throwable ignored) {
                        // TODO do something with this exception
                    } finally {
                        sendQueue.put(new RpcObjectData(END_OF_OBJECT, null, null, null));
                        sendQueue.flush();
                    }
                    return 0;
                });
                return batch;
            });

            List<RpcObjectData> batch = q.take();
            if (batch.get(batch.size() - 1).getState() == END_OF_OBJECT) {
                inProgressGetRpcObjects.remove(request.getId());
            }

            return batch;
        }
    }
}
