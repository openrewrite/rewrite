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

import org.openrewrite.internal.lang.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Passes messages between individual visitors or parsing operations and allows errors to be propagated
 * back to the process controlling parsing or recipe execution.
 */
public interface ExecutionContext {
    String CURRENT_RECIPE = "org.openrewrite.currentRecipe";
    String UNCAUGHT_EXCEPTION_COUNT = "org.openrewrite.uncaughtExceptionCount";
    String DATA_TABLES = "org.openrewrite.dataTables";

    @Incubating(since = "7.20.0")
    default ExecutionContext addObserver(TreeObserver.Subscription observer) {
        putMessageInCollection("org.openrewrite.internal.treeObservers", observer,
                () -> Collections.newSetFromMap(new IdentityHashMap<>()));
        return this;
    }

    @Incubating(since = "7.20.0")
    default Set<TreeObserver.Subscription> getObservers() {
        return getMessage("org.openrewrite.internal.treeObservers", Collections.emptySet());
    }

    void putMessage(String key, @Nullable Object value);

    @Nullable <T> T getMessage(String key);

    default <V, T> T computeMessage(String key, V value, Supplier<T> defaultValue, BiFunction<V, ? super T, ? extends T> remappingFunction) {
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

    default <T> Set<T> putMessageInSet(String key, T value) {
        return putMessageInCollection(key, value, HashSet::new);
    }

    default <T> T getMessage(String key, @Nullable T defaultValue) {
        T t = getMessage(key);
        return t == null ? defaultValue : t;
    }

    @Nullable <T> T pollMessage(String key);

    default <T> T pollMessage(String key, T defaultValue) {
        T t = pollMessage(key);
        return t == null ? defaultValue : t;
    }

    default void putCurrentRecipe(Recipe recipe) {
        putMessage(CURRENT_RECIPE, recipe);
    }

    default Recipe getCurrentRecipe() {
        //noinspection ConstantConditions
        return getMessage(CURRENT_RECIPE);
    }

    default int incrementAndGetUncaughtExceptionCount() {
        int count = getMessage(UNCAUGHT_EXCEPTION_COUNT, 0) + 1;
        putMessage(UNCAUGHT_EXCEPTION_COUNT, count);
        return count;
    }

    Consumer<Throwable> getOnError();

    BiConsumer<Throwable, ExecutionContext> getOnTimeout();

    /**
     * @param inputs The number of inputs to the run. Allows the duration to be scaled to the number of inputs.
     * @return The maximum duration a run (e.g. parse operation, recipe run) can take.
     */
    @Incubating(since = "7.3.0")
    default Duration getRunTimeout(int inputs) {
        return Duration.ofHours(2);
    }
}
