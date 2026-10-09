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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Node's event loop as pnpm's resolution runs on it: microtasks drain before the next request completes, and requests
 * complete in the order they were issued.
 */
final class EventLoop {

    private final Deque<Runnable> microtasks = new ArrayDeque<>();
    private final Deque<Runnable> requests = new ArrayDeque<>();

    <T> Promise<T> request(Supplier<T> perform) {
        Promise<T> promise = new Promise<>(this);
        requests.add(() -> {
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
     * Runs the loop until the promise settles: its value, or what it was rejected with, thrown.
     */
    <T> T await(Promise<T> promise) {
        while (!promise.settled) {
            Runnable task = microtasks.poll();
            if (task == null) {
                task = requests.poll();
            }
            if (task == null) {
                throw new IllegalStateException("the resolution never settles");
            }
            task.run();
        }
        if (promise.error != null) {
            throw promise.error;
        }
        return promise.value;
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

        // an async function's body up to its first await: what it throws rejects the promise it returns
        static <T> Promise<T> async(EventLoop loop, Supplier<Promise<T>> body) {
            try {
                return body.get();
            } catch (RuntimeException e) {
                Promise<T> promise = new Promise<>(loop);
                promise.reject(e);
                return promise;
            }
        }

        static <T> Promise<List<T>> all(EventLoop loop, List<Promise<T>> promises) {
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

        boolean isSettled() {
            return settled;
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
        <U> Promise<U> then(Function<T, Promise<U>> step) {
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
        Promise<T> recover(Function<RuntimeException, Promise<T>> recovery) {
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
