/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.ChangeText;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class DeclarativeRecipeTest implements RewriteTest {

    @Test
    void precondition() {
        rewriteRun(
          spec -> {
              spec.validateRecipeSerialization(false);
              DeclarativeRecipe dr = new DeclarativeRecipe("test", "test", "test", null,
                null, null, true, null);
              dr.addPrecondition(
                toRecipe(() -> new PlainTextVisitor<>() {
                    @Override
                    public PlainText visitText(PlainText text, ExecutionContext ctx) {
                        if ("1".equals(text.getText())) {
                            return SearchResult.found(text);
                        }
                        return text;
                    }
                })
              );
              dr.addUninitialized(
                new ChangeText("2")
              );
              dr.addUninitialized(
                new ChangeText("3")
              );
              dr.initialize(List.of(), Map.of());
              spec.recipe(dr);
          },
          text("1", "3"),
          text("2")
        );
    }

    @Test
    void yamlPrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.PreconditionTest
            preconditions:
              - org.openrewrite.text.Find:
                  find: 1
            recipeList:
              - org.openrewrite.text.ChangeText:
                 toText: 2
              - org.openrewrite.text.ChangeText:
                 toText: 3
            """, "org.openrewrite.PreconditionTest"),
          text("1", "3"),
          text("2")
        );
    }

    @Test
    void yamlPreconditionWithScanningRecipe() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.PreconditionTest
            preconditions:
              - org.openrewrite.text.Find:
                  find: 1
            recipeList:
              - org.openrewrite.text.CreateTextFile:
                 relativeFileName: test.txt
                 fileContents: "test"
            """, "org.openrewrite.PreconditionTest")
            .afterRecipe(run -> {
                assertThat(run.getChangeset().getAllResults()).anySatisfy(
                  s -> {
                      assertThat(s.getAfter()).isNotNull();
                      assertThat(s.getAfter().getSourcePath()).isEqualTo(Paths.get("test.txt"));
                  }
                );
            })
            .expectedCyclesThatMakeChanges(1),
          text("1")
        );
    }

    @Test
    void maxCycles() {
        rewriteRun(
          spec -> spec.recipe(new RepeatedFindAndReplace(".+", "$0+1", 1)),
          text("1", "1+1")
        );
        rewriteRun(
          spec -> spec.recipe(new RepeatedFindAndReplace(".+", "$0+1", 2)).expectedCyclesThatMakeChanges(2),
          text("1", "1+1+1")
        );
    }

    @Test
    void maxCyclesNested() {
        AtomicInteger cycleCount = new AtomicInteger();
        Recipe root = new MaxCycles(
          100,
          List.of(new MaxCycles(
              2,
              List.of(new RepeatedFindAndReplace(".+", "$0+1", 100))
            ),
            toRecipe(() -> new TreeVisitor<>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    cycleCount.incrementAndGet();
                    return tree;
                }
            })
          )
        );
        rewriteRun(
          spec -> spec.recipe(root).cycles(10).cycles(3).expectedCyclesThatMakeChanges(3),
          text("1", "1+1+1")
        );
        assertThat(cycleCount).hasValue(3);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class MaxCycles extends Recipe {
        int maxCycles;
        List<Recipe> recipeList;

        @Override
        public int maxCycles() {
            return maxCycles;
        }

        @Override
        public String getDisplayName() {
            return "Executes recipes multiple times";
        }

        @Override
        public String getDescription() {
            return "Executes recipes multiple times.";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class RepeatedFindAndReplace extends Recipe {
        String find;
        String replace;
        int maxCycles;

        @Override
        public int maxCycles() {
            return maxCycles;
        }

        @Override
        public String getDisplayName() {
            return "Repeated find and replace";
        }

        @Override
        public String getDescription() {
            return "Find and replace repeatedly.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new TreeVisitor<>() {
                @Override
                public @Nullable @NonNull Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    PlainText text = ((PlainText) tree);
                    assert text != null;
                    return text.withText(text.getText().replaceAll(find, replace));
                }
            };
        }
    }
}
