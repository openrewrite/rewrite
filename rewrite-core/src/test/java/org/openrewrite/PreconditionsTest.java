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

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.other;
import static org.openrewrite.test.SourceSpecs.text;

class PreconditionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.validateRecipeSerialization(false);
    }

    @DocumentExample
    @Test
    void not() {
        rewriteRun(
          spec -> spec.recipe(recipe(Preconditions.not(contains("z")))),
          text("hello", "goodbye")
        );
    }

    @Test
    void notNot() {
        rewriteRun(
          spec -> spec.recipe(recipe(Preconditions.not(contains("h")))),
          text("hello")
        );
    }


    @Test
    void or() {
        rewriteRun(
          spec -> spec.recipe(recipe(Preconditions.or(contains("h"), contains("z")))),
          text("hello", "goodbye")
        );
    }

    @Test
    void notOr() {
        rewriteRun(
          spec -> spec.recipe(recipe(Preconditions.or(contains("x"), contains("z")))),
          text("hello")
        );
    }

    @Test
    void and() {
        rewriteRun(
          spec -> spec.recipe(recipe(Preconditions.and(contains("h"), contains("ello")))),
          text("hello", "goodbye")
        );
    }

    @Test
    void notAnd() {
        rewriteRun(
          spec -> spec.recipe(recipe(Preconditions.and(contains("h"), contains("z")))),
          text("hello")
        );
    }

    @Test
    void checkApplicabilityAgainstOtherSourceTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> Preconditions.check(
            new PlainTextVisitor<>(),
            new PlainTextVisitor<>()
          ))),
          other("hello")
        );
    }

    @Test
    void orOtherSourceType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> Preconditions.check(Preconditions.or(
              new PlainTextVisitor<>() {
                  @Nullable
                  @Override
                  public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                      return tree != null && ((PlainText) tree).getText().contains("foo") ? SearchResult.found(tree) : tree;
                  }
              }),
            new TreeVisitor<>() {
                @Override
                public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    return SearchResult.found(tree, "recipe");
                }
            })
          )),
          other("hello")
        );
    }

    @Test
    void andOtherSourceType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> Preconditions.check(Preconditions.and(
              new PlainTextVisitor<>() {
                  @Nullable
                  @Override
                  public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                      return tree != null && ((PlainText) tree).getText().contains("foo") ? SearchResult.found(tree) : tree;
                  }
              }),
            new TreeVisitor<>() {
                @Override
                public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    return SearchResult.found(tree, "recipe");
                }
            })
          )),
          other("hello")
        );
    }

    @Test
    void notOtherSourceType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> Preconditions.check(Preconditions.not(
              new PlainTextVisitor<>() {
                  @Nullable
                  @Override
                  public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                      return tree != null && ((PlainText) tree).getText().contains("foo") ? SearchResult.found(tree) : tree;
                  }
              }),
            new TreeVisitor<>() {
                @Override
                public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    return SearchResult.found(tree, "recipe");
                }
            })
          )),
          other("hello", "~~(recipe)~~>⚛⚛⚛ The contents of this file are not visible. ⚛⚛⚛")
        );
    }

    Recipe recipe(TreeVisitor<?, ExecutionContext> applicability) {
        return toRecipe(() -> Preconditions.check(applicability, new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                return text.withText("goodbye");
            }
        }));
    }

    PlainTextVisitor<ExecutionContext> contains(String s) {
        return new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (text.getText().contains(s)) {
                    return SearchResult.found(text);
                }
                return text;
            }
        };
    }
}
