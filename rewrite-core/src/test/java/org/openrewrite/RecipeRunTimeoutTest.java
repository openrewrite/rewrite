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
package org.openrewrite;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD;
import static org.openrewrite.test.RewriteTest.toRecipe;

class RecipeRunTimeoutTest {

    @Test
    void runTimeoutSkipsScanningAndGeneration() {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        AtomicInteger timeouts = new AtomicInteger();
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add, Duration.ZERO, (t, c) -> timeouts.incrementAndGet());
        ScanTracking recipe = new ScanTracking(Duration.ZERO);

        RecipeRun run = new RecipeScheduler().scheduleRun(recipe, sources("a.txt", "b.txt"), ctx, 3, 1);

        assertThat(recipe.scanned).isEmpty();
        assertThat(recipe.generated).isFalse();
        assertThat(run.getChangeset().getAllResults()).isEmpty();
        assertThat(timeouts).hasValue(1);
        assertThat(errors).singleElement().isInstanceOf(RecipeTimeoutException.class);
    }

    @Test
    void runTimeoutStopsScanningBetweenFiles() {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        AtomicInteger timeouts = new AtomicInteger();
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add, Duration.ofSeconds(1), (t, c) -> timeouts.incrementAndGet());
        ScanTracking recipe = new ScanTracking(Duration.ofMillis(1_100));

        RecipeRun run = new RecipeScheduler().scheduleRun(recipe, sources("a.txt", "b.txt", "c.txt"), ctx, 3, 1);

        assertThat(recipe.scanned).containsExactly("a.txt");
        assertThat(recipe.generated).isFalse();
        assertThat(run.getChangeset().getAllResults()).isEmpty();
        assertThat(timeouts).hasValue(1);
        assertThat(errors).singleElement().isInstanceOf(RecipeTimeoutException.class);
    }

    @Test
    void throwingTimeoutCallbackIsRecordedInEveryPhase() {
        // The run timeout first trips in the scan, generate and edit phase respectively
        assertThrowingTimeoutCallbackRecorded("scan", new ScanTracking(Duration.ZERO), sources("a.txt"), "a.txt");
        assertThrowingTimeoutCallbackRecorded("generate", new ScanTracking(Duration.ZERO), sources(), "error during generation");
        assertThrowingTimeoutCallbackRecorded("edit", toRecipe(() -> new PlainTextVisitor<>()), sources("a.txt"), "a.txt");
    }

    private static void assertThrowingTimeoutCallbackRecorded(String phase, Recipe recipe, LargeSourceSet sources, String errorPath) {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        IllegalStateException callbackError = new IllegalStateException("onTimeout failed");
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add, Duration.ZERO, (t, c) -> {
            throw callbackError;
        });

        RecipeRun run = new RecipeScheduler().scheduleRun(recipe, sources, ctx, 3, 1);

        assertThat(errors).as(phase).hasSize(2);
        assertThat(errors.get(0)).as(phase).isInstanceOf(RecipeTimeoutException.class);
        assertThat(errors.get(1)).as(phase).isSameAs(callbackError);
        assertThat(run.getDataTableRows(SourcesFileErrors.class)).as(phase)
          .singleElement()
          .satisfies(row -> {
              assertThat(row.getSourcePath()).isEqualTo(errorPath);
              assertThat(row.getStackTrace()).startsWith(IllegalStateException.class.getName() + ": onTimeout failed");
          });
    }

    @Test
    void timeoutCallbackFiresEvenIfErrorCallbackThrows() {
        AtomicReference<Throwable> timedOut = new AtomicReference<>();
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            if (t instanceof RecipeTimeoutException) {
                throw new IllegalStateException("onError failed");
            }
        }, Duration.ZERO, (t, c) -> timedOut.set(t));

        RecipeRun run = new RecipeScheduler().scheduleRun(toRecipe(() -> new PlainTextVisitor<>()), sources("a.txt"), ctx, 3, 1);

        assertThat(timedOut.get()).isInstanceOf(RecipeTimeoutException.class);
        assertThat(run.getDataTableRows(SourcesFileErrors.class)).singleElement()
          .satisfies(row -> assertThat(row.getStackTrace()).startsWith(IllegalStateException.class.getName() + ": onError failed"));
    }

    @Test
    @Timeout(value = 30, threadMode = SEPARATE_THREAD)
    void sourceFileTimeoutAbandonsRunawayEdit() {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add);
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, Duration.ofMillis(100));
        Recipe recipe = toRecipe(() -> new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (isRunaway(text)) {
                    revisitForever(this, text, ctx);
                }
                return text.withText(text.getText() + "!");
            }
        });

        RecipeRun run = new RecipeScheduler().scheduleRun(recipe, sources("runaway.txt", "normal.txt"), ctx, 3, 1);

        assertThat(after(run, "normal.txt").getText()).isEqualTo("normal.txt!");
        PlainText runaway = after(run, "runaway.txt");
        assertThat(runaway.getText()).isEqualTo("runaway.txt");
        assertThat(runaway.getSnippets()).singleElement()
          .satisfies(s -> assertThat(s.getMarkers().findFirst(Markup.Error.class))
            .hasValueSatisfying(e -> assertThat(e.getMessage()).contains("exceeded the source file timeout")));
        assertSourceFileTimeoutRecorded(run, errors);
    }

    @Test
    @Timeout(value = 30, threadMode = SEPARATE_THREAD)
    void sourceFileTimeoutAbandonsRunawayScan() {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add);
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, Duration.ofMillis(100));
        ScanTracking recipe = new ScanTracking(Duration.ZERO);

        RecipeRun run = new RecipeScheduler().scheduleRun(recipe, sources("runaway.txt", "normal.txt"), ctx, 3, 1);

        assertThat(recipe.scanned).containsExactly("normal.txt");
        // The accumulator is missing runaway.txt, so acting on it could generate or edit the wrong thing
        assertThat(recipe.generated).isFalse();
        assertThat(recipe.edited).isEmpty();
        assertThat(run.getChangeset().getAllResults())
          .extracting(r -> requireNonNull(r.getAfter()).getSourcePath())
          .containsExactly(Path.of("runaway.txt"));
        assertSourceFileTimeoutRecorded(run, errors);
    }

    @Test
    @Timeout(value = 30, threadMode = SEPARATE_THREAD)
    void sourceFileTimeoutAbandonsScanThatSwallowsTimeout() {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add);
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, Duration.ofMillis(100));
        ScanTracking recipe = new ScanTracking(Duration.ZERO, Runaway.SWALLOWS_TIMEOUT);

        RecipeRun run = new RecipeScheduler().scheduleRun(recipe, sources("runaway.txt", "normal.txt"), ctx, 3, 1);

        assertThat(recipe.scanned).containsExactly("runaway.txt", "normal.txt");
        assertThat(recipe.generated).isFalse();
        assertThat(recipe.edited).isEmpty();
        assertThat(run.getChangeset().getAllResults())
          .extracting(r -> requireNonNull(r.getAfter()).getSourcePath())
          .containsExactly(Path.of("runaway.txt"));
        assertSourceFileTimeoutRecorded(run, errors);
    }

    @Test
    @Timeout(value = 30, threadMode = SEPARATE_THREAD)
    void abandonedScanOnlyStopsThatRecipe() {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add);
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, Duration.ofMillis(100));
        ScanTracking runaway = new ScanTracking(Duration.ZERO);
        ScanTracking healthy = new ScanTracking(Duration.ZERO, Runaway.NEVER);

        RecipeRun run = new RecipeScheduler().scheduleRun(new CompositeRecipe(List.of(runaway, healthy)),
          sources("runaway.txt", "normal.txt"), ctx, 3, 1);

        assertThat(runaway.generated).isFalse();
        assertThat(runaway.edited).isEmpty();
        assertThat(healthy.scanned).containsExactly("runaway.txt", "normal.txt");
        assertThat(after(run, "generated.txt").getText()).isEqualTo("runaway.txt,normal.txt");
        assertThat(healthy.edited).containsExactlyInAnyOrder("runaway.txt", "normal.txt", "generated.txt");
        assertSourceFileTimeoutRecorded(run, errors);
    }

    @Test
    @Timeout(value = 30, threadMode = SEPARATE_THREAD)
    void sourceFileTimeoutDiscardsEditThatSwallowsTimeout() {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add);
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, Duration.ofMillis(100));
        Recipe recipe = toRecipe(() -> new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (isRunaway(text)) {
                    revisitUntilTimeoutCaught(text, ctx, getCursor().getParentOrThrow());
                }
                return text.withText(text.getText() + "!");
            }
        });

        RecipeRun run = new RecipeScheduler().scheduleRun(recipe, sources("runaway.txt", "normal.txt"), ctx, 3, 1);

        assertThat(run.getChangeset().getAllResults())
          .extracting(r -> requireNonNull(r.getAfter()).getSourcePath())
          .containsExactly(Path.of("normal.txt"));
        assertThat(after(run, "normal.txt").getText()).isEqualTo("normal.txt!");
        assertSourceFileTimeoutRecorded(run, errors);
    }

    @Test
    @Timeout(value = 30, threadMode = SEPARATE_THREAD)
    void sourceFileTimeoutAbandonsLoopOfNestedVisitors() {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add);
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, Duration.ofMillis(100));
        Recipe recipe = toRecipe(() -> new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (isRunaway(text)) {
                    //noinspection InfiniteLoopStatement
                    while (true) {
                        // Each nested visitor visits too few nodes to reach a periodic check on its own
                        new PlainTextVisitor<ExecutionContext>().visit(text, ctx, getCursor().getParentOrThrow());
                    }
                }
                return text;
            }
        });

        RecipeRun run = new RecipeScheduler().scheduleRun(recipe, sources("runaway.txt"), ctx, 3, 1);

        assertSourceFileTimeoutRecorded(run, errors);
    }

    @Test
    void sourceFileTimeoutOnlyAppliesWhenExceeded() {
        Recipe recipe = toRecipe(() -> new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                // Enough visits for several periodic deadline checks
                for (int i = 0; i < 10_000; i++) {
                    super.visitText(text, ctx);
                }
                return text.withText(text.getText() + "!");
            }
        });

        for (@Nullable Duration timeout : Arrays.asList(null, Duration.ofMinutes(1))) {
            List<Throwable> errors = new CopyOnWriteArrayList<>();
            ExecutionContext ctx = new InMemoryExecutionContext(errors::add);
            ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, timeout);

            RecipeRun run = new RecipeScheduler().scheduleRun(recipe, sources("a.txt", "b.txt"), ctx, 1, 1);

            assertThat(errors).isEmpty();
            assertThat(after(run, "a.txt").getText()).isEqualTo("a.txt!");
            assertThat(after(run, "b.txt").getText()).isEqualTo("b.txt!");
            assertThat(run.getDataTableRows(SourcesFileErrors.class)).isEmpty();
        }
    }

    private static void assertSourceFileTimeoutRecorded(RecipeRun run, List<Throwable> errors) {
        assertThat(errors).singleElement()
          .extracting(e -> e instanceof RecipeRunException ? e.getCause() : e)
          .isInstanceOfSatisfying(SourceFileTimeoutException.class,
            e -> assertThat(e.getSourcePath()).isEqualTo(Path.of("runaway.txt")));
        assertThat(run.getDataTableRows(SourcesFileErrors.class))
          .singleElement()
          .satisfies(row -> {
              assertThat(row.getSourcePath()).isEqualTo("runaway.txt");
              assertThat(row.getStackTrace()).startsWith(SourceFileTimeoutException.class.getName());
          });
    }

    private static boolean isRunaway(PlainText text) {
        return "runaway.txt".equals(text.getSourcePath().toString());
    }

    private static <P> void revisitForever(PlainTextVisitor<P> visitor, PlainText text, P p) {
        //noinspection InfiniteLoopStatement
        while (true) {
            for (PlainText.Snippet snippet : text.getSnippets()) {
                visitor.visit(snippet, p);
            }
        }
    }

    // Like JavaTemplateSemanticallyEqual.matchesTemplate, which reports "no match" on any RuntimeException
    private static void revisitUntilTimeoutCaught(PlainText text, ExecutionContext ctx, Cursor parent) {
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                new PlainTextVisitor<ExecutionContext>().visit(text, ctx, parent);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static PlainText after(RecipeRun run, String path) {
        return run.getChangeset().getAllResults().stream()
          .map(Result::getAfter)
          .filter(s -> s != null && s.getSourcePath().toString().equals(path))
          .map(PlainText.class::cast)
          .findFirst()
          .orElseThrow(() -> new AssertionError("No result for " + path));
    }

    private static LargeSourceSet sources(String... paths) {
        List<SourceFile> sources = Arrays.stream(paths)
          .map(p -> PlainText.builder()
            .sourcePath(Path.of(p))
            .text(p)
            .snippets(singletonList(new PlainText.Snippet(Tree.randomId(), Markers.EMPTY, "")))
            .build())
          .collect(toList());
        return new InMemoryLargeSourceSet(sources);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    enum Runaway {
        NEVER,
        FOREVER,
        SWALLOWS_TIMEOUT
    }

    static class ScanTracking extends ScanningRecipe<List<String>> {
        final List<String> scanned = new CopyOnWriteArrayList<>();
        final List<String> edited = new CopyOnWriteArrayList<>();
        final AtomicBoolean generated = new AtomicBoolean();
        final Duration scanDelay;
        final Runaway runaway;

        ScanTracking(Duration scanDelay) {
            this(scanDelay, Runaway.FOREVER);
        }

        ScanTracking(Duration scanDelay, Runaway runaway) {
            this.scanDelay = scanDelay;
            this.runaway = runaway;
        }

        @Override
        public String getDisplayName() {
            return "Scan tracking";
        }

        @Override
        public String getDescription() {
            return "Records which files were scanned and edited and generates a file listing those scanned.";
        }

        @Override
        public List<String> getInitialValue(ExecutionContext ctx) {
            return new CopyOnWriteArrayList<>();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(List<String> acc) {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    if (isRunaway(text)) {
                        if (runaway == Runaway.FOREVER) {
                            revisitForever(this, text, ctx);
                        } else if (runaway == Runaway.SWALLOWS_TIMEOUT) {
                            revisitUntilTimeoutCaught(text, ctx, getCursor().getParentOrThrow());
                        }
                    }
                    sleep(scanDelay);
                    String path = text.getSourcePath().toString();
                    scanned.add(path);
                    acc.add(path);
                    return text;
                }
            };
        }

        @Override
        public Collection<? extends SourceFile> generate(List<String> acc, ExecutionContext ctx) {
            generated.set(true);
            return singletonList(PlainText.builder()
              .sourcePath(Path.of("generated.txt"))
              .text(String.join(",", acc))
              .build());
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(List<String> acc) {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    edited.add(text.getSourcePath().toString());
                    return text;
                }
            };
        }
    }
}
