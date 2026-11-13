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

import static java.util.Collections.singletonList;
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

        private final Map<String, Exchange> inProgressGetRpcObjects = new ConcurrentHashMap<>();

        /**
         * Kept after its last batch is handed over, since the receiver can still fail on that batch.
         */
        private volatile @Nullable Exchange last;

        @RequiredArgsConstructor
        private static class Exchange {
            final String id;

            /**
             * The number of refs before this exchange. Refs are numbered in sequence, so
             * every one above it was assigned by this exchange.
             */
            final int refCheckpoint;

            final BlockingQueue<List<RpcObjectData>> batches = new ArrayBlockingQueue<>(1);

            /**
             * Written before {@code END_OF_OBJECT} is queued, and {@link #batches}
             * orders that put against the take of the batch carrying it, so whoever
             * takes the final batch sees this.
             */
            volatile @Nullable Throwable failure;

            volatile boolean cancelled;

            private final CountDownLatch finished = new CountDownLatch(1);

            private @Nullable Thread producer;

            void put(List<RpcObjectData> batch) throws InterruptedException {
                if (cancelled) {
                    throw new CancellationException();
                }
                batches.put(batch);
            }

            synchronized void producing() {
                producer = Thread.currentThread();
            }

            synchronized void produced() {
                producer = null;
                // Clear an interrupt from cancel() so it can't reach the pool thread's next task.
                //noinspection ResultOfMethodCallIgnored
                Thread.interrupted();
                finished.countDown();
            }

            /**
             * Stop the traversal and leave a final batch that fails the transfer, so a receiver
             * still pulling gets an error rather than the rest of an object the sender has
             * already forgotten sending.
             */
            void cancel(boolean trace) {
                synchronized (this) {
                    cancelled = true;
                    if (producer != null) {
                        producer.interrupt();
                    }
                }
                boolean interrupted = false;
                while (true) {
                    try {
                        finished.await();
                        break;
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                failure = new CancellationException("The transfer of " + id + " was cancelled");
                batches.clear();
                batches.add(singletonList(new RpcObjectData(END_OF_OBJECT, null, null, null, trace)));
            }
        }

        @Override
        protected List<RpcObjectData> handle(GetObject request) throws Exception {
            Exchange exchange;
            synchronized (this) {
                exchange = inProgressGetRpcObjects.get(request.getId());
                if (exchange == null) {
                    Object after = localObjects.get(request.getId());
                    if (after == null) {
                        List<RpcObjectData> deleted = new ArrayList<>(2);
                        deleted.add(new RpcObjectData(DELETE, null, null, null, traceGetObject.get()));
                        deleted.add(new RpcObjectData(END_OF_OBJECT, null, null, null, traceGetObject.get()));
                        return deleted;
                    }
                    exchange = start(request, after);
                    inProgressGetRpcObjects.put(request.getId(), exchange);
                }
            }

            List<RpcObjectData> batch = exchange.batches.take();
            if (batch.get(batch.size() - 1).getState() == END_OF_OBJECT) {
                inProgressGetRpcObjects.remove(request.getId(), exchange);
                Throwable failure = exchange.failure;
                if (failure != null) {
                    // The JSON-RPC layer turns only an Exception into an error response, so
                    // the wrapper is what keeps an Error from leaving the peer's request
                    // unanswered until it times out.
                    throw new IllegalStateException("Failed to send object " + request.getId() +
                            (request.getSourceFileType() == null ? "" : " (type: " + request.getSourceFileType() + ")") +
                            ": " + failure, failure);
                }
            }

            return batch;
        }

        private Exchange start(GetObject request, Object after) {
            String id = request.getId();
            Exchange e = new Exchange(id, localRefs.size());
            last = e;
            Object before = remoteObjects.get(id);

            RpcSendQueue sendQueue = new RpcSendQueue(batchSize.get(), e::put, localRefs, request.getSourceFileType(), traceGetObject.get());
            TREE_TRAVERSAL_POOL.submit(() -> {
                e.producing();
                try {
                    sendQueue.send(after, before, null);

                    // All the data has been sent, and the remote should have received
                    // the full tree, so update our understanding of the remote state
                    // of this tree.
                    remoteObjects.put(id, after);
                } catch (Throwable t) {
                    e.failure = t;
                    rollBack(e);

                    PrintStream logFile = log.get();
                    //noinspection ConstantValue
                    if (logFile != null && !e.cancelled) {
                        t.printStackTrace(logFile);
                    }
                } finally {
                    try {
                        sendQueue.put(new RpcObjectData(END_OF_OBJECT, null, null, null, traceGetObject.get()));
                        sendQueue.flush();
                    } finally {
                        e.produced();
                    }
                }
                return 0;
            });
            return e;
        }

        /**
         * Undo the transfer of an object that its receiver failed to take.
         *
         * @see AbortGetObject
         */
        public synchronized void abort(String id) throws InterruptedException {
            Exchange exchange = last;
            if (exchange == null || !exchange.id.equals(id)) {
                // Not the latest transfer, so which refs it assigned is no longer known.
                remoteObjects.remove(id);
                localRefs.clear();
                return;
            }
            if (exchange.cancelled) {
                // Already rolled back; the receiver won't take the batch that fails it either.
                inProgressGetRpcObjects.remove(id, exchange);
                return;
            }
            if (inProgressGetRpcObjects.remove(id, exchange)) {
                // The traversal is waiting to hand over batches nobody will ask for.
                List<RpcObjectData> batch;
                do {
                    batch = exchange.batches.take();
                } while (batch.get(batch.size() - 1).getState() != END_OF_OBJECT);
            }
            rollBack(exchange);
        }

        /**
         * Cancel every transfer still in progress and roll back what each had sent, then run
         * {@code withNoneInProgress} before another can start. A transfer assigns refs from its
         * own thread, so anything else that rolls refs back must not overlap one.
         */
        public synchronized void cancelInProgress(Runnable withNoneInProgress) {
            for (Exchange exchange : inProgressGetRpcObjects.values()) {
                if (!exchange.cancelled) {
                    exchange.cancel(traceGetObject.get());
                    rollBack(exchange);
                }
            }
            withNoneInProgress.run();
        }

        /**
         * Forget that the remote holds the object and the refs assigned while sending it, so that
         * the next transfer sends both whole instead of a delta against a baseline, or a bare ref
         * to an object, that the remote never received.
         */
        private void rollBack(Exchange exchange) {
            remoteObjects.remove(exchange.id);
            localRefs.values().removeIf(ref -> ref > exchange.refCheckpoint);
        }
    }
}
