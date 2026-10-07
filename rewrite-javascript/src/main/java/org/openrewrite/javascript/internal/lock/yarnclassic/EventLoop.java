/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Node's event loop as an install runs on it: microtasks drain before the next I/O completes, in issue order.
 */
final class EventLoop {

    private final Deque<Runnable> microtasks = new ArrayDeque<>();
    private final List<Io<?>> pending = new ArrayList<>();
    private int issued;

    /**
     * One I/O operation: a registry request or a read of the project's files.
     */
    static final class Io<T> {
        final int id;
        final Promise<T> promise;

        /**
         * The I/O that completed before this one was issued, as far as the request issuing it knows.
         */
        final Set<Integer> causal;

        private final Supplier<T> perform;

        Io(int id, Promise<T> promise, Set<Integer> causal, Supplier<T> perform) {
            this.id = id;
            this.promise = promise;
            this.causal = causal;
            this.perform = perform;
        }
    }

    void queueMicrotask(Runnable task) {
        microtasks.add(task);
    }

    <T> Io<T> io(Set<Integer> causal, Supplier<T> perform) {
        Io<T> io = new Io<>(issued++, new Promise<>(this), new LinkedHashSet<>(causal), perform);
        pending.add(io);
        return io;
    }

    // an operation whose id is at least a mark was issued after the mark was taken
    int mark() {
        return issued;
    }

    void run() {
        while (true) {
            Runnable task;
            while ((task = microtasks.poll()) != null) {
                task.run();
            }
            if (pending.isEmpty()) {
                return;
            }
            complete(pending.remove(0));
        }
    }

    private static <T> void complete(Io<T> io) {
        T value;
        try {
            value = io.perform.get();
        } catch (RuntimeException e) {
            io.promise.reject(e);
            return;
        }
        io.promise.resolve(value);
    }

    /**
     * A JavaScript promise: its reactions run as microtasks once it settles.
     */
    static final class Promise<T> {
        private final EventLoop loop;
        private final List<Runnable> reactions = new ArrayList<>();
        private boolean settled;
        private @Nullable T value;
        private @Nullable RuntimeException error;

        Promise(EventLoop loop) {
            this.loop = loop;
        }

        static <T> Promise<T> resolved(EventLoop loop, @Nullable T value) {
            Promise<T> promise = new Promise<>(loop);
            promise.resolve(value);
            return promise;
        }

        static <T> Promise<T> rejected(EventLoop loop, RuntimeException error) {
            Promise<T> promise = new Promise<>(loop);
            promise.reject(error);
            return promise;
        }

        // an async function's body up to its first await: what it throws rejects the promise it returns
        static <T> Promise<T> async(EventLoop loop, Supplier<Promise<T>> body) {
            try {
                return body.get();
            } catch (RuntimeException e) {
                return rejected(loop, e);
            }
        }

        void resolve(@Nullable T value) {
            if (settled) {
                return;
            }
            settled = true;
            this.value = value;
            flush();
        }

        void reject(RuntimeException error) {
            if (settled) {
                return;
            }
            settled = true;
            this.error = error;
            flush();
        }

        private void flush() {
            for (Runnable reaction : reactions) {
                loop.queueMicrotask(reaction);
            }
            reactions.clear();
        }

        private void react(Runnable reaction) {
            if (settled) {
                loop.queueMicrotask(reaction);
            } else {
                reactions.add(reaction);
            }
        }

        boolean isSettled() {
            return settled;
        }

        @Nullable
        RuntimeException getError() {
            return error;
        }

        @Nullable
        T getValue() {
            return value;
        }

        interface Step<T, U> {
            Promise<U> apply(@Nullable T value);
        }

        <U> Promise<U> then(Step<T, U> step) {
            Promise<U> next = new Promise<>(loop);
            react(() -> {
                if (error != null) {
                    next.reject(error);
                    return;
                }
                Promise<U> result;
                try {
                    result = step.apply(value);
                } catch (RuntimeException e) {
                    next.reject(e);
                    return;
                }
                result.react(() -> {
                    if (result.error != null) {
                        next.reject(result.error);
                    } else {
                        next.resolve(result.value);
                    }
                });
            });
            return next;
        }

        interface Recovery<T> {
            Promise<T> apply(RuntimeException error);
        }

        /**
         * {@code try { await this } catch (e) { ... }}.
         */
        Promise<T> recover(Recovery<T> recovery) {
            Promise<T> next = new Promise<>(loop);
            react(() -> {
                if (error == null) {
                    next.resolve(value);
                    return;
                }
                Promise<T> result;
                try {
                    result = recovery.apply(error);
                } catch (RuntimeException e) {
                    next.reject(e);
                    return;
                }
                result.react(() -> {
                    if (result.error != null) {
                        next.reject(result.error);
                    } else {
                        next.resolve(result.value);
                    }
                });
            });
            return next;
        }

        // for (const promise of promises) await promise
        static Promise<Void> awaitInOrder(EventLoop loop, List<Promise<Void>> promises) {
            Promise<Void> chain = resolved(loop, null);
            for (Promise<Void> promise : promises) {
                chain = chain.then(v -> promise);
            }
            return chain;
        }
    }
}
