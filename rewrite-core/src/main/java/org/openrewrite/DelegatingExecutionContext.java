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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DelegatingExecutionContext implements ExecutionContext {
    private final ExecutionContext delegate;

    public DelegatingExecutionContext(ExecutionContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public void putMessage(String key, @Nullable Object value) {
        delegate.putMessage(key, value);
    }

    @Override
    public <T> @Nullable T getMessage(String key) {
        return delegate.getMessage(key);
    }

    @Override
    public <T> @Nullable T pollMessage(String key) {
        return delegate.pollMessage(key);
    }

    @Override
    public Consumer<Throwable> getOnError() {
        return delegate.getOnError();
    }

    @Override
    public BiConsumer<Throwable, ExecutionContext> getOnTimeout() {
        return delegate.getOnTimeout();
    }
}
