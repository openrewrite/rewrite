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

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.marketplace.RecipeMarketplace;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lifecycle accounting for {@link RewriteRpcProcessManager}. These verify that the
 * number of live RPC "servers" stays bounded by the number of currently-live RPC
 * threads — not by how many threads, files, or runs are processed — and is driven to
 * zero on demand.
 * <p>
 * A {@link CountingRpc} stands in for a real server: it increments a shared counter on
 * construction and decrements it on {@link RewriteRpc#shutdown()}, so the counter mirrors
 * the count of live node subprocesses without spawning any.
 */
// The shared static CountingRpc.live counter, reset per test by newManager(), is only
// safe under sequential execution; pin it so enabling method-level parallelism elsewhere
// can't corrupt these assertions.
@Execution(ExecutionMode.SAME_THREAD)
class RewriteRpcProcessManagerTest {

    /**
     * A no-op {@link RewriteRpc} that tracks how many instances are currently "alive".
     * Stands in for a spawned node server so the manager's create/dispose accounting can be
     * asserted without real subprocesses. Its backing {@link JsonRpc} reads an
     * already-at-EOF stream and spins up a {@link java.util.concurrent.ForkJoinPool} on
     * construction; {@link #shutdown()} chains to {@link RewriteRpc#shutdown()} to tear that
     * pool down so its worker threads don't accumulate across the many stubs this suite builds.
     */
    static class CountingRpc extends RewriteRpc {
        static final AtomicInteger live = new AtomicInteger();

        private boolean shutdown;

        CountingRpc() {
            super(new JsonRpc(new HeaderDelimitedMessageHandler(
                    new JsonMessageFormatter(new ParameterNamesModule()),
                    new ByteArrayInputStream(new byte[0]),
                    new OutputStream() {
                        @Override
                        public void write(int b) {
                            // discard
                        }
                    })), new RecipeMarketplace());
            live.incrementAndGet();
        }

        @Override
        public void shutdown() {
            // Idempotent: a server must only ever be counted down once even if it is
            // both reaped and explicitly shut down.
            if (!shutdown) {
                shutdown = true;
                live.decrementAndGet();
                // Tear down the backing JsonRpc ForkJoinPool so its worker threads don't
                // accumulate across the hundreds of stubs this suite constructs.
                super.shutdown();
            }
        }
    }

    /**
     * A {@link CountingRpc} whose {@link #shutdown()} decrements the live counter (so accounting
     * stays accurate) and then throws, modelling a server whose teardown fails — e.g. a node
     * process reporting a non-zero exit code. Used to assert that one failed teardown does not
     * strand the remaining servers.
     */
    static class ThrowingRpc extends CountingRpc {
        @Override
        public void shutdown() {
            super.shutdown();
            throw new RuntimeException("simulated shutdown failure");
        }
    }

    private static RewriteRpcProcessManager<CountingRpc> newManager() {
        CountingRpc.live.set(0);
        return new RewriteRpcProcessManager<>(CountingRpc::new);
    }

    /**
     * Run {@code task} on a fresh thread and join it, so the thread is guaranteed dead
     * (its {@link Thread#isAlive()} is {@code false}) by the time this returns.
     */
    private static void runOnDeadThread(Runnable task) throws InterruptedException {
        Thread t = new Thread(task, "rpc-test-worker");
        t.start();
        t.join();
    }

    @Test
    void getOrStartReusesOnePerThread() {
        RewriteRpcProcessManager<CountingRpc> manager = newManager();

        CountingRpc first = manager.getOrStart();
        CountingRpc second = manager.getOrStart();

        assertThat(second).as("the same thread must reuse its server, never spawn a second")
                .isSameAs(first);
        assertThat(manager.liveCount()).isEqualTo(1);
        assertThat(CountingRpc.live).hasValue(1);
    }

    @Test
    void manySequentialRunsStayBoundedAndEndAtZero() {
        RewriteRpcProcessManager<CountingRpc> manager = newManager();

        // Each "run" lives on its own thread that starts a server then tears it down,
        // mirroring the worker's per-run shutdownCurrent(). The live count must never
        // exceed one regardless of how many runs go through, and must be zero between runs.
        AtomicInteger peak = new AtomicInteger();
        for (int run = 0; run < 200; run++) {
            int finalRun = run;
            try {
                runOnDeadThread(() -> {
                    manager.getOrStart();
                    int live = manager.liveCount();
                    peak.accumulateAndGet(live, Math::max);
                    assertThat(live)
                            .as("run %d should only have its own server live", finalRun)
                            .isEqualTo(1);
                    manager.shutdown();
                });
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertThat(CountingRpc.live)
                    .as("server torn down between runs")
                    .hasValue(0);
        }

        assertThat(peak).as("never more than one live server across 200 sequential runs").hasValue(1);
        assertThat(manager.liveCount()).isZero();
        assertThat(CountingRpc.live).hasValue(0);
    }

    @Test
    void deadThreadOrphansDoNotAccumulate() throws InterruptedException {
        RewriteRpcProcessManager<CountingRpc> manager = newManager();

        // Simulate transient threads (FJP compensation, cached/elastic pool threads that
        // die on idle timeout) that each start a server and then terminate WITHOUT calling
        // shutdown — the exact orphaning that used to leak node processes until JVM exit.
        // Each new RPC thread reaps prior dead-thread orphans before adding its own, so the
        // live count stays bounded instead of climbing to `orphans`. Pre-fix this would be 50.
        int orphans = 50;
        for (int i = 0; i < orphans; i++) {
            runOnDeadThread(manager::getOrStart);
            assertThat(manager.liveCount())
                    .as("orphans must not accumulate as transient threads come and go")
                    .isLessThanOrEqualTo(1);
        }

        // A new RPC thread appearing reaps any remaining dead-thread orphan before adding
        // its own, so the live count is exactly this thread's server.
        manager.getOrStart();
        assertThat(manager.liveCount())
                .as("dead-thread orphans reaped; only the live thread's server remains")
                .isEqualTo(1);
        assertThat(CountingRpc.live).hasValue(1);

        manager.shutdownAll();
        assertThat(CountingRpc.live).hasValue(0);
    }

    @Test
    void shutdownReapsDeadThreadOrphans() throws InterruptedException {
        RewriteRpcProcessManager<CountingRpc> manager = newManager();

        // The orchestrator thread (this one) owns a server, as does a transient thread
        // that dies without cleaning up — the worker's shutdownCurrent() on the
        // orchestrator thread must reach the orphan too, not just its own server.
        manager.getOrStart();
        runOnDeadThread(manager::getOrStart);
        assertThat(CountingRpc.live).hasValue(2);

        manager.shutdown();

        assertThat(manager.liveCount()).isZero();
        assertThat(CountingRpc.live)
                .as("shutdown() tears down the calling thread's server and reaps dead-thread orphans")
                .hasValue(0);
    }

    @Test
    void shutdownAllDrivesLiveServersToZeroAcrossLiveThreads() throws InterruptedException {
        RewriteRpcProcessManager<CountingRpc> manager = newManager();

        // Hold several threads alive and parked, each owning a server, to model concurrent
        // in-flight runs. shutdownAll() must reach servers on live threads too.
        int concurrent = 8;
        List<Thread> threads = new ArrayList<>();
        AtomicInteger started = new AtomicInteger();
        Object park = new Object();
        AtomicBoolean released = new AtomicBoolean();
        for (int i = 0; i < concurrent; i++) {
            Thread t = new Thread(() -> {
                manager.getOrStart();
                started.incrementAndGet();
                synchronized (park) {
                    // Guarded wait: if notifyAll() fires before this thread parks, the
                    // released flag is already set, so we never block on a missed wakeup.
                    while (!released.get()) {
                        try {
                            park.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }, "rpc-test-live-" + i);
            t.start();
            threads.add(t);
        }
        while (started.get() < concurrent) {
            Thread.sleep(5);
        }
        assertThat(manager.liveCount()).isEqualTo(concurrent);

        manager.shutdownAll();

        assertThat(manager.liveCount()).isZero();
        assertThat(CountingRpc.live)
                .as("shutdownAll() stops every server, including those owned by live threads")
                .hasValue(0);

        synchronized (park) {
            released.set(true);
            park.notifyAll();
        }
        for (Thread t : threads) {
            // Bounded join so a regression that strands a worker fails fast instead of hanging the suite.
            t.join(10_000);
            assertThat(t.isAlive()).as("parked worker %s should have been released", t.getName()).isFalse();
        }
    }

    @Test
    void shutdownAllKeepsGoingWhenAServerShutdownThrows() throws InterruptedException {
        RewriteRpcProcessManager<CountingRpc> manager = newManager();

        // One healthy server on this thread and one whose teardown throws, owned by a dead thread.
        // shutdownAll() must tear down both — a single failing shutdown cannot strand the rest.
        manager.getOrStart();
        runOnDeadThread(() -> {
            manager.setFactory(ThrowingRpc::new);
            manager.getOrStart();
        });
        assertThat(CountingRpc.live).hasValue(2);

        manager.shutdownAll();

        assertThat(manager.liveCount()).isZero();
        assertThat(CountingRpc.live)
                .as("a throwing teardown must not strand the healthy server")
                .hasValue(0);
    }

    @Test
    void shutdownReapsDeadOrphanWhoseShutdownThrows() throws InterruptedException {
        RewriteRpcProcessManager<CountingRpc> manager = newManager();

        // This thread owns a healthy server; a dead thread left behind an orphan whose teardown
        // throws. shutdown() reaps the orphan in its finally block and must swallow the failure
        // while still tearing down the calling thread's own server.
        manager.getOrStart();
        runOnDeadThread(() -> {
            manager.setFactory(ThrowingRpc::new);
            manager.getOrStart();
        });
        assertThat(CountingRpc.live).hasValue(2);

        manager.shutdown();

        assertThat(manager.liveCount()).isZero();
        assertThat(CountingRpc.live)
                .as("reaping a dead orphan whose shutdown throws must not strand other servers")
                .hasValue(0);
    }
}
