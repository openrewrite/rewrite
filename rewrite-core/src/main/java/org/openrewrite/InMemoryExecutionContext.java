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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class InMemoryExecutionContext implements ExecutionContext {
    private final Map<String, Object> messages = new ConcurrentHashMap<>();
    private final Consumer<Throwable> onError;
    private final BiConsumer<Throwable, ExecutionContext> onTimeout;

    public InMemoryExecutionContext() {
        this(
                t -> {
                }
        );
    }

    public InMemoryExecutionContext(Consumer<Throwable> onError) {
        this(onError, Duration.ofHours(2));
    }

    public InMemoryExecutionContext(Consumer<Throwable> onError, Duration runTimeout) {
        this(onError, runTimeout, (throwable, ctx) -> {
        });
    }

    public InMemoryExecutionContext(Consumer<Throwable> onError, Duration runTimeout, BiConsumer<Throwable, ExecutionContext> onTimeout) {
        this.onError = onError;
        this.onTimeout = onTimeout;
        putMessage(ExecutionContext.RUN_TIMEOUT, runTimeout);
    }

    @Override
    public void putMessage(String key, @Nullable Object value) {
        if (value == null) {
            messages.remove(key);
        } else {
            messages.put(key, value);
        }
    }

    @Override
    @Nullable
    public <T> T getMessage(String key) {
        //noinspection unchecked
        return (T) messages.get(key);
    }

    @Override
    @Nullable
    public <T> T pollMessage(String key) {
        //noinspection unchecked
        return (T) messages.remove(key);
    }

    public Consumer<Throwable> getOnError() {
        return onError;
    }

    @Override
    public BiConsumer<Throwable, ExecutionContext> getOnTimeout() {
        return onTimeout;
    }
}
