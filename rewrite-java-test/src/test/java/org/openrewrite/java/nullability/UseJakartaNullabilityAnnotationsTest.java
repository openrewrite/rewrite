package org.openrewrite.java.nullability;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseJakartaNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("jsr305", "jakarta.annotation-api"))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java").build().activateRecipes("org.openrewrite.java.nullability.UseJakartaNullabilityAnnotations"));
    }

    @Test
    void replaceJavaxWithJakartaAnnotationsNonNull() {
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

            import jakarta.annotation.NonNull;

            class Test {
                @NonNull
                String variable = "";
            }
            """));
    }

    @Test
    void replaceJavaxWithJakartaAnnotationsNullable() {
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
                              
            import jakarta.annotation.Nullable;

            class Test {
                @Nullable
                String variable;
            }
            """));
    }
}
