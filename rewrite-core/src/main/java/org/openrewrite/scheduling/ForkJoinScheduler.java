/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.scheduling;

import lombok.RequiredArgsConstructor;
import org.openrewrite.RecipeScheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class ForkJoinScheduler implements RecipeScheduler {
    private static final ForkJoinScheduler COMMON_SCHEDULER = new ForkJoinScheduler(new ForkJoinPool((int) (Runtime.getRuntime().availableProcessors() * 1.25)));

    private final ForkJoinPool forkJoinPool;

    public static ForkJoinScheduler common() {
        return COMMON_SCHEDULER;
    }

    @Override
    public <T> CompletableFuture<T> schedule(Callable<T> fn) {
        CompletableFuture<T> f = new CompletableFuture<>();
        forkJoinPool.submit(() -> {
            try {
                T obj = fn.call();
                f.complete(obj);
            } catch (Throwable t) {
                f.completeExceptionally(t);
            }
        });
        return f;
    }
}
