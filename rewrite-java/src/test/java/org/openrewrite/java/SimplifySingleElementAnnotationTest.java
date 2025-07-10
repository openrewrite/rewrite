/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/**
 * Examples for tests taken here: <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.7.3">Single-Element Annotations</a>.
 */
class SimplifySingleElementAnnotationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SimplifySingleElementAnnotation());
    }

    /**
     * Here is an example of a single-element annotation.
     */
    @Test
    @DocumentExample
    void simpleExample() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  @interface Copyright {
                      String value();
                  }
                  """
              )
            ),
          java(
            """
              @Copyright(value = "2002 Yoyodyne Propulsion Systems, Inc.")
              class OscillationOverthruster {
              }
              """,
            """
              @Copyright("2002 Yoyodyne Propulsion Systems, Inc.")
              class OscillationOverthruster {
              }
              """
          )
        );
    }

    /**
     * Here is an example of an array-valued single-element annotation.
     */
    @Test
    void simpleExampleWithArray() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  @interface Endorsers {
                      String[] value();
                  }
                  """
              )
            ),
          java(
            """
              @Endorsers(value = {"Children", "Unscrupulous dentists"})
              public class Lollipop {
              }
              """,
            """
              @Endorsers({"Children", "Unscrupulous dentists"})
              public class Lollipop {
              }
              """
          )
        );
    }

    /**
     * Here is an example of a single-element array-valued single-element annotation
     * (note that the curly braces are omitted).
     */
    @Test
    void simpleExampleWithSingleElementArray() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  @interface Endorsers {
                      String[] value();
                  }
                  """
              )
            ),
          java(
            """
              @Endorsers(value = {"Epicurus"})
              public class Lollipop {
              }
              """,
            """
              @Endorsers("Epicurus")
              public class Lollipop {
              }
              """
          )
        );
    }

    /**
     * Here is an example of a single-element annotation that uses an enum type defined inside the annotation type.
     */
    @Test
    void simpleExampleWithSingleElementEnum() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  public @interface Quality {
                      Level value();
                  
                      enum Level {
                          POOR, AVERAGE, GOOD, EXCELLENT
                      }
                  }
                  """
              )
            ),
          java(
            """
              @Quality(value = Quality.Level.GOOD)
              class Karma {
              }
              """,
            """
              @Quality(Quality.Level.GOOD)
              class Karma {
              }
              """
          )
        );
    }

    @Test
    void noChanges() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  @interface Copyright {
                      String value();
                  }
                  """
              )
            ),
          java(
            """
              @Copyright("2002 Yoyodyne Propulsion Systems, Inc.")
              class OscillationOverthruster {
              }
              """
          )
        );
    }

}
