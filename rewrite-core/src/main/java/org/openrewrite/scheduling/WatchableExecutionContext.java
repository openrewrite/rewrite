/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.scheduling;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class WatchableExecutionContext implements ExecutionContext {
    private final ExecutionContext delegate;
    private boolean hasNewMessages;

    public boolean hasNewMessages() {
        return hasNewMessages;
    }

    public void resetHasNewMessages() {
        this.hasNewMessages = false;
    }

    @Override
    public void putMessage(String key, @Nullable Object value) {
        hasNewMessages = true;
        delegate.putMessage(key, value);
    }

    public void putCycle(RecipeRunCycle<?> cycle) {
        delegate.putMessage(CURRENT_CYCLE, cycle);
    }

    @Nullable
    @Override
    public <T> T getMessage(String key) {
        return delegate.getMessage(key);
    }

    @Nullable
    @Override
    public <T> T pollMessage(String key) {
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
