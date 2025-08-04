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
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.marker.Markup;
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;
import org.openrewrite.table.SourcesFileErrors;
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
import java.util.Map;
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
                  PlainText plainText = (PlainText) tree;
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
        recipe.initialize(List.of(), Map.of());
        rewriteRun(
          spec -> spec.executionContext(ctx).recipe(recipe),
          text("foo", "bar")
        );
        assertThat(path).doesNotExist();
    }
}

@AllArgsConstructor
class BoomRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "We go boom";
    }

    @Override
    public String getDescription() {
        return "Test recipe.";
    }

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

    @Override
    public String getDisplayName() {
        return "Boom generate";
    }

    @Override
    public String getDescription() {
        return "Throws a boom exception during ScanningRecipe.generate().";
    }

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

@AllArgsConstructor
class RecipeWritingToFile extends ScanningRecipe<RecipeWritingToFile.Accumulator> {

    final int position;

    @Override
    public String getDisplayName() {
        return "Write text to a file";
    }

    @Override
    public String getDescription() {
        return "Writes text to a file.";
    }

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
