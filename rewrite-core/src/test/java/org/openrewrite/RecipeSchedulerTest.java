/*
 * Copyright 2022 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.marker.Markup;
import org.openrewrite.scheduling.RecipeRunCycle;
import org.openrewrite.scheduling.WatchableExecutionContext;
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SearchResults;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.openrewrite.scheduling.WorkingDirectoryExecutionContextView.WORKING_DIRECTORY_ROOT;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class RecipeSchedulerTest implements RewriteTest {

    @DocumentExample
    @Test
    void exceptionsCauseResult() {
        rewriteRun(
          spec -> spec
            .executionContext(new InMemoryExecutionContext())
            .recipe(new BoomRecipe())
            .afterRecipe(run -> {
                  SourceFile after = run.getChangeset().getAllResults().getFirst().getAfter();
                  assertThat(after).isNotNull();
                  assertThat(after.getMarkers().findFirst(Markup.Error.class))
                    .hasValueSatisfying(err -> {
                        assertThat(err.getMessage()).isEqualTo("boom");
                        assertThat(err.getDetail())
                          .matches("org.openrewrite.BoomException: boom" +
                            "\\s+org.openrewrite.BoomRecipe\\$1.visitText\\(RecipeSchedulerTest.java:\\d+\\)" +
                            "\\s+org.openrewrite.BoomRecipe\\$1.visitText\\(RecipeSchedulerTest.java:\\d+\\)");
                    });
              }
            ),
          text(
            "hello",
            "~~(boom)~~>hello"
          )
        );
    }

    @Test
    void exceptionDuringGenerate() {
        rewriteRun(
          spec -> spec.recipe(new BoomGenerateRecipe(false))
            .executionContext(new InMemoryExecutionContext())
            .dataTable(SourcesFileErrors.Row.class, rows ->
              assertThat(rows)
                .singleElement()
                .extracting(SourcesFileErrors.Row::getRecipe)
                .isEqualTo("org.openrewrite.BoomGenerateRecipe"))
        );
    }

    @Test
    void recipeRunExceptionDuringGenerate() {
        rewriteRun(
          spec -> spec.recipe(new BoomGenerateRecipe(true))
            .executionContext(new InMemoryExecutionContext())
            .dataTable(SourcesFileErrors.Row.class, rows ->
              assertThat(rows)
                .singleElement()
                .extracting(SourcesFileErrors.Row::getRecipe)
                .isEqualTo("org.openrewrite.BoomGenerateRecipe"))
        );
    }

    @Test
    void suppliedWorkingDirectoryRoot(@TempDir Path path) {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        WorkingDirectoryExecutionContextView.view(ctx).setRoot(path);
        AtomicInteger cycle = new AtomicInteger(0);
        rewriteRun(
          spec -> spec.executionContext(ctx).recipe(toRecipe(() -> new TreeVisitor<>() {
              @Override
              public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                  assert tree != null;
                  var plainText = (PlainText) tree;
                  Path workingDirectory = WorkingDirectoryExecutionContextView.view(ctx)
                    .getWorkingDirectory();
                  assertThat(workingDirectory).hasParent(path);
                  if (cycle.incrementAndGet() == 2) {
                      assertThat(workingDirectory.resolve("foo.txt")).hasContent("foo");
                  }
                  assertDoesNotThrow(() -> {
                      Files.writeString(workingDirectory.resolve("foo.txt"), plainText.getText());
                  });
                  return plainText.withText("bar");
              }
          })),
          text("foo", "bar")
        );
        assertThat(path).doesNotExist();
    }

    @Test
    void managedWorkingDirectoryWithRecipe(@TempDir Path path) {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        WorkingDirectoryExecutionContextView.view(ctx).setRoot(path);
        rewriteRun(
          spec -> spec.executionContext(ctx).recipe(new RecipeWritingToFile(0)),
          text("foo", "bar")
        );
        assertThat(path).doesNotExist();
    }

    @Test
    void managedWorkingDirectoryWithMultipleRecipes(@TempDir Path path) {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        WorkingDirectoryExecutionContextView.view(ctx).setRoot(path);
        DeclarativeRecipe recipe = new DeclarativeRecipe(
          "root",
          "Root recipe",
          "Root recipe.",
          emptySet(),
          null,
          URI.create("dummy:recipe.yml"),
          false,
          emptyList()
        );
        recipe.addUninitialized(new RecipeWritingToFile(1));
        recipe.addUninitialized(new RecipeWritingToFile(2));
        recipe.initialize(List.of());
        rewriteRun(
          spec -> spec.executionContext(ctx).recipe(recipe),
          text("foo", "bar")
        );
        assertThat(path).doesNotExist();
    }

    @Test
    void verifyCycleInvariantsDuringMultipleCycles() {
        List<Integer> cyclesFromFactory = new java.util.ArrayList<>();
        List<Integer> cyclesFromContext = new java.util.ArrayList<>();
        AtomicInteger visitCount = new AtomicInteger(0);

        RecipeScheduler trackingScheduler = new RecipeScheduler() {
            @Override
            protected RecipeRunCycle<LargeSourceSet> createRecipeRunCycle(
                    Recipe recipe, int cycle, Cursor rootCursor,
                    WatchableExecutionContext ctxWithWatch,
                    RecipeRunStats recipeRunStats, SearchResults searchResults,
                    SourcesFileResults sourceFileResults, SourcesFileErrors errorsTable) {
                cyclesFromFactory.add(cycle);
                return super.createRecipeRunCycle(recipe, cycle, rootCursor, ctxWithWatch,
                        recipeRunStats, searchResults, sourceFileResults, errorsTable);
            }
        };

        // Recipe that causes another cycle by returning different content each time (up to 2 times)
        Recipe multiCycleRecipe = toRecipe(() -> new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                // Verify cycle is accessible from context during visitor execution
                cyclesFromContext.add(ctx.getCycle());
                int count = visitCount.incrementAndGet();
                if (count <= 2) {
                    return text.withText(text.getText() + count);
                }
                return text;
            }
        }).withCausesAnotherCycle(true);

        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        List<SourceFile> sources = List.of(PlainText.builder().text("v").sourcePath(Path.of("test.txt")).build());
        trackingScheduler.scheduleRun(multiCycleRecipe, new InMemoryLargeSourceSet(sources), ctx, 5, 1);

        // Verify cycle numbers increment correctly: Cycle 1, 2, 3 (stops after no change in cycle 3)
        assertThat(cyclesFromFactory).containsExactly(1, 2, 3);
        // Verify cycle is correctly registered in context and accessible during visitor execution
        assertThat(cyclesFromContext).containsExactly(1, 2, 3);
    }

    @Test
    void recordsBeforeAndAfterSourceFilesCorrectly() {
        List<String> beforeContents = new java.util.ArrayList<>();
        List<String> afterContents = new java.util.ArrayList<>();

        RecipeScheduler trackingScheduler = new RecipeScheduler() {
            @Override
            protected RecipeRunCycle<LargeSourceSet> createRecipeRunCycle(
                    Recipe recipe, int cycle, Cursor rootCursor,
                    WatchableExecutionContext ctxWithWatch,
                    RecipeRunStats recipeRunStats, SearchResults searchResults,
                    SourcesFileResults sourceFileResults, SourcesFileErrors errorsTable) {
                return new RecipeRunCycle<>(recipe, cycle, rootCursor, ctxWithWatch,
                        recipeRunStats, searchResults, sourceFileResults, errorsTable, LargeSourceSet::edit) {
                    @Override
                    protected void recordSourceFileResultAndSearchResults(
                            @Nullable SourceFile before, @Nullable SourceFile after,
                            java.util.List<Recipe> recipeStack, ExecutionContext ctx) {
                        if (before instanceof PlainText) {
                            beforeContents.add(((PlainText) before).getText());
                        }
                        if (after instanceof PlainText) {
                            afterContents.add(((PlainText) after).getText());
                        }
                        super.recordSourceFileResultAndSearchResults(before, after, recipeStack, ctx);
                    }
                };
            }
        };

        Recipe recipe = toRecipe(() -> new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                return text.withText("modified:" + text.getText());
            }
        });

        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        List<SourceFile> sources = List.of(
                PlainText.builder().text("a").sourcePath(Path.of("a.txt")).build(),
                PlainText.builder().text("b").sourcePath(Path.of("b.txt")).build()
        );
        trackingScheduler.scheduleRun(recipe, new InMemoryLargeSourceSet(sources), ctx, 3, 1);

        assertThat(beforeContents).containsExactlyInAnyOrder("a", "b");
        assertThat(afterContents).containsExactlyInAnyOrder("modified:a", "modified:b");
    }

    @Test
    void recordsGeneratedSourceFiles() {
        List<String> generatedPaths = new java.util.ArrayList<>();

        RecipeScheduler trackingScheduler = new RecipeScheduler() {
            @Override
            protected RecipeRunCycle<LargeSourceSet> createRecipeRunCycle(
                    Recipe recipe, int cycle, Cursor rootCursor,
                    WatchableExecutionContext ctxWithWatch,
                    RecipeRunStats recipeRunStats, SearchResults searchResults,
                    SourcesFileResults sourceFileResults, SourcesFileErrors errorsTable) {
                return new RecipeRunCycle<>(recipe, cycle, rootCursor, ctxWithWatch,
                        recipeRunStats, searchResults, sourceFileResults, errorsTable, LargeSourceSet::edit) {
                    @Override
                    protected void recordSourceFileResultAndSearchResults(
                            @Nullable SourceFile before, @Nullable SourceFile after,
                            java.util.List<Recipe> recipeStack, ExecutionContext ctx) {
                        // Track files that were generated (before is null)
                        if (before == null && after != null) {
                            generatedPaths.add(after.getSourcePath().toString());
                        }
                        super.recordSourceFileResultAndSearchResults(before, after, recipeStack, ctx);
                    }
                };
            }
        };

        Recipe generatingRecipe = new GeneratingRecipe();

        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        List<SourceFile> sources = List.of(PlainText.builder().text("existing").sourcePath(Path.of("existing.txt")).build());
        trackingScheduler.scheduleRun(generatingRecipe, new InMemoryLargeSourceSet(sources), ctx, 3, 1);

        assertThat(generatedPaths).containsExactly("generated.txt");
    }
}

@AllArgsConstructor
class BoomRecipe extends Recipe {
    @Getter
    final String displayName = "We go boom";

    @Getter
    final String description = "Test recipe.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                throw new BoomException();
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@Value
class BoomGenerateRecipe extends ScanningRecipe<Integer> {

    boolean wrapAsRecipeRunException;

    String displayName = "Boom generate";

    String description = "Throws a boom exception during ScanningRecipe.generate().";

    @Override
    public Integer getInitialValue(ExecutionContext ctx) {
        return 0;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Integer acc) {
        return TreeVisitor.noop();
    }

    @Override
    public Collection<? extends SourceFile> generate(Integer acc, ExecutionContext ctx) {
        throw wrapAsRecipeRunException ? new RecipeRunException(new BoomException(), null) : new BoomException();
    }
}

/**
 * Simplified exception that only displays stack trace elements within the [BoomRecipe].
 */
