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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Node's event loop as an install runs on it: microtasks drain before the next I/O completes, and I/O completes in
 * the order it was issued.
 */
public final class EventLoop {
    private final Deque<Runnable> microtasks = new ArrayDeque<>();
    private final Deque<Runnable> io = new ArrayDeque<>();
    private int issued;

    public void queueMicrotask(Runnable task) {
        microtasks.add(task);
    }

    /**
     * Issues one I/O operation: a registry request or a read of the project's files.
     */
    public <T> Promise<T> request(Supplier<T> perform) {
        Promise<T> promise = new Promise<>(this);
        issued++;
        io.add(() -> {
            T value;
            try {
                value = perform.get();
            } catch (RuntimeException e) {
                promise.reject(e);
                return;
            }
            promise.resolve(value);
        });
        return promise;
    }

    /**
     * How many I/O operations were issued so far: one issued later has at least this index.
     */
    public int issued() {
        return issued;
    }

    /**
     * Runs the loop until nothing is left to do.
     */
    public void run() {
        while (step()) {
            // each step runs one task
        }
    }

    /**
     * Runs the loop until the promise settles: its value, or what it was rejected with, thrown.
     */
    public <T> @Nullable T await(Promise<T> promise) {
        while (!promise.settled) {
            if (!step()) {
                throw new IllegalStateException("the promise never settles");
            }
        }
        if (promise.error != null) {
            throw promise.error;
        }
        return promise.value;
    }

    private boolean step() {
        Runnable task = microtasks.poll();
        if (task == null) {
            task = io.poll();
        }
        if (task == null) {
            return false;
        }
        task.run();
        return true;
    }

    /**
     * A JavaScript promise: its reactions run as microtasks once it settles.
     */
    public static final class Promise<T> {
        private final EventLoop loop;
        private final List<Runnable> reactions = new ArrayList<>();
        private boolean settled;
        private @Nullable T value;
        private @Nullable RuntimeException error;

        public Promise(EventLoop loop) {
            this.loop = loop;
        }

        public static <T> Promise<T> resolved(EventLoop loop, @Nullable T value) {
            Promise<T> promise = new Promise<>(loop);
            promise.resolve(value);
            return promise;
        }

        public static <T> Promise<T> rejected(EventLoop loop, RuntimeException error) {
            Promise<T> promise = new Promise<>(loop);
            promise.reject(error);
            return promise;
        }

        // an async function's body up to its first await: what it throws rejects the promise it returns
        public static <T> Promise<T> async(EventLoop loop, Supplier<Promise<T>> body) {
            try {
                return body.get();
            } catch (RuntimeException e) {
                return rejected(loop, e);
            }
        }

        public static <T> Promise<List<T>> all(EventLoop loop, List<Promise<T>> promises) {
            Promise<List<T>> all = new Promise<>(loop);
            if (promises.isEmpty()) {
                all.resolve(new ArrayList<>());
                return all;
            }
            Object[] values = new Object[promises.size()];
            int[] remaining = {promises.size()};
            for (int i = 0; i < promises.size(); i++) {
                int index = i;
                Promise<T> promise = promises.get(i);
                promise.react(() -> {
                    if (promise.error != null) {
                        all.reject(promise.error);
                        return;
                    }
                    values[index] = promise.value;
                    if (--remaining[0] == 0) {
                        @SuppressWarnings("unchecked")
                        List<T> list = (List<T>) new ArrayList<>(Arrays.asList(values));
                        all.resolve(list);
                    }
                });
            }
            return all;
        }

        // for (const promise of promises) await promise
        public static Promise<Void> awaitInOrder(EventLoop loop, List<Promise<Void>> promises) {
            Promise<Void> chain = resolved(loop, null);
            for (Promise<Void> promise : promises) {
                chain = chain.then(v -> promise);
            }
            return chain;
        }

        public void resolve(@Nullable T value) {
            if (settled) {
                return;
            }
            settled = true;
            this.value = value;
            flush();
        }

        public void reject(RuntimeException error) {
            if (settled) {
                return;
            }
            settled = true;
            this.error = error;
            flush();
        }

        public @Nullable RuntimeException getError() {
            return error;
        }

        public @Nullable T getValue() {
            return value;
        }

        private void flush() {
            for (Runnable reaction : reactions) {
                loop.microtasks.add(reaction);
            }
            reactions.clear();
        }

        private void react(Runnable reaction) {
            if (settled) {
                loop.microtasks.add(reaction);
            } else {
                reactions.add(reaction);
            }
        }

        /**
         * {@code await this}, then the rest of the async function, up to its next await.
         */
        public <U> Promise<U> then(Function<@Nullable T, Promise<U>> step) {
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

        /**
         * {@code try { await this } catch (e) { ... }}.
         */
        public Promise<T> recover(Function<RuntimeException, Promise<T>> recovery) {
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
    }
}
