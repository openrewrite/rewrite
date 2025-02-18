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
package org.openrewrite.rpc;

import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.JsonRpcRequest;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.rpc.request.GetObject;
import org.openrewrite.rpc.request.RecipeRpcRequest;
import org.openrewrite.rpc.request.Visit;
import org.openrewrite.rpc.request.VisitResponse;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static io.moderne.jsonrpc.JsonRpcMethod.typed;
import static org.openrewrite.rpc.RpcObjectData.State.END_OF_OBJECT;

public class RewriteRpc {
    private final JsonRpc jsonRpc;
    private int batchSize = 10;
    private Duration timeout = Duration.ofMinutes(1);

    /**
     * Keeps track of the local and remote state of objects that are used in
     * visits and other operations for which incremental state sharing is useful
     * between two processes.
     */
    private final Map<String, Object> remoteObjects = new HashMap<>();
    private final Map<String, Object> localObjects = new HashMap<>();

    /**
     * Keeps track of objects that need to be referentially deduplicated, and
     * the ref IDs to look them up by on the remote.
     */
    private final Map<Object, Integer> localRefs = new IdentityHashMap<>();
    private final Map<Integer, Object> remoteRefs = new IdentityHashMap<>();

    private final Map<String, BlockingQueue<List<RpcObjectData>>> inProgressGetRpcObjects = new ConcurrentHashMap<>();

    private static final ExecutorService forkJoin = ForkJoinPool.commonPool();

    public RewriteRpc(JsonRpc jsonRpc) {
        this.jsonRpc = jsonRpc;

        jsonRpc.method("Visit", typed(Visit.class, request -> {
            Constructor<?> ctor = Class.forName(request.getVisitor()).getDeclaredConstructor();
            ctor.setAccessible(true);

            //noinspection unchecked
            TreeVisitor<Tree, Object> visitor = (TreeVisitor<Tree, Object>) ctor.newInstance();

            SourceFile before = getObject(request.getTreeId());
            localObjects.put(before.getId().toString(), before);

            Object p = getObject(request.getP());
            if (p instanceof ExecutionContext) {
                // This is likely to be reused in subsequent visits, so we keep it.
                localObjects.put(request.getP(), p);
            }

            SourceFile after = (SourceFile) visitor.visit(before, p);
            if (after == null) {
                localObjects.remove(before.getId().toString());
            } else {
                localObjects.put(after.getId().toString(), after);
            }
            return new VisitResponse(before != after);
        }));

        jsonRpc.method("GetObject", typed(GetObject.class, request -> {
            BlockingQueue<List<RpcObjectData>> q = inProgressGetRpcObjects.computeIfAbsent(request.getId(), id -> {
                BlockingQueue<List<RpcObjectData>> batch = new ArrayBlockingQueue<>(1);
                Object after = localObjects.get(id);
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
        }));
        jsonRpc.bind();
    }

    public RewriteRpc batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public RewriteRpc timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public void shutdown() {
        jsonRpc.shutdown();
    }

    public <P> Tree visit(SourceFile sourceFile, String visitorName, P p) {
        VisitResponse response = scan(sourceFile, visitorName, p);
        return response.isModified() ?
                getObject(sourceFile.getId().toString()) :
                sourceFile;
    }

    public <P> VisitResponse scan(SourceFile sourceFile, String visitorName, P p) {
        // Set the local state of this tree, so that when the remote
        // asks for it, we know what to send.
        localObjects.put(sourceFile.getId().toString(), sourceFile);
        String pId = Integer.toString(System.identityHashCode(p));
        localObjects.put(pId, p);

        return send("Visit", new Visit(visitorName, sourceFile.getId().toString(), pId),
                VisitResponse.class);
    }

    private <T> T getObject(String id) {
        RpcReceiveQueue q = new RpcReceiveQueue(remoteRefs, () -> send("GetObject",
                new GetObject(id), RecipeObjectDataBatch.class));
        Object remoteObject = q.receive(localObjects.get(id), before -> {
            if (before instanceof RpcCodec) {
                //noinspection unchecked
                return ((RpcCodec<Object>) before).rpcReceive(before, q);
            }
            return before;
        });
        if (q.take().getState() != END_OF_OBJECT) {
            throw new IllegalStateException("Expected END_OF_OBJECT");
        }
        // We are now in sync with the remote state of the object.
        remoteObjects.put(id, remoteObject);

        //noinspection unchecked
        return (T) remoteObject;
    }

    private <P> P send(String method, RecipeRpcRequest body, Class<P> responseType) {
        try {
            // TODO handle error
            return jsonRpc
                    .send(JsonRpcRequest.newRequest(method)
                            .namedParameters(body)
                            .build())
                    .get(timeout.getSeconds(), TimeUnit.SECONDS)
                    .getResult(responseType);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static class RecipeObjectDataBatch extends ArrayList<RpcObjectData> {
    }
}
