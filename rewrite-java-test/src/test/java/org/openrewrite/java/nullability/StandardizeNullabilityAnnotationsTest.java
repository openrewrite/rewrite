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
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.openrewrite.java.Assertions.java;

class StandardizeNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("javax.annotation-api", "rewrite-core"));
    }

    @Test
    void removesImportIfPossible() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""
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
    void addsImportIfNecessary() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of("javax.annotation.Nullable", "javax.annotation.Nonnull"))), java("""
            package org.openrewrite.internal.lang;
                          
            class Test {
              @NonNull
              String variable = "";
            }
            """,

          """
            package org.openrewrite.internal.lang;
              
            import javax.annotation.Nonnull;
              
            class Test {
              @Nonnull
              String variable = "";
            }
            """));
    }

    @Test
    void doesNotAddImportIfUnnecessary() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""
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
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""
          class Test {
            String variable = "";
          }
          """));
    }

    @Test
    void unchangedWhenOnlyTheConfiguredNullabilityAnnotationsWereUsed() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""            
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
    void replacesAllAnnotationsIfDifferentNonConfiguredAnnotationWereUsed() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""        
          import javax.annotation.Nonnull;
          import jakarta.annotation.Nullable;
                          
          class Test {
            @Nonnull
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

    @Test
    void shouldReplaceAnnotationsOnPackage() {
        fail("not yet implemented");
    }

    @Test
    void shouldReplaceAnnotationsOnClass() {
        fail("not yet implemented");
    }

    @Test
    void shouldReplaceAnnotationsOnMethod() {
        fail("not yet implemented");
    }

    @Test
    void shouldReplaceAnnotationsOnReturnType() {
        fail("not yet implemented");
    }

    @Test
    void shouldReplaceAnnotationsOnParameter() {
        fail("not yet implemented");
    }

    @Test
    void shouldReplaceAnnotationsOnField() {
        fail("not yet implemented");
    }

    @Test
    void shouldReplaceAnnotationsOnLocalField() {
        fail("not yet implemented");
    }

    @Test
    void shouldReplaceTwoAnnotationsWithOne() {
        // Maybe @NonNullApi and @NonNullFields
        fail("not yet implemented");
    }

    @Test
    void shouldReplaceOneAnnotationsWithTwo() {
        // Maybe @NonNullApi and @NonNullFields
        fail("not yet implemented");
    }
}
