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
        spec.parser(JavaParser.fromJavaVersion().classpath("rewrite-core", "jsr305")).recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java").build().activateRecipes("org.openrewrite.java.nullability.UseOpenRewriteNullabilityAnnotations"));
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
          """, """
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
    void replaceJavaxWithOpenRewriteAnnotationNonNullClass() {
        rewriteRun(java("""
          package org.openrewrite.java;

          import javax.annotation.Nonnull;

          @Nonnull
          class Test {
          }
          """, """
          package org.openrewrite.java;

          import org.openrewrite.internal.lang.NonNull;

          @NonNull
          class Test {
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
