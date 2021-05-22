package org.openrewrite.scheduling;

import org.openrewrite.RecipeScheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

public class ForkJoinScheduler implements RecipeScheduler {

    private static final ForkJoinScheduler COMMON_SCHEDULER = new ForkJoinScheduler(ForkJoinPool.commonPool());

    private final ForkJoinPool forkJoinPool;

    public ForkJoinScheduler(ForkJoinPool forkJoinPool) {
        this.forkJoinPool = forkJoinPool;
    }

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
            } catch (Exception e) {
                f.completeExceptionally(e);
            }
        });
        return f;
    }

    @Override
    public CompletionStage<Void> schedule(Runnable fn) {
        return schedule(() -> {
            fn.run();
            return null;
        });
    }
}
