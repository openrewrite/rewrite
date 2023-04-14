package org.openrewrite.java.nullability;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseOpenRewriteNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("rewrite-core", "jsr305"))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java").build().activateRecipes("org.openrewrite.java.nullability.UseOpenRewriteNullabilityAnnotations"));
    }

    @Test
    void replaceJavaxWithOpenRewriteAnnotationsNonNull() {
        rewriteRun(java("""
            package org.openrewrite.java;
                              
            import javax.annotation.Nonnull;

            class Test {
                @Nonnull
                String variable = "";
            }

            """,
          """
            package org.openrewrite.java;

            import org.openrewrite.internal.lang.NonNull;

            class Test {
                @NonNull
                String variable = "";
            }
            """));
    }

    @Test
    void replaceJavaxWithOpenRewriteAnnotationsNullable() {
        rewriteRun(java("""
            package org.openrewrite.java;

            import javax.annotation.Nullable;

            class Test {
                @Nullable
                String variable;
            }
            """,

          """
            package org.openrewrite.java;
                              
            import org.openrewrite.internal.lang.Nullable;

            class Test {
                @Nullable
                String variable;
            }
            """));
    }

    @Test
    void replaceJavaxWithOpenRewriteAnnotationsNonNullPackage() {
        rewriteRun(java("""
          @Nonnull
          package org.openrewrite.java;

          import javax.annotation.Nonnull;
          """, """
          @NonNullApi
          @NonNullFields
          package org.openrewrite.java;

          import org.openrewrite.internal.lang.NonNullApi;
          import org.openrewrite.internal.lang.NonNullFields;
          """));
    }
}
