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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class StandardizeNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new StandardizeNullabilityAnnotations(Nullable.class.getName(), NonNull.class.getName()));
    }

    @Test
    void removesImportIfNecessaryNullable() {
        rewriteRun(java("""
            package org.openrewrite.internal.lang;
                          
            import javax.annotation.Nullable;
                          
            class Test {
              @javax.annotation.Nullable
              String variable;
            }
            """,

          """
            package org.openrewrite.internal.lang;
              
            class Test {
              @Nullable
              String variable;
            }
            """));
    }

    @Test
    void removesImportIfNecessaryNonNull() {
        rewriteRun(java("""
            package org.openrewrite.internal.lang;
                          
            import javax.annotation.Nonnull;
                          
            class Test {
              @Nonnull
              String variable = "";
            }
            """,

          """
            package org.openrewrite.internal.lang;
              
            class Test {
              @NonNull
              String variable = "";
            }
            """));
    }

    @Test
    void addsImportIfNecessaryNullable() {
        rewriteRun(java("""
            package javax.annotation;
                          
            class Test {
              @Nullable
              String variable = "";
            }
            """,

          """
            package javax.annotation;
              
            import org.openrewrite.internal.lang.Nullable;
              
            class Test {
              @Nullable
              String variable;
            }
            """));
    }

    @Test
    void addsImportIfNecessaryNonNull() {
        rewriteRun(java("""
            package javax.annotation;
                          
            class Test {
              @Nonnull
              String variable = "";
            }
            """,

          """
            package javax.annotation;
              
            import org.openrewrite.internal.lang.NonNull;
              
            class Test {
              @NonNull
              String variable = "";
            }
            """));
    }

    @Test
    void doesNotAddImportIfUnnecessaryNullable() {
        rewriteRun(java("""
            package org.openrewrite.internal.lang;
                          
            import javax.annotation.Nullable;
                          
            class Test {
              @javax.annotation.Nullable
              String variable;
            }
            """,

          """
            package org.openrewrite.internal.lang;
              
            class Test {
              @Nullable
              String variable;
            }
            """));
    }

    @Test
    void doesNotAddImportIfUnnecessaryNonNull() {
        rewriteRun(java("""
            package org.openrewrite.internal.lang;
                          
            import javax.annotation.Nonnull;
                          
            class Test {
              @Nonnull
              String variable = "";
            }
            """,

          """
            package org.openrewrite.internal.lang;
              
            class Test {
              @NonNull
              String variable = "";
            }
            """));
    }

    @Test
    void unchangedWhenNoNullabilityAnnotationWasUsed() {
        rewriteRun(java("""
          class Test {
            String variable = "";
          }
          """));
    }

    @Test
    void unchangedWhenOnlyTheConfiguredNullabilityAnnotationsWereUsed() {
        rewriteRun(java("""            
          import org.openrewrite.internal.lang.NonNull;
          import org.openrewrite.internal.lang.Nullable;
                          
          class Test {
            @NonNull
            String nonNullVariable = "";
            
            @Nullable
            String nullableVariable;
          }
          """));
    }

    @Test
    void replacesAnnotationIfNonConfiguredAnnotationWasUsedNullable() {
        rewriteRun(java("""
            package org.openrewrite.java.nullability;
                          
            import javax.annotation.Nullable;
                          
            class Test {
              @Nullable
              String variable;
            }
            """,

          """
            package org.openrewrite.java.nullability;
              
            import org.openrewrite.internal.lang.Nullable;
              
            class Test {
              @Nullable
              String variable;
            }
            """));
    }

    @Test
    void replacesAnnotationIfNonConfiguredAnnotationWasUsedNonNull() {
        rewriteRun(java("""
            package org.openrewrite.java.nullability;
                          
            import javax.annotation.Nonnull;
                          
            class Test {
              @Nonnull
              String variable = "";
            }
            """,

          """
            package org.openrewrite.java.nullability;
              
            import org.openrewrite.internal.lang.NonNull;
              
            class Test {
              @NonNull
              String variable = "";
            }
            """));
    }

    @Test
    void replacesAllAnnotationsIfDifferentNonConfiguredAnnotationWereUsed() {
        rewriteRun(java("""        
            import javax.annotation.Nonnull;
            import jakarta.annotation.Nullable;    
          import org.openrewrite.internal.lang.NonNull;
          import org.openrewrite.internal.lang.Nullable;
                          
          class Test {
            @NonNull
            String nonNullVariable = "";
            
            @Nullable
            String nullableVariable;
          }
          """, """
          import org.openrewrite.internal.lang.NonNull;
          import org.openrewrite.internal.lang.Nullable;
                          
          class Test {
            @NonNull
            String nonNullVariable = "";
            
            @Nullable
            String nullableVariable;
          }
          """));
    }
}
