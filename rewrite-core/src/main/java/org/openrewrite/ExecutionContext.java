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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ExecutionContext {
    private final int maxCycles;
    private final Consumer<Throwable> onError;

    Map<String, Object> messages = new HashMap<>();

    private ExecutionContext(int maxCycles, Consumer<Throwable> onError) {
        this.maxCycles = maxCycles;
        this.onError = onError;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public <T> T peekMessage(String key) {
        //noinspection unchecked
        return (T) messages.get(key);
    }

    @Nullable
    public <T> T pollMessage(String key) {
        //noinspection unchecked
        return (T) messages.remove(key);
    }

    int getMaxCycles() {
        return maxCycles;
    }

    Consumer<Throwable> getOnError() {
        return onError;
    }

    public static class Builder {
        private int maxCycles = 3;
        private Consumer<Throwable> onError;

        public Builder maxCycles(int maxCycles) {
            this.maxCycles = maxCycles;
            return this;
        }

        public Builder doOnError(Consumer<Throwable> onError) {
            this.onError = onError;
            return this;
        }

        public ExecutionContext build() {
            return new ExecutionContext(maxCycles, onError);
        }
    }
}
