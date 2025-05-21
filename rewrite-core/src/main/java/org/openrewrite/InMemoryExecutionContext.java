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

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class InMemoryExecutionContext implements ExecutionContext, Cloneable {
    @Nullable
    private volatile Map<String, Object> messages;

    private final Consumer<Throwable> onError;
    private final BiConsumer<Throwable, ExecutionContext> onTimeout;
    private static final ThreadLocal<Boolean> inTest = ThreadLocal.withInitial(() -> false);

    public InMemoryExecutionContext() {
        this(t -> {
        });
    }

    public InMemoryExecutionContext(Consumer<Throwable> onError) {
        this(onError, Duration.ofHours(2));
    }

    public InMemoryExecutionContext(Consumer<Throwable> onError, Duration runTimeout) {
        this(onError, runTimeout, (throwable, ctx) -> {
        });
    }

    public InMemoryExecutionContext(Consumer<Throwable> onError, Duration runTimeout, BiConsumer<Throwable, ExecutionContext> onTimeout) {
        this(onError, runTimeout, onTimeout, inTest.get());
    }

    private InMemoryExecutionContext(Consumer<Throwable> onError, Duration runTimeout, BiConsumer<Throwable, ExecutionContext> onTimeout, boolean inTestOverride) {
        this.onError = onError;
        this.onTimeout = onTimeout;
        putMessage(ExecutionContext.RUN_TIMEOUT, runTimeout);
        assert !inTestOverride : "Recipes must never instantiate their own execution context. " +
                               "Always use the execution context passed in to the visitor. " +
                               "Failure to follow this rule can lead to settings, caches, and http configuration being ignored, duplicated, or lost. " +
                               "If you are sure you have a good reason to ignore this advice, use the static method unsafeExecutionContext().";
    }

    /**
     * Sets the thread-local flag indicating whether the current thread is in a test.
     * If you're writing a recipe, you should never need to touch this.
     * Always use the execution context provided to the visitor at execution time.
     */
    public static void unsafeSetInTest(boolean isInTest) {
        inTest.set(isInTest);
    }

    /**
     * Use only when you are sure you have a good reason to create an execution context that doesn't actually have any context.
     * If you're writing a recipe, you should never need to touch this.
     * Always use the execution context provided to the visitor at execution time.
     */
    public static InMemoryExecutionContext unsafeExecutionContext() {
        return new InMemoryExecutionContext(t -> {}, Duration.ofHours(2), (throwable, ctx) -> {}, false);
    }

    @Override
    public Map<String, @Nullable Object> getMessages() {
        if (messages == null) {
            synchronized (this) {
                if (messages == null) {
                    messages = new ConcurrentHashMap<>();
                }
            }
        }
        //noinspection DataFlowIssue
        return messages;
    }

    @Override
    public void putMessage(String key, @Nullable Object value) {
        if (value == null) {
            if (messages != null) {
                getMessages().remove(key);
            }
        } else {
            getMessages().put(key, value);
        }
    }

    @Override
    public <T> @Nullable T getMessage(String key) {
        //noinspection unchecked
        return (T) getMessages().get(key);
    }

    @Override
    public <T> @Nullable T pollMessage(String key) {
        //noinspection unchecked
        return (T) getMessages().remove(key);
    }

    @Override
    public Consumer<Throwable> getOnError() {
        return onError;
    }

    @Override
    public BiConsumer<Throwable, ExecutionContext> getOnTimeout() {
        return onTimeout;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public InMemoryExecutionContext clone() {
        InMemoryExecutionContext clone = new InMemoryExecutionContext();

        clone.messages = new ConcurrentHashMap<>(getMessages());
        //noinspection DataFlowIssue
        clone.messages.computeIfPresent(DATA_TABLES, (key, dt) ->
                new ConcurrentHashMap<>(((Map<?, ?>) dt)));
        return clone;
    }
}
