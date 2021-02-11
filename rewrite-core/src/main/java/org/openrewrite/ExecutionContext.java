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

import java.util.function.Consumer;

/**
 * Passes messages between individual visitors or parsing operations and allows errors to be propagated
 * back to the process controlling parsing or recipe execution.
 */
public interface ExecutionContext {
    void putMessage(String key, Object value);

    @Nullable
    <T> T peekMessage(String key);

    @Nullable
    <T> T pollMessage(String key);

    Consumer<Throwable> getOnError();
}
