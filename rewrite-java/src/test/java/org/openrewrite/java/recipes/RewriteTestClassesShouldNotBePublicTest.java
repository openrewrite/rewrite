/*
 * Copyright 2024 the original author or authors.
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

import static org.openrewrite.java.Assertions.java;

class RewriteTestClassesShouldNotBePublicTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
          .recipe(new RewriteTestClassesShouldNotBePublic());
    }

    @Test
    @DocumentExample
    void rewriteTestThatOverridesDefaults() {
        rewriteRun(
          java(
            """
              import org.openrewrite.test.RecipeSpec;
              import org.openrewrite.test.RewriteTest;
              
              // org.openrewrite.java.testing.cleanup.TestsShouldNotBePublicTest skips classes that override defaults()
              public class ATest implements RewriteTest {
                  @Override
                  public void defaults(RecipeSpec spec) {
                  }
              }
              """,
            """
              import org.openrewrite.test.RecipeSpec;
              import org.openrewrite.test.RewriteTest;

              // org.openrewrite.java.testing.cleanup.TestsShouldNotBePublicTest skips classes that override defaults()
              class ATest implements RewriteTest {
                  @Override
                  public void defaults(RecipeSpec spec) {
                  }
              }
              """
          )
        );
    }

    @Test
    void rewriteTestClassesNotPublic() {
        rewriteRun(
          java(
            """
              import org.openrewrite.test.RewriteTest;
              
              public class ATest implements RewriteTest {
              }
              """,
            """
              import org.openrewrite.test.RewriteTest;

              class ATest implements RewriteTest {
              }
              """
          )
        );
    }

    @Test
    void noChangePublicStaticMethod() {
        rewriteRun(
          java(
            """
              import org.openrewrite.test.RewriteTest;
              
              public class ATest implements RewriteTest {
                  public static void helper() {}
              }
              """
          )
        );
    }

    @Test
    void noChangePublicStaticField() {
        rewriteRun(
          java(
            """
              import org.openrewrite.test.RewriteTest;
              
              public class ATest implements RewriteTest {
                  public static String helper = "helper";
              }
              """
          )
        );
    }

    @Test
    void noChangeNotPublic() {
        rewriteRun(
          java(
            """
              import org.openrewrite.test.RewriteTest;

              class ATest implements RewriteTest {
              }
              """
          )
        );
    }

    @Test
    void noChangeNotRewriteTest() {
        rewriteRun(
          java(
            """
              public class ATest {
                  void testMethod() {
                  }
              }
              """
          )
        );
    }
}