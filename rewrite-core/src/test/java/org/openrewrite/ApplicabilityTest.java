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
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import static org.openrewrite.test.SourceSpecs.text;

class ApplicabilityTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.validateRecipeSerialization(false);
    }

    @Test
    void not() {
        rewriteRun(
          spec -> spec.recipe(recipe(Applicability.not(contains("z")))),
          text("hello", "goodbye")
        );
    }

    @Test
    void notNot() {
        rewriteRun(
          spec -> spec.recipe(recipe(Applicability.not(contains("h")))),
          text("hello")
        );
    }


    @Test
    void or() {
        rewriteRun(
          spec -> spec.recipe(recipe(Applicability.or(contains("h"), contains("z")))),
          text("hello", "goodbye")
        );
    }

    @Test
    void notOr() {
        rewriteRun(
          spec -> spec.recipe(recipe(Applicability.or(contains("x"), contains("z")))),
          text("hello")
        );
    }

    @Test
    void and() {
        rewriteRun(
          spec -> spec.recipe(recipe(Applicability.and(contains("h"), contains("ello")))),
          text("hello", "goodbye")
        );
    }

    @Test
    void notAnd() {
        rewriteRun(
          spec -> spec.recipe(recipe(Applicability.and(contains("h"), contains("z")))),
          text("hello")
        );
    }

    Recipe recipe(TreeVisitor<?, ExecutionContext> applicability) {
        return new Recipe() {
            @Override
            public String getDisplayName() {
                return "Say goodbye";
            }

            @Override
            protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
                return applicability;
            }

            @Override
            protected TreeVisitor<?, ExecutionContext> getVisitor() {
                return new PlainTextVisitor<>() {
                    @Override
                    public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                        return text.withText("goodbye");
                    }
                };
            }
        };
    }

    PlainTextVisitor<ExecutionContext> contains(String s) {
        return new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                if (text.getText().contains(s)) {
                    return SearchResult.found(text);
                }
                return text;
            }
        };
    }
}
