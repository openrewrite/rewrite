package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ReplaceValidateNotNullHavingVarargsWithObjectsRequireNonNullTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
            .parser(JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(true)
                .classpath("commons-lang3"))
            .recipe(new ReplaceValidateNotNullHavingVarargsWithObjectsRequireNonNull());
    }

    @Test
    void doNothingIfMethodNotFound(){
        rewriteRun(
            java(
                """
                  import org.apache.commons.lang3.Validate;
                  class Test {
                      void test(Object obj) {
                          
                      }
                  }
                  """
            )
        );
    }

    @Test
    void doNothingOnMethodsWithOneArg(){
        rewriteRun(
            java(
                """
                  import org.apache.commons.lang3.Validate;
                  class Test {
                      void test(Object obj) {
                            Validate.notNull(obj);
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
                          Objects.requireNonNull(obj, "Object should not be null");
                      }
                  }
                  """
            )
        );
    }

    @Test
    void replaceMethodsWithThreeArg(){
        rewriteRun(
            java(
                """
                         import org.apache.commons.lang3.Validate;
                         class Test {
                             void test(Object obj) {
                                   Validate.notNull(obj,"Object in %s should not be null","request xyz");
                             }
                         }
                      """,
                """
                  import java.util.Objects;
    
                  class Test {
                      void test(Object obj) {
                          Objects.requireNonNull(obj, String.format("Object in %s should not be null", "request xyz"));
                      }
                  }
                  """
            )
        );
    }
}
