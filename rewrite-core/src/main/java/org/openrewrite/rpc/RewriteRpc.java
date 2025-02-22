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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.JsonRpcRequest;
import io.moderne.jsonrpc.internal.SnowflakeId;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.ObjectMappers;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.rpc.request.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static io.moderne.jsonrpc.JsonRpcMethod.typed;
import static java.util.Collections.emptyList;
import static org.openrewrite.rpc.RpcObjectData.State.DELETE;
import static org.openrewrite.rpc.RpcObjectData.State.END_OF_OBJECT;

public class RewriteRpc {
    private final ObjectMapper mapper = ObjectMappers.propertyBasedMapper(null);

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

    private final Map<String, Recipe> preparedRecipes = new HashMap<>();

    private final Map<String, BlockingQueue<List<RpcObjectData>>> inProgressGetRpcObjects = new ConcurrentHashMap<>();

    private static final ExecutorService forkJoin = ForkJoinPool.commonPool();

    public RewriteRpc(JsonRpc jsonRpc, Environment marketplace) {
        this.jsonRpc = jsonRpc;

        jsonRpc.method("Visit", typed(Visit.class, request -> {
            //noinspection unchecked
            TreeVisitor<?, Object> visitor = (TreeVisitor<?, Object>) instantiateVisitor(request);

            SourceFile before = getObject(request.getTreeId());
            localObjects.put(before.getId().toString(), before);

            Object p = getObject(request.getP());
            if (p instanceof ExecutionContext) {
                // This is likely to be reused in subsequent visits, so we keep it.
                localObjects.put(request.getP(), p);
            }

            SourceFile after = (SourceFile) visitor.visit(before, p,
                    getCursor(request.getCursor()));
            if (after == null) {
                localObjects.remove(before.getId().toString());
            } else {
                localObjects.put(after.getId().toString(), after);
            }
            return new VisitResponse(before != after);
        }));

        jsonRpc.method("Generate", typed(Generate.class, request -> {
            // TODO implement me!
            return emptyList();
        }));

        jsonRpc.method("GetObject", typed(GetObject.class, request -> {
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
        }));

        jsonRpc.method("GetRecipes", typed(Map.class, request ->
                marketplace.listRecipeDescriptors()));

        jsonRpc.method("PrepareRecipe", typed(PrepareRecipe.class, request -> {
            Recipe recipe = new RecipeLoader(null).load(request.getId(), request.getOptions());
            String instanceId = SnowflakeId.generateId();
            preparedRecipes.put(instanceId, recipe);
            return new PrepareRecipeResponse(instanceId, recipe.getDescriptor(),
                    "edit:" + instanceId,
                    recipe instanceof ScanningRecipe ? "scan:" + instanceId : null);
        }));

        jsonRpc.method("Print", typed(Print.class, request -> {
            Tree tree = getObject(request.getId());
            Cursor cursor = getCursor(request.getCursor());
            return tree.print(new Cursor(cursor, tree));
        }));

        jsonRpc.bind();
    }

    private TreeVisitor<?, ?> instantiateVisitor(Visit request) {
        String visitorName = request.getVisitor();

        if (visitorName.startsWith("scan:")) {
            //noinspection unchecked
            return ((ScanningRecipe<Object>) preparedRecipes.get(visitorName.substring("scan:".length() + 1)))
                    .getScanner(0);
        } else if (visitorName.startsWith("edit:")) {
            return preparedRecipes.get(visitorName.substring("edit:".length() + 1))
                    .getVisitor();
        }

        Map<Object, Object> withJsonType = request.getVisitorOptions() == null ?
                new HashMap<>() :
                new HashMap<>(request.getVisitorOptions());
        withJsonType.put("@c", visitorName);
        return mapper.convertValue(withJsonType, new TypeReference<TreeVisitor<Tree, Object>>() {
        });
    }

    private Cursor getCursor(@Nullable List<String> cursorIds) {
        Cursor cursor = new Cursor(null, Cursor.ROOT_VALUE);
        if (cursorIds != null) {
            for (String cursorId : cursorIds) {
                Object cursorObject = getObject(cursorId);
                remoteObjects.put(cursorId, cursorObject);
                cursor = new Cursor(cursor, cursorObject);
            }
        }
        return cursor;
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

    public <P> @Nullable Tree visit(SourceFile sourceFile, String visitorName, P p) {
        return visit(sourceFile, visitorName, p, null);
    }

    public <P> @Nullable Tree visit(SourceFile sourceFile, String visitorName, P p, @Nullable Cursor cursor) {
        VisitResponse response = scan(sourceFile, visitorName, p, cursor);
        return response.isModified() ?
                getObject(sourceFile.getId().toString()) :
                sourceFile;
    }

    public <P> VisitResponse scan(SourceFile sourceFile, String visitorName, P p) {
        return scan(sourceFile, visitorName, p, null);
    }

    public <P> VisitResponse scan(SourceFile sourceFile, String visitorName, P p,
                                  @Nullable Cursor cursor) {
        // Set the local state of this tree, so that when the remote
        // asks for it, we know what to send.
        localObjects.put(sourceFile.getId().toString(), sourceFile);
        String pId = Integer.toString(System.identityHashCode(p));
        localObjects.put(pId, p);

        List<String> cursorIds = getCursorIds(cursor);

        return send("Visit", new Visit(visitorName, null, sourceFile.getId().toString(), pId, cursorIds),
                VisitResponse.class);
    }

    public Collection<? extends SourceFile> generate(String remoteRecipeId) {
        List<String> generated = send("Generate", new Generate(remoteRecipeId), GenerateResponse.class);
        if (!generated.isEmpty()) {
            return generated.stream()
                    .map(this::<SourceFile>getObject)
                    .collect(Collectors.toList());
        }
        return emptyList();
    }

    public List<RecipeDescriptor> getRecipes() {
        return send("GetRecipes", new GetRecipes(), GetRecipesResponse.class);
    }

    public Recipe prepareRecipe(String id, Map<String, Object> options) {
        PrepareRecipeResponse r = send("PrepareRecipe", new PrepareRecipe(id, options), PrepareRecipeResponse.class);
        return new RpcRecipe(this, r.getId(), r.getDescriptor(), r.getEditVisitor(), r.getScanVisitor());
    }

    public String print(SourceFile tree) {
        return print(tree, new Cursor(null, Cursor.ROOT_VALUE));
    }

    public String print(Tree tree, Cursor parent) {
        localObjects.put(tree.getId().toString(), tree);
        return send("Print", new Print(tree.getId().toString(), getCursorIds(parent)), String.class);
    }

    private @Nullable List<String> getCursorIds(@Nullable Cursor cursor) {
        List<String> cursorIds = null;
        if (cursor != null) {
            cursorIds = cursor.getPathAsStream().map(c -> {
                String id = c instanceof Tree ?
                        ((Tree) c).getId().toString()
                        : Integer.toString(System.identityHashCode(c));
                localObjects.put(id, c);
                return id;
            }).collect(Collectors.toList());
        }
        return cursorIds;
    }

    private <T> T getObject(String id) {
        RpcReceiveQueue q = new RpcReceiveQueue(remoteRefs, () -> send("GetObject",
                new GetObject(id), GetObjectResponse.class));
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

    private <P> P send(String method, RpcRequest body, Class<P> responseType) {
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
}
