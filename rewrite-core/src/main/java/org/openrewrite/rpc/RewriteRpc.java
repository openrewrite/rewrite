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
import io.moderne.jsonrpc.JsonRpcMethod;
import io.moderne.jsonrpc.JsonRpcRequest;
import io.moderne.jsonrpc.internal.SnowflakeId;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.rpc.request.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
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

    @VisibleForTesting
    final Map<String, Object> localObjects = new HashMap<>();

    /* A reverse map of the objects back to their IDs */
    final Map<Object, String> localObjectIds = new IdentityHashMap<>();

    private final Map<Integer, Object> remoteRefs = new IdentityHashMap<>();

    public RewriteRpc(JsonRpc jsonRpc, Environment marketplace) {
        this.jsonRpc = jsonRpc;

        Map<String, Recipe> preparedRecipes = new HashMap<>();
        Map<Recipe, Cursor> recipeCursors = new IdentityHashMap<>();

        jsonRpc.rpc("Visit", new Visit.Handler(localObjects, preparedRecipes, recipeCursors,
                this::getObject, this::getCursor));
        jsonRpc.rpc("Generate", new Generate.Handler(localObjects, preparedRecipes, recipeCursors,
                this::getObject));
        jsonRpc.rpc("GetObject", new GetObject.Handler(batchSize, remoteObjects, localObjects));
        jsonRpc.rpc("GetRecipes", new JsonRpcMethod<Void>() {
            @Override
            protected Object handle(Void noParams) {
                return marketplace.listRecipeDescriptors();
            }
        });
        jsonRpc.rpc("PrepareRecipe", new PrepareRecipe.Handler(preparedRecipes));
        jsonRpc.rpc("Print", new JsonRpcMethod<Print>() {
            @Override
            protected Object handle(Print request) {
                Tree tree = getObject(request.getTreeId());
                Cursor cursor = getCursor(request.getCursor());
                return tree.print(new Cursor(cursor, tree));
            }
        });

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

        String pId = maybeUnwrapExecutionContext(p);
        List<String> cursorIds = getCursorIds(cursor);

        return send("Visit", new Visit(visitorName, null, sourceFile.getId().toString(), pId, cursorIds),
                VisitResponse.class);
    }

    public Collection<? extends SourceFile> generate(String remoteRecipeId, ExecutionContext ctx) {
        String ctxId = maybeUnwrapExecutionContext(ctx);
        List<String> generated = send("Generate", new Generate(remoteRecipeId, ctxId),
                GenerateResponse.class);
        if (!generated.isEmpty()) {
            return generated.stream()
                    .map(this::<SourceFile>getObject)
                    .collect(Collectors.toList());
        }
        return emptyList();
    }

    /**
     * If the p object is a DelegatingExecutionContext, unwrap it to get the underlying
     * ExecutionContext
     *
     * @param p   A visitor parameter, which may or may not be an ExecutionContext
     * @param <P> The type of p
     * @return The ID of p as represented in the local object cache.
     */
    private <P> String maybeUnwrapExecutionContext(P p) {
        Object p2 = p;
        while (p2 instanceof DelegatingExecutionContext) {
            p2 = ((DelegatingExecutionContext) p2).getDelegate();
        }
        String pId = localObjectIds.computeIfAbsent(p2, p3 -> SnowflakeId.generateId());
        if (p2 instanceof ExecutionContext) {
            ((ExecutionContext) p2).putMessage("org.openrewrite.rpc.id", pId);
        }
        localObjects.put(pId, p2);
        return pId;
    }

    public List<RecipeDescriptor> getRecipes() {
        return send("GetRecipes", null, GetRecipesResponse.class);
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

    @VisibleForTesting
    @Nullable
    List<String> getCursorIds(@Nullable Cursor cursor) {
        List<String> cursorIds = null;
        if (cursor != null) {
            cursorIds = cursor.getPathAsStream().map(c -> {
                String id = c instanceof Tree ?
                        ((Tree) c).getId().toString() :
                        localObjectIds.computeIfAbsent(c, c2 -> SnowflakeId.generateId());
                localObjects.put(id, c);
                return id;
            }).collect(Collectors.toList());
        }
        return cursorIds;
    }

    @VisibleForTesting
    public <T> T getObject(String id) {
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
        localObjects.put(id, remoteObject);

        //noinspection unchecked
        return (T) remoteObject;
    }

    private <P> P send(String method, @Nullable RpcRequest body, Class<P> responseType) {
        try {
            // TODO handle error
            return jsonRpc
                    .send(JsonRpcRequest.newRequest(method, body))
                    .get(timeout.getSeconds(), TimeUnit.SECONDS)
                    .getResult(responseType);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    Cursor getCursor(@Nullable List<String> cursorIds) {
        Cursor cursor = new Cursor(null, Cursor.ROOT_VALUE);
        if (cursorIds != null) {
            for (int i = cursorIds.size() - 1; i >= 0; i--) {
                String cursorId = cursorIds.get(i);
                Object cursorObject = getObject(cursorId);
                remoteObjects.put(cursorId, cursorObject);
                cursor = new Cursor(cursor, cursorObject);
            }
        }
        return cursor;
    }
}
