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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;

class RecipeMarkupDemonstrationTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"debug", "info", "warning", "error"})
    void markup(String level) {
        rewriteRun(
          spec -> spec.recipe(new RecipeMarkupDemonstration(level)),
          java(
            """
              class Test {
              }
              """,
            String.format("""
              /*~~(This is a%s %s message.)~~>*/class Test {
              }
              """, level.equals("error") || level.equals("info") ? "n" : "", level)
          )
        );
    }

    @Test
    void exceptionsWithoutCicularReferences() {
        Recipe testRecipe = new TestRecipe();
        rewriteRun(
          spec -> spec.recipe(testRecipe),
          java(
            """
              class Test {
              }
              """,
            """
              /*~~(this the parent of a circular exception)~~>*/class Test {
              }
              """)
        );
    }
    @EqualsAndHashCode(callSuper = false)
    static class TestRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "test recipe that produces an error";
        }
        @Override
        public String getDescription() {
            return "test recipe that produces an error using a circular exception cause.";
        }
        @Override
        protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
            return ListUtils.mapFirst(before, sourceFile -> {
                Exception exception = new RuntimeException("this the parent of a circular exception");
                Exception exception2 = new RuntimeException("this the child of a circular exception", exception);
                exception.initCause(exception2);
                SourceFile sf = Markup.error(sourceFile, exception);
                Markup.Error marker = sf.getMarkers().findFirst(Markup.Error.class).get();
                assert marker.getException().getCause() == null;
                //Otherwise, there is an error if the exception is serialized.
                return sf;
            });
        }
    };

}
