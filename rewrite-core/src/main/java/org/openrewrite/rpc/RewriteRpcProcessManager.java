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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Manages the lifecycle of {@link RewriteRpc} instances, allowing for a single
 * Rewrite RPC subprocess per thread.
 * <p>
 * Each thread that does RPC work (parsing or printing a Python/JavaScript/… LST)
 * lazily gets its own subprocess via {@link #getOrStart()}, which is reused for
 * the life of that thread so a thread never spawns more than one server. Because
 * each server is an out-of-process node process with no finalizer, it must be
 * torn down explicitly — letting the owning {@link RewriteRpc} become garbage
 * does <em>not</em> stop the OS process (it lingers until the JVM-exit shutdown
 * hook on {@link RewriteRpcProcess} fires).
 * <p>
 * The hazard this guards against is <em>orphaned</em> servers: a server started
 * on thread X cannot be reached by {@link #shutdown()} called on thread Y, so
 * any server whose owning thread terminates before calling {@code shutdown()}
 * (ForkJoinPool ManagedBlocker compensation threads, cached/elastic pool threads
 * that die on their idle timeout, …) would otherwise survive — and accumulate
 * without bound — until JVM exit. To keep the number of live servers in check,
 * this manager tracks every started server in a process-wide, thread-keyed
 * registry and {@linkplain #reapDeadThreads() reaps} servers whose owning thread
 * is no longer alive. Reaping is activity-driven: it runs when a new RPC thread
 * starts a server (in {@link #getOrStart()}) and on {@link #shutdown()}, so a
 * dead thread's server lingers only until the next such event — the residual is
 * bounded by the number of RPC threads that have retired since the last reap,
 * not by the total number of threads ever created. Reaping a dead thread's
 * server is always safe because a dead thread cannot be mid-RPC, which is what
 * makes it sound to do even while other threads are running concurrent RPC work.
 */
public class RewriteRpcProcessManager<R extends RewriteRpc> {
    /**
     * Process-wide registry of live servers keyed by their owning thread. Replaces a
     * bare {@link ThreadLocal} so that, in addition to the per-thread fast path, the
     * full set of live servers can be enumerated for reaping and definitive shutdown.
     */
    private final Map<Thread, R> rpcByThread = new ConcurrentHashMap<>();
    private final ThreadLocal<Supplier<R>> factory;

    public RewriteRpcProcessManager(Supplier<R> defaultFactory) {
        this.factory = ThreadLocal.withInitial(() -> defaultFactory);
    }

    public @Nullable R get() {
        return rpcByThread.get(Thread.currentThread());
    }

    public R getOrStart() {
        Thread current = Thread.currentThread();
        R existing = rpcByThread.get(current);
        if (existing != null) {
            // Fast path: this thread already owns a server. Avoid the reap sweep so
            // hot parse/print paths don't pay for it on every call.
            return existing;
        }
        // A new RPC thread is appearing — exactly when orphaned servers from
        // previously-retired threads should be cleaned up before adding another.
        reapDeadThreads();
        R started = factory.get().get();
        // Construct the server before touching the map. Only the current thread ever
        // inserts its own key, so putIfAbsent's loser branch can fire only if this
        // thread re-entered getOrStart() during construction; if it did, shut the
        // duplicate down and keep the already-registered server.
        R prior = rpcByThread.putIfAbsent(current, started);
        if (prior != null) {
            started.shutdown();
            return prior;
        }
        return started;
    }

    public void setFactory(Supplier<R> factory) {
        this.factory.set(factory);
    }

    public void reset() {
        R current = get();
        if (current != null) {
            current.reset();
        }
    }

    /**
     * Shut down the calling thread's server (if any) and reap any servers whose
     * owning thread has since died. Callers already invoke this at run boundaries,
     * so routing dead-thread reaping through it keeps orphaned servers from
     * accumulating across runs without requiring any change at the call sites.
     */
    public void shutdown() {
        R current = rpcByThread.remove(Thread.currentThread());
        try {
            if (current != null) {
                current.shutdown();
            }
        } finally {
            reapDeadThreads();
        }
    }

    /**
     * Shut down <em>every</em> live server across all threads, driving the live-server
     * count to zero. Unlike {@link #reapDeadThreads()} this also stops servers owned by
     * currently-live threads, so it must only be called once no thread is doing — or
     * about to do — RPC work, e.g. on JVM/service shutdown. A server is registered only
     * after it is constructed in {@link #getOrStart()}, so calling this concurrently with
     * {@code getOrStart()} could let a just-created server register after the sweep and
     * survive; calling it mid parse/print would tear that run's server out from under it.
     * <p>
     * This is not required for correctness on JVM exit: each RPC subprocess registers its
     * own JVM-exit shutdown hook, so {@code shutdownAll()} only bounds the live-server
     * count earlier within a long-running JVM.
     */
    public void shutdownAll() {
        for (Thread t : rpcByThread.keySet()) {
            R r = rpcByThread.remove(t);
            if (r != null) {
                try {
                    r.shutdown();
                } catch (Throwable ignored) {
                    // Keep going so one bad teardown can't strand the rest.
                }
            }
        }
    }

    /**
     * The number of live servers currently tracked by this manager. Intended for
     * diagnostics and tests asserting that the count stays bounded.
     */
    public int liveCount() {
        return rpcByThread.size();
    }

    /**
     * Shut down servers whose owning thread is no longer alive. Safe to call at any
     * time, including while other threads are doing RPC work, because a terminated
     * thread cannot be mid-RPC on its server.
     */
    private void reapDeadThreads() {
        for (Map.Entry<Thread, R> e : rpcByThread.entrySet()) {
            if (!e.getKey().isAlive()) {
                // remove() returns the value to exactly one racing reaper, so the
                // server is shut down once even under concurrent reaping.
                R dead = rpcByThread.remove(e.getKey());
                if (dead != null) {
                    try {
                        dead.shutdown();
                    } catch (Throwable ignored) {
                        // Best-effort: the owning thread is gone, so there is no one
                        // to surface this to; keep reaping the remaining orphans.
                    }
                }
            }
        }
    }
}
