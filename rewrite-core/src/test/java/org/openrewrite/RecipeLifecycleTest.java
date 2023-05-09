/*
 * Copyright 2020 the original author or authors.
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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.openrewrite.Recipe.noop;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class RecipeLifecycleTest implements RewriteTest {

    @Test
    void panic() {
        var ctx = new InMemoryExecutionContext();
        ctx.putMessage(Recipe.PANIC, true);

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
              @Override
              public Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                  fail("Should never have reached a visit method");
                  return tree;
              }
          })).executionContext(ctx),
          text("hello")
        );
    }

    @DocumentExample
    @Test
    void generateFile() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe()
              .withGenerator(() -> List.of(PlainText.builder().sourcePath(Paths.get("test.txt")).text("test").build()))
              .withName("test.GeneratingRecipe")
            )
            .afterRecipe(run -> assertThat(run.getChangeset().getAllResults().stream()
              .map(r -> r.getRecipeDescriptorsThatMadeChanges().get(0).getName()))
              .containsOnly("test.GeneratingRecipe"))
            .cycles(1).expectedCyclesThatMakeChanges(1),
          text(null, "test", spec -> spec.path("test.txt"))
        );
    }

    @Test
    void deleteFile() {
        var changes = new Recipe() {
            @Override
            public String getName() {
                return "test.DeletingRecipe";
            }

            @Override
            public String getDisplayName() {
                return getName();
            }

            @Override
            public TreeVisitor<?, ExecutionContext> getVisitor() {
                return new TreeVisitor<>() {
                    @Override
                    public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                        return null;
                    }
                };
            }
        }.run(
          new InMemoryLargeSourceSet(List.of(PlainText.builder().sourcePath(Paths.get("test.txt")).text("test").build())),
          new InMemoryExecutionContext()
        ).getChangeset().getAllResults();

        assertThat(changes.stream().map(r -> r.getRecipeDescriptorsThatMadeChanges().get(0).getName()))
          .containsExactly("test.DeletingRecipe");
    }

    @Test
    void deleteFileByReturningNullFromVisit() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new PlainTextVisitor<>() {
              @Override
              public @Nullable PlainText visit(@Nullable Tree tree, ExecutionContext executionContext) {
                  return null;
              }
          })),
          text(
            "hello",
            (String) null
          )
        );
    }

    class FooVisitor<P> extends TreeVisitor<FooSource, P> {
        @Override
        public @Nullable FooSource preVisit(FooSource tree, P p) {
            //noinspection ConstantConditions
            if (!(tree instanceof FooSource)) {
                throw new RuntimeException("tree is not a FooSource");
            }
            return super.preVisit(tree, p);
        }

        @Override
        public @Nullable FooSource postVisit(FooSource tree, P p) {
            //noinspection ConstantConditions
            if (!(tree instanceof FooSource)) {
                throw new RuntimeException("tree is not a FooSource");
            }
            return super.postVisit(tree, p);
        }
    }

    class FooSource implements SourceFile {
        @Override
        public Path getSourcePath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends SourceFile> T withSourcePath(Path path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable Charset getCharset() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends SourceFile> T withCharset(Charset charset) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCharsetBomMarked() {
            return false;
        }

        @Override
        public <T extends SourceFile> T withCharsetBomMarked(boolean marked) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable Checksum getChecksum() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends SourceFile> T withChecksum(@Nullable Checksum checksum) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable FileAttributes getFileAttributes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends SourceFile> T withFileAttributes(@Nullable FileAttributes fileAttributes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UUID getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Markers getMarkers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends Tree> T withMarkers(Markers markers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends Tree> T withId(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
            return v.isAdaptableTo(FooVisitor.class);
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/389")
    @Test
    void sourceFilesAcceptOnlyApplicableVisitors() {
        var sources = List.of(new FooSource(), PlainText.builder().sourcePath(Paths.get("test.txt")).text("test").build());
        var fooVisitor = new FooVisitor<ExecutionContext>();
        var textVisitor = new PlainTextVisitor<ExecutionContext>();
        var ctx = new InMemoryExecutionContext();

        for (SourceFile source : sources) {
            fooVisitor.visit(source, ctx);
            textVisitor.visit(source, ctx);
        }
    }

    @DocumentExample
    @Test
    void accurateReportingOfRecipesMakingChanges() {
        rewriteRun(
          spec -> spec
            .recipes(testRecipe("Change1"), noop(), testRecipe("Change2"))
            .validateRecipeSerialization(false)
            .afterRecipe(run -> {
                var changes = run.getChangeset().getAllResults();
                assertThat(changes).hasSize(1);
                assertThat(changes.get(0).getRecipeDescriptorsThatMadeChanges().stream().map(RecipeDescriptor::getName))
                  .containsExactlyInAnyOrder("Change1", "Change2");
            })
            .cycles(1).expectedCyclesThatMakeChanges(1),
          text(
            "Hello",
            "Change2Change1Hello"
          )
        );
    }

    private Recipe testRecipe(@Language("markdown") String name) {
        return toRecipe(() -> new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                if (!text.getText().contains(name)) {
                    return text.withText(name + text.getText());
                }
                return super.visitText(text, executionContext);
            }
        }).withName(name);
    }
}
