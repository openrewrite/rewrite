package org.openrewrite.java.nullability;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.NonNullFields;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;

class UseSpringNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("jsr305", "spring-core"))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java").build().activateRecipes("org.openrewrite.java.nullability.UseSpringNullabilityAnnotations"));
    }

    @Test
    void replaceJavaxWithSpringAnnotationsNonNull() {
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

            import org.springframework.lang.NonNull;

            class Test {
                @NonNull
                String variable = "";
            }
            """));
    }

    @Test
    void replaceJavaxWithSpringAnnotationsNullable() {
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
    void replaceJavaxWithSpringAnnotationsNonNullClass() {
        rewriteRun(java("""
          package org.openrewrite.java;

          import javax.annotation.Nonnull;

          @Nonnull
          class Test {
          }
          """, """
          package org.openrewrite.java;

          import org.springframework.lang.NonNullApi;
          import org.springframework.lang.NonNullFields;
          
          @NonNullApi
          @NonNullFields
          class Test {
          }
          """
        ));
    }

    @Test
    void replaceJavaxWithSpringAnnotationsNonNullPackage() {
        rewriteRun(java("""
          @Nonnull
          package org.openrewrite.java;

          import javax.annotation.Nonnull;
          """, """
          @NonNullApi
          @NonNullFields
          package org.openrewrite.java;

          import org.springframework.lang.NonNullApi;
          import org.springframework.lang.NonNullFields;
          """));
    }
}
