package org.openrewrite.java.nullability;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseJavaxNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("rewrite-core", "jsr305"))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java").build().activateRecipes("org.openrewrite.java.nullability.UseJavaxNullabilityAnnotations"));
    }

    @Test
    void replaceOpenRewriteWithJavaxAnnotationsNonNull() {
        rewriteRun(java("""
                        package org.openrewrite.java;

                        import org.openrewrite.internal.lang.NonNull;

                        class Test {
                            @NonNull
                            String variable = "";
                        }
                        """,

          """
                  package org.openrewrite.java;
                  
                  import javax.annotation.Nonnull;

                  class Test {
                      @Nonnull
                      String variable = "";
                  }
                  """));
    }

    @Test
    void replaceOpenRewriteWithJavaxAnnotationsNullable() {
        rewriteRun(java("""
                        package org.openrewrite.java;

                        import org.openrewrite.internal.lang.Nullable;

                        class Test {
                            @Nullable
                            String variable;
                        }
                        """,

          """
                  package org.openrewrite.java;
                  
                  import javax.annotation.Nullable;

                  class Test {
                      @Nullable
                      String variable;
                  }
                  """));
    }
}
