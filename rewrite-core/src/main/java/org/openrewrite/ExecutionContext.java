/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.rpc.request.Visit;
import org.openrewrite.scheduling.RecipeRunCycle;

import java.util.*;
import java.util.function.*;

import static java.util.Collections.emptySet;
import static java.util.Collections.newSetFromMap;
import static java.util.Objects.requireNonNull;

/**
 * Passes messages between individual visitors or parsing operations and allows errors to be propagated
 * back to the process controlling parsing or recipe execution.
 */
public interface ExecutionContext extends RpcCodec<ExecutionContext> {
    String CURRENT_CYCLE = "org.openrewrite.currentCycle";
    String CURRENT_RECIPE = "org.openrewrite.currentRecipe";
    String RUN_TIMEOUT = "org.openrewrite.runTimeout";
    String REQUIRE_PRINT_EQUALS_INPUT = "org.openrewrite.requirePrintEqualsInput";
    String SCANNING_MUTATION_VALIDATION = "org.openrewrite.test.scanningMutationValidation";

    /**
     * Messages whose keys start with this prefix are shared with remote RPC peers
     * when this context is transferred via {@link #rpcSend} / {@link #rpcReceive}.
     * All other messages are process-local (they may hold non-serializable state
     * like caches, callbacks or open resources) and never cross the wire.
     * <p>
     * Values of shared messages must be limited to strings and lists of strings so
     * that every peer language can consume them without dedicated codecs.
     */
    String RPC_SHARED_MESSAGE_PREFIX = "org.openrewrite.rpc.shared.";

    @Incubating(since = "7.20.0")
    default ExecutionContext addObserver(TreeObserver.Subscription observer) {
        putMessageInCollection("org.openrewrite.internal.treeObservers", observer,
                () -> newSetFromMap(new IdentityHashMap<>()));
        return this;
    }

    @Incubating(since = "7.20.0")
    default Set<TreeObserver.Subscription> getObservers() {
        return getMessage("org.openrewrite.internal.treeObservers", emptySet());
    }

    Map<String, @Nullable Object> getMessages();

    void putMessage(String key, @Nullable Object value);

    <T> @Nullable T getMessage(String key);

    default <T> T computeMessageIfAbsent(String key, Function<? super String, ? extends T> defaultValue) {
        //noinspection unchecked
        return (T) getMessages().computeIfAbsent(key, defaultValue);
    }

    default <V, T> T computeMessage(String key, @Nullable V value, Supplier<T> defaultValue, BiFunction<@Nullable V, ? super T, ? extends T> remappingFunction) {
        T oldMessage = getMessage(key);
        if (oldMessage == null) {
            oldMessage = defaultValue.get();
        }
        T newMessage = remappingFunction.apply(value, oldMessage);
        putMessage(key, newMessage);
        return newMessage;
    }

    default <V, C extends Collection<V>> C putMessageInCollection(String key, V value, Supplier<C> newCollection) {
        return computeMessage(key, value, newCollection, (v, acc) -> {
            C c = newCollection.get();
            c.addAll(acc);
            c.add(value);
            return c;
        });
    }

    @SuppressWarnings("unused")
    default <T> Set<T> putMessageInSet(String key, T value) {
        return putMessageInCollection(key, value, HashSet::new);
    }

    default <T> T getMessage(String key, @Nullable T defaultValue) {
        T t = getMessage(key);
        //noinspection DataFlowIssue
        return t == null ? defaultValue : t;
    }

    <T> @Nullable T pollMessage(String key);

    @SuppressWarnings("unused")
    default <T> T pollMessage(String key, T defaultValue) {
        T t = pollMessage(key);
        return t == null ? defaultValue : t;
    }

    @SuppressWarnings("unused")
    default void putCurrentRecipe(Recipe recipe) {
        putMessage(CURRENT_RECIPE, recipe);
    }

    Consumer<Throwable> getOnError();

    BiConsumer<Throwable, ExecutionContext> getOnTimeout();

    default int getCycle() {
        return getCycleDetails().getCycle();
    }

    default RecipeRunCycle<?> getCycleDetails() {
        return requireNonNull(getMessage(CURRENT_CYCLE));
    }

    /**
     * Sends the messages under {@link #RPC_SHARED_MESSAGE_PREFIX} as a single value:
     * a list of {@code [key, value]} pairs sorted by key, or nothing (NO_CHANGE) when
     * there are none. Because a plain list carries no value type, peers receive it as
     * raw JSON without needing a dedicated codec for it.
     * <p>
     * Note that contexts are mutated in place rather than copied, so on a re-send the
     * before and after state are the same instance and the shared messages are simply
     * sent again whenever any exist.
     */
    @Override
    default void rpcSend(ExecutionContext ctx, RpcSendQueue q) {
        q.getAndSend(ctx, ExecutionContext::rpcSharedMessages);
    }

    @Override
    default ExecutionContext rpcReceive(ExecutionContext ctx, RpcReceiveQueue q) {
        List<List<Object>> after = q.receive(rpcSharedMessages(ctx));
        Set<String> retained = new HashSet<>();
        if (after != null) {
            for (List<Object> entry : after) {
                String key = (String) entry.get(0);
                ctx.putMessage(key, entry.get(1));
                retained.add(key);
            }
        }
        ctx.getMessages().keySet().removeIf(key ->
                key.startsWith(RPC_SHARED_MESSAGE_PREFIX) && !retained.contains(key));
        return ctx;
    }

    /**
     * @return The messages under {@link #RPC_SHARED_MESSAGE_PREFIX} as a list of
     * {@code [key, value]} pairs sorted by key, or null when there are none.
     */
    static @Nullable List<List<Object>> rpcSharedMessages(ExecutionContext ctx) {
        Map<String, Object> shared = new TreeMap<>();
        for (Map.Entry<String, @Nullable Object> message : ctx.getMessages().entrySet()) {
            if (message.getKey().startsWith(RPC_SHARED_MESSAGE_PREFIX) && message.getValue() != null) {
                shared.put(message.getKey(), message.getValue());
            }
        }
        if (shared.isEmpty()) {
            return null;
        }
        List<List<Object>> entries = new ArrayList<>(shared.size());
        for (Map.Entry<String, Object> entry : shared.entrySet()) {
            entries.add(Arrays.asList(entry.getKey(), entry.getValue()));
        }
        return entries;
    }
}