class BoomException extends RuntimeException {
    public BoomException() {
        super("boom");
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return Arrays.stream(super.getStackTrace())
          .filter(st -> st.getClassName().startsWith(BoomRecipe.class.getName()))
          .toArray(StackTraceElement[]::new);
    }
}

class GeneratingRecipe extends ScanningRecipe<AtomicInteger> {
    @Getter
    final String displayName = "Generating recipe";

    @Getter
    final String description = "Generates a new file.";

    @Override
    public AtomicInteger getInitialValue(ExecutionContext ctx) {
        return new AtomicInteger(0);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicInteger acc) {
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                acc.incrementAndGet();
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(AtomicInteger acc, ExecutionContext ctx) {
        if (acc.get() > 0) {
            return List.of(PlainText.builder()
                    .text("generated content")
                    .sourcePath(Path.of("generated.txt"))
                    .build());
        }
        return List.of();
    }
}

@AllArgsConstructor
class RecipeWritingToFile extends ScanningRecipe<RecipeWritingToFile.Accumulator> {

    final int position;

    @Getter
    final String displayName = "Write text to a file";

    @Getter
    final String description = "Writes text to a file.";

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        Path workingDirectory = validateExecutionContext(ctx);
        return new Accumulator(workingDirectory);
    }

    private Path validateExecutionContext(ExecutionContext ctx) {
        Path workingDirectory = WorkingDirectoryExecutionContextView.view(ctx)
          .getWorkingDirectory();
        assertThat(workingDirectory).isDirectory();
        assertThat(workingDirectory).hasParent(ctx.getMessage(WORKING_DIRECTORY_ROOT));
        assertThat(ctx.getCycleDetails().getRecipePosition()).isEqualTo(position);
        assertThat(workingDirectory.getFileName().toString())
          .isEqualTo("cycle" + ctx.getCycle() + "_recipe" + position);
        return workingDirectory;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                assert tree != null;
                Path workingDirectory = validateExecutionContext(ctx);
                assertThat(acc.workingDirectory()).isEqualTo(workingDirectory);
                assertThat(workingDirectory).isEmptyDirectory();
                assertDoesNotThrow(() -> {
                    Files.writeString(workingDirectory.resolve("manifest.txt"), ((SourceFile) tree).getSourcePath().toString(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                });
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        Path workingDirectory = validateExecutionContext(ctx);
        assertThat(acc.workingDirectory()).isEqualTo(workingDirectory);
        assertThat(workingDirectory).isDirectoryContaining(path -> "manifest.txt".equals(path.getFileName().toString()));
        assertDoesNotThrow(() -> {
            assertThat(workingDirectory.resolve("manifest.txt")).hasContent("file.txt");
        });
        return List.of();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Path workingDirectory = WorkingDirectoryExecutionContextView
                  .view(ctx).getWorkingDirectory();
                assertThat(workingDirectory).isDirectory();
                assertThat(acc.workingDirectory()).isEqualTo(workingDirectory);
                assertThat(workingDirectory).isDirectoryContaining(path -> "manifest.txt".equals(path.getFileName().toString()));
                assertDoesNotThrow(() -> {
                    assertThat(workingDirectory.resolve("manifest.txt")).hasContent("file.txt");
                });
                assert tree instanceof PlainText;
                return ((PlainText) tree).withText("bar");
            }
        };
    }

    public record Accumulator(Path workingDirectory) {
    }
}
