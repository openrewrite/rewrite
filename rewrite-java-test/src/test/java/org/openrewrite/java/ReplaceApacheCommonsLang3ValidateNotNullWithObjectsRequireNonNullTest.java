package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceApacheCommonsLang3ValidateNotNullWithObjectsRequireNonNullTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("commons-lang3"))
          .recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java")
          .build()
          .activateRecipes("org.openrewrite.java.cleanup.ReplaceApacheCommonsLang3ValidateNotNullWithObjectsRequireNonNull"));
    }

    @Test
    void replaceMethodsWithOneArg(){
        rewriteRun(
          java(
            """
              import org.apache.commons.lang3.Validate;
              class Test {
                  void test(Object obj) {
                        Validate.notNull(obj);
                  }
              }
              """,
            """
              import java.util.Objects;
              class Test {
                  void test(Object obj) {
                        Objects.requireNonNull(obj);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodsWithTwoArg(){
        rewriteRun(
          java(
            """
                     import org.apache.commons.lang3.Validate;
                     class Test {
                         void test(Object obj) {
                               Validate.notNull(obj,"Object should not be null");
                         }
                     }
                  """,
            """
                    import java.util.Objects;
                    class Test {
                        void test(Object obj) {
                              Objects.requireNonNull(obj,"Object should not be null");
                        }
                    }
                 """

          )
        );
    }

    @Test
    void doNothingOnMethodsWithThreeArg(){
        rewriteRun(
          java(
            """
                     import org.apache.commons.lang3.Validate;
                     class Test {
                         void test(Object obj) {
                               Validate.notNull(obj,"Object in %s should not be null","request xyz");
                         }
                     }
                  """
          )
        );
    }
}
