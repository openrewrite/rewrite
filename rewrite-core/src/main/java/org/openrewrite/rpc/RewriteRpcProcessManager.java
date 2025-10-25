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

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Manages the lifecycle of {@link RewriteRpc} instances, allowing for a single
 * Rewrite RPC subprocess per thread.
 */
public class RewriteRpcProcessManager<R extends RewriteRpc> {
    private final ThreadLocal<R> rpc = new ThreadLocal<>();
    private final ThreadLocal<Supplier<R>> factory;

    public RewriteRpcProcessManager(Supplier<R> defaultFactory) {
        this.factory = ThreadLocal.withInitial(() -> defaultFactory);
    }

    public @Nullable R get() {
        return rpc.get();
    }

    public R getOrStart() {
        R current = rpc.get();
        //noinspection ConstantValue
        if (current == null) {
            current = factory.get().get();
            rpc.set(current);
        }
        return current;
    }

    public void setFactory(Supplier<R> factory) {
        this.factory.set(factory);
    }

    public void shutdown() {
        R current = rpc.get();
        //noinspection ConstantValue
        if (current != null) {
            current.shutdown();
            rpc.remove();
        }
    }
}
