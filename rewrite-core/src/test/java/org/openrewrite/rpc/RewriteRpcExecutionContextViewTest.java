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

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RewriteRpcExecutionContextViewTest {

    @Test
    void noSemaphoreInstalled_runsWithoutThrottling() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        Integer result = RewriteRpcExecutionContextView.view(ctx).withInFlightSlot(() -> 42);
        assertThat(result).isEqualTo(42);
        assertThat(RewriteRpcExecutionContextView.view(ctx).getInFlightSemaphore()).isNull();
    }

    @Test
    void setMaxInFlight_installsFreshSemaphore() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        RewriteRpcExecutionContextView.view(ctx).setMaxInFlight(4);
        Semaphore s = RewriteRpcExecutionContextView.view(ctx).getInFlightSemaphore();
        assertThat(s).isNotNull();
        assertThat(s.availablePermits()).isEqualTo(4);
    }

    @Test
    void perCtxSemaphores_doNotShareAcrossIndependentContexts() {
        ExecutionContext ctxA = new InMemoryExecutionContext();
        ExecutionContext ctxB = new InMemoryExecutionContext();
        RewriteRpcExecutionContextView.view(ctxA).setMaxInFlight(2);
        RewriteRpcExecutionContextView.view(ctxB).setMaxInFlight(2);

        assertThat(RewriteRpcExecutionContextView.view(ctxA).getInFlightSemaphore())
                .isNotSameAs(RewriteRpcExecutionContextView.view(ctxB).getInFlightSemaphore());
    }

    @Test
    void sharedSemaphore_caps_concurrencyAcross_independentContexts() throws InterruptedException {
        // The shared-semaphore path: one Semaphore installed into multiple ExecutionContexts.
        // No matter how many contexts/threads call withInFlightSlot concurrently, only
        // {permits} of them can be inside the work block at once.
        Semaphore shared = new Semaphore(2);

        int contexts = 8;
        ExecutionContext[] ctxs = new ExecutionContext[contexts];
        for (int i = 0; i < contexts; i++) {
            ctxs[i] = new InMemoryExecutionContext();
            RewriteRpcExecutionContextView.view(ctxs[i]).setInFlightSemaphore(shared);
        }

        AtomicInteger concurrentInside = new AtomicInteger();
        AtomicInteger maxConcurrentObserved = new AtomicInteger();
        CountDownLatch allDone = new CountDownLatch(contexts);

        for (int i = 0; i < contexts; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    RewriteRpcExecutionContextView.view(ctxs[idx]).withInFlightSlot(() -> {
                        int now = concurrentInside.incrementAndGet();
                        maxConcurrentObserved.accumulateAndGet(now, Math::max);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        concurrentInside.decrementAndGet();
                        return null;
                    });
                } finally {
                    allDone.countDown();
                }
            }, "in-flight-" + i).start();
        }

        assertThat(allDone.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(maxConcurrentObserved.get()).isLessThanOrEqualTo(2);
    }

    @Test
    void permitIsReleased_onException() {
        Semaphore shared = new Semaphore(1);
        ExecutionContext ctx = new InMemoryExecutionContext();
        RewriteRpcExecutionContextView.view(ctx).setInFlightSemaphore(shared);

        assertThat(shared.availablePermits()).isEqualTo(1);
        try {
            RewriteRpcExecutionContextView.view(ctx).withInFlightSlot(() -> {
                throw new RuntimeException("boom");
            });
        } catch (RuntimeException expected) {
            // Permit must still be released so a wedged work block doesn't
            // permanently consume slots from the shared budget.
        }
        assertThat(shared.availablePermits()).isEqualTo(1);
    }
}
