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
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.rpc.request.*;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.rpc.RpcObjectData.State.END_OF_OBJECT;

/**
 * Base class for RPC clients with thread-local context support.
 */
@SuppressWarnings("UnusedReturnValue")
public class RewriteRpc implements AutoCloseable {

    public static Builder<?> from(JsonRpc jsonRpc, Environment marketplace) {
        //noinspection rawtypes
        return new Builder(marketplace) {
            @Override
            public RewriteRpc build() {
                return new RewriteRpc(jsonRpc, marketplace, timeout);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends Builder<T>> {
        protected final Environment marketplace;
        protected Duration timeout = Duration.ofMinutes(1);

        protected Builder(Environment marketplace) {
            this.marketplace = marketplace;
        }

        public T timeout(Duration timeout) {
            this.timeout = timeout;
            return (T) this;
        }

        public abstract RewriteRpc build();
    }

    private final JsonRpc jsonRpc;

    private final AtomicInteger batchSize = new AtomicInteger(200);
    private final Duration timeout;
    private final AtomicBoolean traceSendPackets = new AtomicBoolean(false);
    private @Nullable PrintStream traceFile;

    /**
     * Keeps track of the local and remote state of objects that are used in
     * visits and other operations for which incremental state sharing is useful
     * between two processes.
     */
    @VisibleForTesting
    final Map<String, Object> remoteObjects = new HashMap<>();

    @VisibleForTesting
    final Map<String, Object> localObjects = new HashMap<>();

    /* A reverse map of the objects back to their IDs */
    final Map<Object, String> localObjectIds = new IdentityHashMap<>();

    @VisibleForTesting
    final Map<Integer, Object> remoteRefs = new HashMap<>();

    @VisibleForTesting
    final IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();

    /**
     * Creates a new RPC interface that can be used to communicate with a remote.
     *
     * @param marketplace The marketplace of recipes that this peer makes available.
     *                    Even if this peer is the host process, configuring this
     *                    marketplace allows the remote peer to discover what recipes
     *                    the host process has available for its use in composite recipes.
     */
    protected RewriteRpc(JsonRpc jsonRpc, Environment marketplace, Duration timeout) {
        this.timeout = timeout;

        this.jsonRpc = jsonRpc;

        Map<String, Recipe> preparedRecipes = new HashMap<>();
        Map<Recipe, Cursor> recipeCursors = new IdentityHashMap<>();

        jsonRpc.rpc("GetRef", new GetRef.Handler(remoteRefs, localRefs, batchSize, traceSendPackets));
        jsonRpc.rpc("Visit", new Visit.Handler(localObjects, preparedRecipes, recipeCursors,
                this::getObject, this::getCursor));
        jsonRpc.rpc("Generate", new Generate.Handler(localObjects, preparedRecipes, recipeCursors,
                this::getObject));
        jsonRpc.rpc("GetObject", new GetObject.Handler(batchSize, remoteObjects, localObjects, localRefs, traceSendPackets));
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
        this.batchSize.set(batchSize);
        return this;
    }

    public RewriteRpc traceGetObjectOutput() {
        this.traceSendPackets.set(true);
        return this;
    }

    public RewriteRpc traceGetObjectInput(PrintStream log) {
        this.traceFile = log;
        return this;
    }


    @Override
    public void close() {
        jsonRpc.shutdown();
    }

    public <P> @Nullable Tree visit(SourceFile sourceFile, String visitorName, P p) {
        return visit(sourceFile, visitorName, p, null);
    }

    public <P> @Nullable Tree visit(Tree tree, String visitorName, P p, @Nullable Cursor cursor) {
        VisitResponse response = scan(tree, visitorName, p, cursor);
        return response.isModified() ?
                getObject(tree.getId().toString()) :
                tree;
    }

    public <P> VisitResponse scan(SourceFile sourceFile, String visitorName, P p) {
        return scan(sourceFile, visitorName, p, null);
    }

    public <P> VisitResponse scan(Tree sourceFile, String visitorName, P p,
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
                    .collect(toList());
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

    public Recipe prepareRecipe(String id) {
        return prepareRecipe(id, emptyMap());
    }

    public Recipe prepareRecipe(String id, Map<String, Object> options) {
        PrepareRecipeResponse r = send("PrepareRecipe", new PrepareRecipe(id, options), PrepareRecipeResponse.class);
        // FIXME do this validation on the server side instead
        for (OptionDescriptor option : r.getDescriptor().getOptions()) {
            if (option.isRequired() && !options.containsKey(option.getName())) {
                throw new IllegalArgumentException("Missing required option `" + option.getName() + "` for recipe `" + id + "`.");
            }
        }
        return new RpcRecipe(this, r.getId(), r.getDescriptor(), r.getEditVisitor(), r.getScanVisitor());
    }

    public Stream<SourceFile> parse(Iterable<Parser.Input> inputs, @Nullable Path relativeTo, Parser parser, ExecutionContext ctx) {
        List<Parser.Input> inputList = new ArrayList<>();
        List<Parse.Input> mappedInputs = new ArrayList<>();
        for (Parser.Input input : inputs) {
            inputList.add(input);
            if (input.isSynthetic() || !Files.isRegularFile(input.getPath())) {
                mappedInputs.add(new Parse.StringInput(input.getSource(ctx).readFully(), input.getPath()));
            } else {
                mappedInputs.add(new Parse.PathInput(input.getPath()));
            }
        }

        if (inputList.isEmpty()) {
            return Stream.of();
        }

        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        parsingListener.intermediateMessage(String.format("Starting parsing of %,d files", inputList.size()));

        return StreamSupport.stream(new Spliterator<SourceFile>() {
            private int index = 0;
            private @Nullable List<String> ids;

            @Override
            public boolean tryAdvance(Consumer<? super SourceFile> action) {
                if (ids == null) {
                    // FIXME handle `TimeoutException` gracefully
                    ids = send("Parse", new Parse(mappedInputs, relativeTo != null ? relativeTo.toString() : null), ParseResponse.class);

                    // If batch is empty, we're done
                    if (ids.isEmpty()) {
                        return false;
                    }
                }

                // Process current item in batch
                if (index >= inputList.size()) {
                    return false;
                }

                Parser.Input input = inputList.get(index);
                String id = ids.get(index);
                index++;

                SourceFile sourceFile = null;
                parsingListener.startedParsing(input);
                try {
                    sourceFile = parser.requirePrintEqualsInput(getObject(id), input, relativeTo, ctx);
                } catch (Exception e) {
                    sourceFile = ParseError.build(parser, input, relativeTo, ctx, e);
                } finally {
                    if (sourceFile != null) {
                        action.accept(sourceFile);
                        parsingListener.parsed(input, sourceFile);
                    }
                }
                return true;
            }

            @Override
            public @Nullable Spliterator<SourceFile> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return inputList.size() - index;
            }

            @Override
            public int characteristics() {
                return ORDERED | SIZED | SUBSIZED;
            }
        }, false);
    }

    public String print(SourceFile tree) {
        return print(tree, new Cursor(null, Cursor.ROOT_VALUE), null);
    }

    public String print(SourceFile tree, Print.@Nullable MarkerPrinter markerPrinter) {
        return print(tree, new Cursor(null, Cursor.ROOT_VALUE), markerPrinter);
    }

    public String print(Tree tree, Cursor parent, Print.@Nullable MarkerPrinter markerPrinter) {
        localObjects.put(tree.getId().toString(), tree);
        return send("Print", new Print(tree.getId().toString(), getCursorIds(parent), markerPrinter), String.class);
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
            }).collect(toList());
        }
        return cursorIds;
    }

