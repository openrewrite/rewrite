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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static org.openrewrite.rpc.RpcObjectData.State.DELETE;
import static org.openrewrite.rpc.RpcObjectData.State.END_OF_OBJECT;

@Value
public class GetObject implements RpcRequest {
    String id;

    @Nullable
    String sourceFileType;

    /**
     * An action for the handler to perform instead of a normal data transfer.
     * When null, this is a normal data-transfer request.
     * <p>
     * Supported actions:
     * <ul>
     *   <li>"revert" — sent by the receiver after a deserialization failure.
     *       The handler reverts both {@code remoteObjects} and {@code localObjects}
     *       for this ID to the pre-transfer state.</li>
     * </ul>
     */
    @Nullable
    String action;

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<GetObject> {
        private static final ExecutorService forkJoin = ForkJoinPool.commonPool();

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

        @RequiredArgsConstructor
        private static class InProgressSend {
            final BlockingQueue<List<RpcObjectData>> queue;
            final @Nullable Object before;
            final AtomicBoolean cancelled;
        }

        private final Map<String, InProgressSend> inProgressGetRpcObjects = new ConcurrentHashMap<>();

        /**
         * Stores the pre-transfer {@code remoteObjects} value for each in-flight
         * or recently completed transfer. Used by the "revert" action to restore
         * both {@code remoteObjects} and {@code localObjects} to the state before
         * the transfer started.
         */
        private final Map<String, @Nullable Object> actionBaseline = new HashMap<>();

        @Override
        protected List<RpcObjectData> handle(GetObject request) throws Exception {
            String action = request.getAction();
            if (action != null) {
                if ("revert".equals(action)) {
                    String id = request.getId();
                    InProgressSend stale = inProgressGetRpcObjects.remove(id);
                    if (stale != null) {
                        stale.cancelled.set(true);
                    }
                    if (actionBaseline.containsKey(id)) {
                        Object before = actionBaseline.remove(id);
                        if (before != null) {
                            remoteObjects.put(id, before);
                            localObjects.put(id, before);
                        } else {
                            remoteObjects.remove(id);
                            localObjects.remove(id);
                        }
                    }
                }
                return emptyList();
            }

            Object after = localObjects.get(request.getId());

            if (after == null) {
                // Clean up any stale in-progress send for this ID
                InProgressSend stale = inProgressGetRpcObjects.remove(request.getId());
                if (stale != null) {
                    stale.cancelled.set(true);
                }

                List<RpcObjectData> deleted = new ArrayList<>(2);
                deleted.add(new RpcObjectData(DELETE, null, null, null, traceGetObject.get()));
                deleted.add(new RpcObjectData(END_OF_OBJECT, null, null, null, traceGetObject.get()));
                return deleted;
            }

            Object currentBefore = remoteObjects.get(request.getId());

            InProgressSend inProgress = inProgressGetRpcObjects.computeIfAbsent(request.getId(), id -> {
                // Save the pre-transfer baseline for potential revert
                actionBaseline.put(id, currentBefore);

                BlockingQueue<List<RpcObjectData>> batch = new ArrayBlockingQueue<>(1);
                AtomicBoolean cancelled = new AtomicBoolean(false);

                RpcSendQueue sendQueue = new RpcSendQueue(batchSize.get(), batch::put, localRefs, request.getSourceFileType(), traceGetObject.get());
                forkJoin.submit(() -> {
                    try {
                        sendQueue.send(after, currentBefore, null);

                        // Optimistically update remoteObjects — the receiver is
                        // expected to send action="revert" if deserialization fails,
                        // which will roll this back.
                        if (!cancelled.get()) {
                            remoteObjects.put(id, after);
                        }
                    } catch (Throwable t) {
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
                return new InProgressSend(batch, currentBefore, cancelled);
            });

            List<RpcObjectData> batch = inProgress.queue.take();
            if (batch.get(batch.size() - 1).getState() == END_OF_OBJECT) {
                inProgressGetRpcObjects.remove(request.getId());
            }

            return batch;
        }
    }
}
