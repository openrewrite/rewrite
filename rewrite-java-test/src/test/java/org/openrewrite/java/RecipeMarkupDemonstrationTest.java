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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

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
    void exceptionsWithoutCircularReferences() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(r -> new JavaIsoVisitor<>() {
                @Override
                public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if (tree instanceof JavaSourceFile) {
                        Exception exception = new RuntimeException("this the parent of a circular exception");
                        Exception exception2 = new RuntimeException("this the child of a circular exception", exception);
                        exception.initCause(exception2);
                        JavaSourceFile sf = (JavaSourceFile) Markup.error(tree, exception);
                        Markup.Error marker = sf.getMarkers().findFirst(Markup.Error.class).get();
                        assert marker.getException().getCause() == null;
                        //Otherwise, there is an error if the exception is serialized.
                        return sf;
                    }
                    return (J) tree;
                }
            })),
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
}
