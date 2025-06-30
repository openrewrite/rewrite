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
import lombok.Value;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.openrewrite.rpc.RpcObjectData.State.DELETE;
import static org.openrewrite.rpc.RpcObjectData.State.END_OF_OBJECT;

@Value
public class GetRef implements RpcRequest {
    int ref;

    @RequiredArgsConstructor
    public static class Handler extends io.moderne.jsonrpc.JsonRpcMethod<GetRef> {
        private static final ExecutorService forkJoin = ForkJoinPool.commonPool();

        private final Map<Integer, Object> remoteRefs;
        private final IdentityHashMap<Object, Integer> localRefs;
        private final AtomicInteger batchSize;
        private final AtomicBoolean trace;

        private final Map<Integer, BlockingQueue<List<RpcObjectData>>> inProgress = new ConcurrentHashMap<>();

        @Override
        public List<RpcObjectData> handle(GetRef request) throws InterruptedException {
            Integer refId = request.getRef();
            Object after = localRefs.entrySet().stream()
                    .filter(e -> e.getValue().equals(refId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (after == null) {
                List<RpcObjectData> deleted = new ArrayList<>(2);
                deleted.add(new RpcObjectData(DELETE, null, null, null, null));
                deleted.add(new RpcObjectData(END_OF_OBJECT, null, null, null, null));
                return deleted;
            }

            BlockingQueue<List<RpcObjectData>> q = inProgress.computeIfAbsent(request.getRef(), id -> {
                BlockingQueue<List<RpcObjectData>> batch = new ArrayBlockingQueue<>(1);

                // TODO not quite right as it will now register it as a new ref
                localRefs.remove(after);
                RpcSendQueue sendQueue = new RpcSendQueue(batchSize.get(), batch::put, localRefs, trace.get());
                forkJoin.submit(() -> {
                    try {
                        sendQueue.send(after, null, null);

                        // All the data has been sent, and the remote should have received
                        // the full tree, so update our understanding of the remote state
                        // of this tree.
                        remoteRefs.put(id, after);
                    } catch (Throwable ignored) {
                        // TODO do something with this exception
                    } finally {
                        localRefs.put(after, id);
                        sendQueue.put(new RpcObjectData(END_OF_OBJECT, null, null, null, null));
                        sendQueue.flush();
                    }
                    return 0;
                });
                return batch;
            });

            List<RpcObjectData> batch = q.take();
            if (batch.get(batch.size() - 1).getState() == END_OF_OBJECT) {
                inProgress.remove(request.getRef());
            }

            return batch;
        }
    }
}
