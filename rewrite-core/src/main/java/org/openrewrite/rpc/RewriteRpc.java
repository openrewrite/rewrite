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
import io.moderne.jsonrpc.JsonRpcSuccess;
import io.moderne.jsonrpc.internal.SnowflakeId;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.rpc.internal.PreparedRecipeCache;
import org.openrewrite.rpc.request.*;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.rpc.RpcObjectData.State.END_OF_OBJECT;

/**
 * Base class for RPC clients with thread-local context support.
 */
@SuppressWarnings("UnusedReturnValue")
public class RewriteRpc {
    private final JsonRpc jsonRpc;
    private final AtomicInteger batchSize = new AtomicInteger(200);
    private Duration timeout = Duration.ofSeconds(30);
    private Supplier<? extends @Nullable RuntimeException> livenessCheck = () -> null;

    final PreparedRecipeCache preparedRecipes = new PreparedRecipeCache();

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

    private @Nullable List<String> remoteLanguages;

    /**
     * Creates a new RPC interface that can be used to communicate with a remote.
     *
     * @param marketplace The marketplace of recipes that this peer makes available.
     *                    Even if this peer is the host process, configuring this
     *                    marketplace allows the remote peer to discover what recipes
     *                    the host process has available for its use in composite recipes.
     */
    public RewriteRpc(JsonRpc jsonRpc, Environment marketplace) {
        this.jsonRpc = jsonRpc;

        jsonRpc.rpc("Visit", new Visit.Handler(localObjects, preparedRecipes,
                this::getObject, this::getCursor));
        jsonRpc.rpc("Generate", new Generate.Handler(localObjects, preparedRecipes,
                this::getObject));
        jsonRpc.rpc("GetObject", new GetObject.Handler(batchSize, remoteObjects, localObjects, localRefs));
        jsonRpc.rpc("GetRecipes", new JsonRpcMethod<Void>() {
            @Override
            protected Object handle(Void noParams) {
                return marketplace.listRecipeDescriptors();
            }
        });
        jsonRpc.rpc("GetLanguages", new JsonRpcMethod<Void>() {
            @Override
            protected Object handle(Void noParams) {
                return Stream.of(
                        ifOnClasspath("org.openrewrite.text.PlainText"),
                        ifOnClasspath("org.openrewrite.json.tree.Json$Document"),
                        ifOnClasspath("org.openrewrite.java.tree.J$CompilationUnit"),
                        ifOnClasspath("org.openrewrite.javascript.tree.JS$CompilationUnit")
                ).filter(Objects::nonNull).toArray(String[]::new);
            }

            private @Nullable String ifOnClasspath(String className) {
                try {
                    Class.forName(className);
                    return className;
                } catch (ClassNotFoundException e) {
                    return null;
                }
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

    public RewriteRpc livenessCheck(Supplier<? extends @Nullable RuntimeException> livenessCheck) {
        this.livenessCheck = livenessCheck;
        return this;
    }

    public RewriteRpc timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public RewriteRpc batchSize(int batchSize) {
        this.batchSize.set(batchSize);
        return this;
    }

    public void shutdown() {
        jsonRpc.shutdown();
    }

    public <P> @Nullable Tree visit(SourceFile sourceFile, String visitorName, P p) {
        return visit(sourceFile, visitorName, p, null);
    }

    public <P> @Nullable Tree visit(Tree sourceFile, String visitorName, P p, @Nullable Cursor cursor) {
        // Set the local state of this tree, so that when the remote asks for it, we know what to send.
        localObjects.put(sourceFile.getId().toString(), sourceFile);

        String pId = maybeUnwrapExecutionContext(p);
        List<String> cursorIds = getCursorIds(cursor);

        VisitResponse response = send("Visit", new Visit(visitorName, null,
                sourceFile.getId().toString(), pId, cursorIds), VisitResponse.class);
        return response.isModified() ?
                getObject(sourceFile.getId().toString()) :
                sourceFile;
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

    public List<String> getLanguages() {
        if (remoteLanguages == null) {
            remoteLanguages = Arrays.asList(send("GetLanguages", null, String[].class));
        }
        return remoteLanguages;
    }

    public RpcRecipe prepareRecipe(String id) {
        return prepareRecipe(id, emptyMap());
    }

    public RpcRecipe prepareRecipe(String id, Map<String, Object> options) {
        PrepareRecipeResponse r = send("PrepareRecipe", new PrepareRecipe(id, options), PrepareRecipeResponse.class);

        // FIXME do this validation on the server side instead
        for (OptionDescriptor option : r.getDescriptor().getOptions()) {
            if (option.isRequired() && !options.containsKey(option.getName())) {
                throw new IllegalArgumentException("Missing required option `" + option.getName() + "` for recipe `" + id + "`.");
            }
        }

        return new RpcRecipe(this, r.getId(), r.getDescriptor(), r.getEditVisitor(),
                matchAll(r.getEditPreconditions()), r.getScanVisitor(), matchAll(r.getScanPreconditions()));
    }

    private @Nullable TreeVisitor<?, ExecutionContext> matchAll(List<PrepareRecipeResponse.Precondition> preconditions) {
        if (preconditions.isEmpty()) {
            return null;
        }

        List<TreeVisitor<?, ExecutionContext>> visitors = new ArrayList<>(preconditions.size());
        for (PrepareRecipeResponse.Precondition p : preconditions) {
            visitors.add(preparedRecipes.instantiateVisitor(
                    p.getVisitorName(), p.getVisitorOptions()));
        }

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(@NonNull Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                Tree t = tree;
                for (TreeVisitor<?, ExecutionContext> v : visitors) {
                    //noinspection unchecked
                    t = ((TreeVisitor<Tree, ExecutionContext>) v).visit(tree, ctx);
                    if (t == tree) {
                        // One of the preconditions didn't match, so we fail the whole precondition
                        return tree;
                    }
                }
                return t;
            }
        };
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
            return Stream.empty();
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
                    assert ids.size() == inputList.size();
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

        RpcReceiveQueue q = new RpcReceiveQueue(remoteRefs, () -> send("GetObject",
                new GetObject(id), GetObjectResponse.class));
        Object remoteObject = q.receive(localObject, null);
        if (q.take().getState() != END_OF_OBJECT) {
            throw new IllegalStateException("Expected END_OF_OBJECT");
        }

        if (remoteObject != null) {
            // We are now in sync with the remote state of the object.
            remoteObjects.put(id, requireNonNull(remoteObject));
            localObjects.put(id, remoteObject);
        }

        //noinspection unchecked,DataFlowIssue
        return (T) remoteObject;
    }

    protected <P> P send(String method, @Nullable RpcRequest body, Class<P> responseType) {
        try {
            checkLiveness();

            // Send the request and get the future
            CompletableFuture<JsonRpcSuccess> future = jsonRpc.send(JsonRpcRequest.newRequest(method, body));

            // Poll for completion while checking if process is alive
            long totalTimeoutMs = timeout.toMillis();
            long checkIntervalMs = 500; // Check every 500ms
            long elapsedMs = 0;

            while (elapsedMs < totalTimeoutMs) {
                try {
                    // Try to get the result with a short timeout
                    return future.get(checkIntervalMs, TimeUnit.MILLISECONDS).getResult(responseType);
                } catch (TimeoutException e) {
                    checkLiveness();
                    elapsedMs += checkIntervalMs;
                    // Continue waiting if process is still alive and we haven't hit total timeout
                }
            }

            // If we get here, we've hit the total timeout
            throw new RuntimeException("Request timed out after " + timeout.getSeconds() + " seconds");
        } catch (ExecutionException | InterruptedException e) {
            // Check if process crashed during the request
            checkLiveness();
            throw new RuntimeException(e);
        }
    }

    private void checkLiveness() {
        RuntimeException livenessProblem = livenessCheck.get();
        if (livenessProblem != null) {
            throw livenessProblem;
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
