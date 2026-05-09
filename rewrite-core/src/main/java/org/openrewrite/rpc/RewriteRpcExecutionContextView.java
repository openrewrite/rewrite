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

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

public class RewriteRpcExecutionContextView extends DelegatingExecutionContext {
    private static final String MAX_IN_FLIGHT = "org.openrewrite.rpc.maxInFlight";
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
     * Cap on concurrent in-flight RPC requests across this run, sized to the
     * memory headroom of the host running rewrite-rpc subprocesses. Each
     * permit covers one actively-executing visit/parse/generate/print; idle
     * subprocesses do not consume a permit. Default is unlimited so that
     * out-of-the-box OpenRewrite behavior is unchanged; callers (e.g. a host
     * that knows its own memory budget) opt in by setting an explicit cap.
     */
    public RewriteRpcExecutionContextView setMaxInFlight(int maxInFlight) {
        putMessage(MAX_IN_FLIGHT, maxInFlight);
        return this;
    }

    public int getMaxInFlight() {
        return getMessage(MAX_IN_FLIGHT, Integer.MAX_VALUE);
    }

    /**
     * Run {@code work} while holding one of the in-flight RPC permits. The
     * semaphore is created lazily on first call and shared across every
     * thread that observes this ExecutionContext.
     */
    public <T> T withInFlightSlot(Supplier<T> work) {
        Semaphore slots = computeMessageIfAbsent(IN_FLIGHT_SEMAPHORE,
                k -> new Semaphore(getMaxInFlight()));
        slots.acquireUninterruptibly();
        try {
            return work.get();
        } finally {
            slots.release();
        }
    }
}
