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

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.FindAndReplace;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
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
              public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
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
              .withMaxCycles(1)
            )
            .afterRecipe(run -> assertThat(run.getChangeset().getAllResults().stream()
              .map(r -> r.getRecipeDescriptorsThatMadeChanges().get(0).getName()))
              .containsOnly("test.GeneratingRecipe")),
          text(null, "test", spec -> spec.path("test.txt"))
        );
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class DeleteFirst extends Recipe {

        @Override
        public String getDisplayName() {
            return "Delete a file";
        }

        @Override
        public String getDescription() {
            return "Deletes a file early on in the recipe pipeline. " +
                   "Subsequent recipes should not be passed a null source file.";
        }

        @Override
        public List<Recipe> getRecipeList() {
            return Arrays.asList(
              new DeleteSourceFiles("test.txt"),
              new FindAndReplace("test", "", null, null, null, null, null));
        }
    }

    @Test
    void deletionWithSubsequentRecipes() {
        rewriteRun(
          spec -> spec.recipe(new DeleteFirst()),
          text("test", null, spec -> spec.path("test.txt"))
        );
    }

    @Test
    void deleteFileByReturningNullFromVisit() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new PlainTextVisitor<>() {
              @Override
              public @Nullable PlainText visit(@Nullable Tree tree, ExecutionContext ctx) {
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
            }),
          text(
            "Hello",
            "Change2Change1Hello"
          )
        );
    }

    private Recipe testRecipe(@Language("markdown") String name) {
        return toRecipe(() -> new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (!text.getText().contains(name)) {
                    return text.withText(name + text.getText());
                }
                return super.visitText(text, ctx);
            }
        }).withName(name);
    }

    @Test
    void canCallImperativeRecipeWithoutArgsFromDeclarative() {
        rewriteRun(spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: test.recipe
            displayName: Test Recipe
            recipeList:
              - org.openrewrite.NoArgRecipe
            """,
          "test.recipe"
          ),
          text("Hi", "NoArgRecipeHi"));
    }

    @Test
    void canCallImperativeRecipeWithUnnecessaryArgsFromDeclarative() {
        rewriteRun(spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: test.recipe
            displayName: Test Recipe
            recipeList:
              - org.openrewrite.NoArgRecipe:
                  foo: bar
            """,
            "test.recipe"
          ),
          text("Hi", "NoArgRecipeHi"));
    }

    @Test
    void canCallRecipeWithNoExplicitConstructor() {
        rewriteRun(spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: test.recipe
            displayName: Test Recipe
            recipeList:
              - org.openrewrite.DefaultConstructorRecipe
            """,
            "test.recipe"
          ),
          text("Hi", "DefaultConstructorRecipeHi"));
    }

    @Test
    void declarativeRecipeChain() {
        rewriteRun(spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: test.recipe.a
            displayName: Test Recipe
            recipeList:
              - test.recipe.b
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: test.recipe.b
            displayName: Test Recipe
            recipeList:
              - test.recipe.c
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: test.recipe.c
            displayName: Test Recipe
            recipeList:
              - org.openrewrite.NoArgRecipe
            """,
            "test.recipe.a"
          ),
          text("Hi", "NoArgRecipeHi"));
    }

    @Test
    void declarativeRecipeChainAcrossFiles() {
        rewriteRun(spec -> spec.recipe(Environment.builder()
            .load(new YamlResourceLoader(new ByteArrayInputStream("""
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: test.recipe.c
                displayName: Test Recipe
                recipeList:
                  - org.openrewrite.NoArgRecipe
                """.getBytes()),
              URI.create("rewrite.yml"), new Properties()))
            .load(new YamlResourceLoader(new ByteArrayInputStream("""
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: test.recipe.b
                displayName: Test Recipe
                recipeList:
                  - test.recipe.c
                """.getBytes()),
              URI.create("rewrite.yml"), new Properties()))
            .load(new YamlResourceLoader(new ByteArrayInputStream("""
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: test.recipe.a
                displayName: Test Recipe
                recipeList:
                  - test.recipe.b
                """.getBytes()),
              URI.create("rewrite.yml"), new Properties()))
            .build()
            .activateRecipes("test.recipe.a")),
          text("Hi", "NoArgRecipeHi"));
    }

    @Test
    void declarativeRecipeChainFromResources() {
        rewriteRun(spec -> spec.recipeFromResources("test.declarative.sample.a"),
          text("Hi", "after"));
    }

    @Test
    void declarativeRecipeChainFromResourcesIncludesImperativeRecipesInDescriptors() {
        rewriteRun(spec -> spec.recipeFromResources("test.declarative.sample.a")
            .afterRecipe(recipeRun -> assertThat(recipeRun.getChangeset().getAllResults().get(0)
              .getRecipeDescriptorsThatMadeChanges().get(0).getRecipeList().get(0).getRecipeList().get(0)
              .getDisplayName()).isEqualTo("Change text")),
          text("Hi", "after"));
    }

    @Test
    void declarativeRecipeChainFromResourcesLongList() {
        rewriteRun(spec -> spec.recipe(Environment.builder()
            .load(new YamlResourceLoader(requireNonNull(RecipeLifecycleTest.class.getResourceAsStream("/META-INF/rewrite/test-sample-a.yml")), URI.create("rewrite.yml"), new Properties()))
            .load(new YamlResourceLoader(requireNonNull(RecipeLifecycleTest.class.getResourceAsStream("/META-INF/rewrite/test-sample-b.yml")), URI.create("rewrite.yml"), new Properties()))
            .build()
            .activateRecipes("test.declarative.sample.a")),
          text("Hi", "after"));
    }
}

@SuppressWarnings("unused") // referenced in yaml
class DefaultConstructorRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "DefaultConstructorRecipe";
    }

    @Override
    public String getDescription() {
        return "DefaultConstructorRecipe.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (!text.getText().contains(getDisplayName())) {
                    return text.withText(getDisplayName() + text.getText());
                }
                return super.visitText(text, ctx);
            }
        };
    }
}

@SuppressWarnings("unused") // referenced in yaml
@NoArgsConstructor
class NoArgRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "NoArgRecipe";
    }

    @Override
    public String getDescription() {
        return "NoArgRecipe.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (!text.getText().contains(getDisplayName())) {
                    return text.withText(getDisplayName() + text.getText());
                }
                return super.visitText(text, ctx);
            }
        };
    }
}
