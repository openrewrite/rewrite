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
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcSendQueue;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.openrewrite.rpc.RpcObjectData.State.DELETE;
import static org.openrewrite.rpc.RpcObjectData.State.END_OF_OBJECT;

@Value
public class GetObject implements RpcRequest {
    String id;

    @Nullable
    String sourceFileType;

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<GetObject> {
        // Dedicated pool for tree traversal so GetObject producers can't be starved
        // by unrelated tasks (e.g. repo-level fork-join work) occupying the commonPool.
        private static final ExecutorService TREE_TRAVERSAL_POOL = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "rpc-get-object-traversal");
            t.setDaemon(true);
            return t;
        });

        private final AtomicInteger batchSize;
        private final Map<String, Object> remoteObjects;
        private final Map<String, Object> localObjects;

        /**
         * Keeps track of objects that need to be referentially deduplicated, and
         * the ref IDs to look them up by on the remote.
         */
        private final IdentityHashMap<Object, Integer> localRefs;

        private final AtomicReference<PrintStream> log;
        private final Supplier<Boolean> traceGetObject;

        private final Map<String, BlockingQueue<List<RpcObjectData>>> inProgressGetRpcObjects = new ConcurrentHashMap<>();

        @Override
        protected List<RpcObjectData> handle(GetObject request) throws Exception {
            Object after = localObjects.get(request.getId());

            if (after == null) {
                List<RpcObjectData> deleted = new ArrayList<>(2);
                deleted.add(new RpcObjectData(DELETE, null, null, null, traceGetObject.get()));
                deleted.add(new RpcObjectData(END_OF_OBJECT, null, null, null, traceGetObject.get()));
                return deleted;
            }

            BlockingQueue<List<RpcObjectData>> q = inProgressGetRpcObjects.computeIfAbsent(request.getId(), id -> {
                BlockingQueue<List<RpcObjectData>> batch = new ArrayBlockingQueue<>(1);
                Object before = remoteObjects.get(id);

                RpcSendQueue sendQueue = new RpcSendQueue(batchSize.get(), batch::put, localRefs, request.getSourceFileType(), traceGetObject.get());
                TREE_TRAVERSAL_POOL.submit(() -> {
                    try {
                        sendQueue.send(after, before, null);

                        // All the data has been sent, and the remote should have received
                        // the full tree, so update our understanding of the remote state
                        // of this tree.
                        remoteObjects.put(id, after);
                    } catch (Throwable t) {
                        // Reset our tracking of the remote state so the next interaction
                        // forces a full object sync (ADD) instead of a delta (CHANGE)
                        // against the stale, partially-sent baseline.
                        remoteObjects.remove(id);
                        PrintStream logFile = log.get();
                        //noinspection ConstantValue
                        if (logFile != null) {
                            t.printStackTrace(logFile);
                        }
                    } finally {
                        sendQueue.put(new RpcObjectData(END_OF_OBJECT, null, null, null, traceGetObject.get()));
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
