/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

public class RewriteRpcExecutionContextView extends DelegatingExecutionContext {
    private static final String IN_FLIGHT_SEMAPHORE = "org.openrewrite.rpc.inFlightSemaphore";

    public RewriteRpcExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static RewriteRpcExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof RewriteRpcExecutionContextView) {
            return (RewriteRpcExecutionContextView) ctx;
        }
        return new RewriteRpcExecutionContextView(ctx);
    }

    /**
     * Install a {@link Semaphore} that caps the number of concurrent in-flight
     * rewrite-rpc requests this {@link ExecutionContext} participates in. Each
     * permit covers one actively-executing visit/parse/generate/print; idle
     * subprocesses do not consume a permit.
     * <p>
     * Pass the <strong>same</strong> {@code Semaphore} instance into multiple
     * {@code ExecutionContext}s (e.g. one per parallel recipe-run task) to
     * give them a single shared concurrency budget. This is the intended way
     * to enforce a host-wide cap when many independent {@code ExecutionContext}s
     * exist on the same JVM.
     * <p>
     * If no semaphore is installed, in-flight calls are unbounded — the
     * out-of-the-box OpenRewrite default.
     */
    public RewriteRpcExecutionContextView setInFlightSemaphore(Semaphore inFlightSemaphore) {
        putMessage(IN_FLIGHT_SEMAPHORE, inFlightSemaphore);
        return this;
    }

    public @Nullable Semaphore getInFlightSemaphore() {
        return getMessage(IN_FLIGHT_SEMAPHORE);
    }

    /**
     * Convenience for the single-context case: install a fresh {@code Semaphore}
     * with the given number of permits. The semaphore is created here and is
     * scoped to this {@code ExecutionContext} only; child contexts that view
     * the same delegate share it, but a separately-constructed
     * {@code InMemoryExecutionContext} elsewhere does <em>not</em>.
     * <p>
     * For a host-wide cap spanning multiple parallel runs, construct one
     * {@code Semaphore} yourself at the orchestrator level and install it
     * via {@link #setInFlightSemaphore(Semaphore)} on every participating
     * context.
     */
    public RewriteRpcExecutionContextView setMaxInFlight(int permits) {
        return setInFlightSemaphore(new Semaphore(permits));
    }

    /**
     * Run {@code work} while holding one in-flight RPC permit, if a semaphore
     * has been installed via {@link #setInFlightSemaphore(Semaphore)} or
     * {@link #setMaxInFlight(int)}. If none is installed, {@code work} runs
     * without throttling.
     */
    public <T> T withInFlightSlot(Supplier<T> work) {
        Semaphore slots = getInFlightSemaphore();
        if (slots == null) {
            return work.get();
        }
        slots.acquireUninterruptibly();
        try {
            return work.get();
        } finally {
            slots.release();
        }
    }
}
