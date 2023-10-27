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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateTestToRewrite8Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateTestToRewrite8())
          .parser(JavaParser.fromJavaVersion()
            .classpath(JavaParser.runtimeClasspath())
          ).afterTypeValidationOptions(TypeValidation.none());
    }

    @DocumentExample
    @Test
    void doNextToRecipes() {
        // language=java
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate;

              import org.junit.jupiter.api.Test;
              import org.openrewrite.java.JavaParser;
              import org.openrewrite.test.RecipeSpec;
              import org.openrewrite.test.RewriteTest;
              import org.openrewrite.text.ChangeText;

              import static org.openrewrite.test.SourceSpecs.text;

              class Test implements RewriteTest {
                  @Override
                  public void defaults(RecipeSpec spec) {
                      spec.recipe(new ChangeText("foo").doNext(new ChangeText("bar")))
                        .parser(JavaParser.fromJavaVersion()
                          .classpath(JavaParser.runtimeClasspath())
                        );
                  }

                  @Test
                  void changeText() {
                      rewriteRun(
                        spec -> spec.expectedCyclesThatMakeChanges(2)
                          .recipe(new ChangeText("foo")
                            .doNext(new ChangeText("bar"))
                            .doNext(new ChangeText("baz"))
                          ),
                        text(
                          "hello", "baz"
                        )
                      );
                  }
              }
              """,
            """
              package org.openrewrite.java.migrate;

              import org.junit.jupiter.api.Test;
              import org.openrewrite.java.JavaParser;
              import org.openrewrite.test.RecipeSpec;
              import org.openrewrite.test.RewriteTest;
              import org.openrewrite.text.ChangeText;

              import static org.openrewrite.test.SourceSpecs.text;

              class Test implements RewriteTest {
                  @Override
                  public void defaults(RecipeSpec spec) {
                      spec.recipes(new ChangeText("foo"), new ChangeText("bar"))
                        .parser(JavaParser.fromJavaVersion()
                          .classpath(JavaParser.runtimeClasspath())
                        );
                  }

                  @Test
                  void changeText() {
                      rewriteRun(
                        spec -> spec.expectedCyclesThatMakeChanges(2).recipes(new ChangeText("foo"), new ChangeText("bar"), new ChangeText("baz")),
                        text(
                          "hello", "baz"
                        )
                      );
                  }
              }
              """
          )
        );
    }
}