    @VisibleForTesting
    public <T> T getObject(String id) {
        // Check if we have a cached version of this object
        Object localObject = localObjects.get(id);
        String lastKnownId = localObject != null ? id : null;

        RpcReceiveQueue q = new RpcReceiveQueue(remoteRefs, traceFile, () -> send("GetObject",
                new GetObject(id, lastKnownId), GetObjectResponse.class), this::getRef);
        Object remoteObject = q.receive(localObject, null);
        if (q.take().getState() != END_OF_OBJECT) {
            throw new IllegalStateException("Expected END_OF_OBJECT");
        }
        // We are now in sync with the remote state of the object.
        remoteObjects.put(id, remoteObject);
        localObjects.put(id, remoteObject);

        //noinspection unchecked
        return (T) remoteObject;
    }

    private Object getRef(Integer refId) {
        RpcReceiveQueue q = new RpcReceiveQueue(remoteRefs, traceFile, () -> send("GetRef",
                new GetRef(refId), GetRefResponse.class), nestedRefId -> {
            throw new IllegalStateException("Nested ref calls not supported in GetRef: " + nestedRefId);
        });

        Object ref = q.receive(null, null);
        if (q.take().getState() != END_OF_OBJECT) {
            throw new IllegalStateException("Expected END_OF_OBJECT");
        }

        if (ref == null) {
            throw new IllegalStateException("Reference " + refId + " not found on remote");
        }

        remoteRefs.put(refId, ref);
        localRefs.put(ref, refId);

        return ref;
    }

    protected <P> P send(String method, @Nullable RpcRequest body, Class<P> responseType) {
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
