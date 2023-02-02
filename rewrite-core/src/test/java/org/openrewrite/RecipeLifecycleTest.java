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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.ChangeText;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.openrewrite.Recipe.NOOP;
import static org.openrewrite.Recipe.noop;
import static org.openrewrite.Tree.randomId;
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

    @Test
    void notApplicableRecipe() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new PlainTextVisitor<>() {
              @Override
              public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                  return text.withText("goodbye");
              }
          }).addApplicableTest(NOOP)),
          text("hello")
        );
    }

    @Test
    void generateFile() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe()
              .withVisit((before, ctx) -> ListUtils.concat(before, new PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "test")))
              .withName("test.GeneratingRecipe")
            )
            .afterRecipe(run -> assertThat(run.getResults().stream()
              .map(r -> r.getRecipeDescriptorsThatMadeChanges().get(0).getName()))
              .containsOnly("test.GeneratingRecipe"))
            .cycles(1).expectedCyclesThatMakeChanges(1),
          text(null, "test", spec -> spec.path("test.txt"))
        );
    }

    @Test
    void deleteFile() {
        var results = new Recipe() {
            @Override
            public String getName() {
                return "test.DeletingRecipe";
            }

            @Override
            public String getDisplayName() {
                return getName();
            }

            @Override
            protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
                return Collections.emptyList();
            }
        }.run(List.of(new PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "test"))).getResults();

        assertThat(results.stream().map(r -> r.getRecipeDescriptorsThatMadeChanges().get(0).getName()))
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

    @Issue("https://github.com/openrewrite/rewrite/issues/2711")
    @Test
    void yamlApplicabilityWithAnySource() {
        //language=yaml
        String yamlRecipe = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.ApplicabilityExactlyOnce
          displayName: Applicability test runs once for the whole recipe list
          description: >
            An applicability test should be run once and if a match is found, all recipes in the list should be run.
            So if one of the recipes in the list makes a change which would cause the applicability test to no longer match,
            subsequent recipes in the list should still execute.
            
            Given a text file containing the number "1", running this recipe should result in a file which contains "3".
            If the applicability test is incorrectly applied to individual recipes in the list, the (incorrect) result would be "2".
          applicability:
            anySource:
              - org.openrewrite.text.FindAndReplace:
                  find: "1"
                  replace: "Applicable"
          recipeList:
            - org.openrewrite.text.ChangeText:
                  toText: "2"
            - org.openrewrite.text.ChangeText:
                  toText: "3"
          """;
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
            .scanRuntimeClasspath()
            .load(
              new YamlResourceLoader(
                new ByteArrayInputStream(yamlRecipe.getBytes(StandardCharsets.UTF_8)),
                Paths.get("applicability.yml").toUri(),
                new Properties()))
            .build()
            .activateRecipes("org.openrewrite.ApplicabilityExactlyOnce")),
          text("1", "3"),
          text("unknown", "3")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2711")
    @Test
    void yamlApplicabilityWithSingleSource() {
        //language=yaml
        String yamlRecipe = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.ApplicabilityExactlyOnce
          displayName: Applicability test runs once for the whole recipe list
          description: >
            An applicability test should be run once and if a match is found, all recipes in the list should be run.
            So if one of the recipes in the list makes a change which would cause the applicability test to no longer match,
            subsequent recipes in the list should still execute.
            
            Given a text file containing the number "1", running this recipe should result in a file which contains "3".
            If the applicability test is incorrectly applied to individual recipes in the list, the (incorrect) result would be "2".
          applicability:
            singleSource:
              - org.openrewrite.text.FindAndReplace:
                  find: "1"
                  replace: "A"
          recipeList:
            - org.openrewrite.text.ChangeText:
                  toText: "2"
            - org.openrewrite.text.ChangeText:
                  toText: "3"
          """;
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
              .scanRuntimeClasspath()
              .load(
                new YamlResourceLoader(
                  new ByteArrayInputStream(yamlRecipe.getBytes(StandardCharsets.UTF_8)),
                  Paths.get("applicability.yml").toUri(),
                  new Properties()))
              .build()
              .activateRecipes("org.openrewrite.ApplicabilityExactlyOnce")),
          text("1", "3"),
          text("2")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2754")
    @Test
    void yamlApplicabilityTrueWithRecipesHaveVisitMethodOverridden() {
        //language=yaml
        String yamlRecipe = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.RecipesToTransformMultiFiles
          displayName: Recipes in the list which override 'visit()' method should be run if applicability test pass
          description: >
            A recipe has two different ways to run.
            1. Override 'getVisitor()' method, and invoke `getVisitor().visit(@Nullable Tree tree, P p)` to transform a single file.
            2. Override `visit(List<SourceFile> before, ExecutionContext ctx)` method, and invoke it to transform multiple files.
            Typically, for a recipe, only one of the two methods mentioned above is required to be overridden.
            The recipe scheduler invokes both methods in different places in the flow, this test is intended to make sure 
            those recipes that overrides 'visit()' method can be run correctly with the applicability test.
            
            All recipes in the list should be run only when the applicability test pass.
            
            Given a text file containing the number "1", running this recipe should result in a file which has text "1->2->3".
          applicability:
            singleSource:
              - org.openrewrite.text.FindAndReplace:
                  find: "1"
                  replace: "A"
          recipeList:
            - org.openrewrite.text.AppendToTextFile:
                  relativeFileName: "file.txt"
                  content: "->2"
                  preamble: "preamble"
                  appendNewline : false
                  existingFileStrategy: "continue"
            - org.openrewrite.text.AppendToTextFile:
                  relativeFileName: "file.txt"
                  content: "->3"
                  preamble: "preamble"
                  appendNewline : false
                  existingFileStrategy: "continue"
          """;
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
            .scanRuntimeClasspath()
            .load(
              new YamlResourceLoader(
                new ByteArrayInputStream(yamlRecipe.getBytes(StandardCharsets.UTF_8)),
                Paths.get("applicability.yml").toUri(),
                new Properties()))
            .build()
            .activateRecipes("org.openrewrite.RecipesToTransformMultiFiles")),
          text("1",
            "1->2->3",
            spec -> spec.path("file.txt").noTrim())
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2754")
    @Disabled
    @Test
    void yamlApplicabilityFalseWithRecipesHaveVisitMethodOverridden() {
        //language=yaml
        String yamlRecipe = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.RecipesToTransformMultiFiles
          displayName: Recipes in the list which override 'visit()' method should not be run if applicability test fail
          description: >
            A recipe has two different ways to run.
            1. Override 'getVisitor()' method, and invoke `getVisitor().visit(@Nullable Tree tree, P p)` to transform a single file.
            2. Override `visit(List<SourceFile> before, ExecutionContext ctx)` method, and invoke it to transform multiple files.
            Typically, for a recipe, only one of the two methods mentioned above is required to be overridden.
            The recipe scheduler invokes both methods in different places in the flow, this test is intended to make sure 
            those recipes that overrides 'visit()' method can be run correctly with the applicability test.
            
            All recipes in the list should not be run only when the applicability test fail.
            
            Given a text file containing the number "2", running this recipe should result with no change.
          applicability:
            singleSource:
              - org.openrewrite.text.FindAndReplace:
                  find: "1"
                  replace: "A"
          recipeList:
            - org.openrewrite.text.AppendToTextFile:
                  relativeFileName: "file.txt"
                  content: "->2"
                  preamble: "preamble"
                  appendNewline : false
                  existingFileStrategy: "continue"
            - org.openrewrite.text.AppendToTextFile:
                  relativeFileName: "file.txt"
                  content: "->3"
                  preamble: "preamble"
                  appendNewline : false
                  existingFileStrategy: "continue"
          """;
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
            .scanRuntimeClasspath()
            .load(
              new YamlResourceLoader(
                new ByteArrayInputStream(yamlRecipe.getBytes(StandardCharsets.UTF_8)),
                Paths.get("applicability.yml").toUri(),
                new Properties()))
            .build()
            .activateRecipes("org.openrewrite.RecipesToTransformMultiFiles")),
          text("2",
            spec -> spec.path("file.txt").noTrim())
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
        var sources = List.of(new FooSource(), new PlainText(randomId(), Paths.get("test.txt"), Markers.build(List.of()), null, false, null, null, "Hello"));
        var fooVisitor = new FooVisitor<ExecutionContext>();
        var textVisitor = new PlainTextVisitor<ExecutionContext>();
        var ctx = new InMemoryExecutionContext();

        for (SourceFile source : sources) {
            fooVisitor.visit(source, ctx);
            textVisitor.visit(source, ctx);
        }
    }

    @Test
    void accurateReportingOfRecipesMakingChanges() {
        rewriteRun(
          spec -> spec
            .recipe(testRecipe("Change1").doNext(noop()).doNext(testRecipe("Change2")))
            .validateRecipeSerialization(false)
            .afterRecipe(run -> {
                var results = run.getResults();
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getRecipeDescriptorsThatMadeChanges().stream().map(RecipeDescriptor::getName))
                  .containsExactlyInAnyOrder("Change1", "Change2");
            })
            .cycles(1).expectedCyclesThatMakeChanges(1),
          text(
            "Hello",
            "Change2Change1Hello"
          )
        );
    }

    @Test
    void recipeDescriptorsReturnCorrectStructure() {
        Recipe r = noop();
        r.doNext(testRecipe("A")
          .doNext(testRecipe("B")
            .doNext(testRecipe("D")
              .doNext(testRecipe("C"))))
          .doNext(noop()));
        r.doNext(testRecipe("A")
          .doNext(testRecipe("B")
            .doNext(testRecipe("E"))
            .doNext(new ChangeText("E1"))
            .doNext(new ChangeText("E2"))));
        r.doNext(testRecipe("E")
          .doNext(testRecipe("F")));
        r.doNext(noop());

        rewriteRun(
          spec -> spec
            .recipe(r)
            .validateRecipeSerialization(false)
            .afterRecipe(run -> {
                var results = run.getResults();
                var recipeDescriptors = results.get(0).getRecipeDescriptorsThatMadeChanges();
                assertThat(recipeDescriptors).hasSize(2);

                var aDescriptor = recipeDescriptors.get(0);
                var bDescriptor = aDescriptor.getRecipeList().get(0);
                // B (2 test recipes, 2 ChangeText with different options and 1 noChangeRecipe) resulting in 4 changes
                assertThat(bDescriptor.getName()).isEqualTo("B");
                assertThat(bDescriptor.getRecipeList()).hasSize(4);
            })
            .cycles(1).expectedCyclesThatMakeChanges(1),
          text(
            "Hello",
            "FE2")
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
