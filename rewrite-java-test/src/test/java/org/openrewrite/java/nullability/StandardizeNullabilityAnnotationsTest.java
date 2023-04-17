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
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.NonNullFields;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;

class StandardizeNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("rewrite-core", "jsr305", "spring-core"));
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
                package org.openrewrite.java;
                                
                class Test {
                    String variable = "";
                }
                """));
    }

    @Test
    void unchangedWhenOnlyTheConfiguredNullabilityAnnotationsWereUsed() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""            
                package org.openrewrite.java;

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
                package org.openrewrite.java;

                import javax.annotation.Nonnull;
                import org.springframework.lang.Nullable;

                class Test {
                    @Nonnull
                    String nonNullVariable = "";

                    @Nullable
                    String nullableVariable;
                }
                """, """
                package org.openrewrite.java;

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
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(NonNullApi.class.getName()))), java("""
                @NonNullApi
                package org.openrewrite.java;

                import org.springframework.lang.NonNullApi;
                """, """
                @NonNullApi
                package org.openrewrite.java;

                import org.openrewrite.internal.lang.NonNullApi;
                """));
    }

    @Test
    void shouldReplaceAnnotationsOnClass() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""        
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
    void shouldReplaceAnnotationsOnMethod() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""        
                package org.openrewrite.java;

                import javax.annotation.Nonnull;

                class Test {
                    @Nonnull
                    public String getString() {
                        return "";
                    }
                }
                """, """
                package org.openrewrite.java;
                                
                import org.openrewrite.internal.lang.NonNull;

                class Test {
                    @NonNull
                    public String getString() {
                        return "";
                    }
                }
                """));
    }

    @Test
    void shouldReplaceAnnotationsOnReturnType() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""        
                package org.openrewrite.java;
                                
                import javax.annotation.Nonnull;

                class Test {
                    public @Nonnull String getString() {
                        return "";
                    }
                }
                """, """
                package org.openrewrite.java;

                import org.openrewrite.internal.lang.NonNull;

                class Test {
                    public @NonNull String getString() {
                        return "";
                    }
                }
                """));
    }

    @Test
    void shouldReplaceAnnotationsOnParameter() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""        
                package org.openrewrite.java;

                import javax.annotation.Nonnull;

                class Test {
                    public String getString(@Nonnull String parameter) {
                        return parameter;
                    }
                }
                """, """
                package org.openrewrite.java;

                import org.openrewrite.internal.lang.NonNull;

                class Test {
                    public String getString(@NonNull String parameter) {
                        return parameter;
                    }
                }
                """));
    }

    @Test
    void shouldReplaceAnnotationsOnField() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""        
                package org.openrewrite.java;

                import javax.annotation.Nonnull;

                class Test {
                    @Nonnull
                    String nonNullVariable = "";
                }
                """, """
                package org.openrewrite.java;

                import org.openrewrite.internal.lang.NonNull;

                class Test {
                    @NonNull
                    String nonNullVariable = "";
                }
                """));
    }

    @Test
    void shouldReplaceAnnotationsOnLocalField() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""        
                package org.openrewrite.java;

                import javax.annotation.Nonnull;

                class Test {
                    public String getString() {
                        @Nonnull
                        String parameter = "";
                        return parameter;
                    }
                }
                """, """
                package org.openrewrite.java;

                import org.openrewrite.internal.lang.NonNull;

                class Test {
                    public String getString() {
                        @NonNull
                        String parameter = "";
                        return parameter;
                    }
                }
                """));
    }

    @Test
    void shouldReplaceTwoAnnotationsWithOne() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of("javax.annotation.Nonnull"))), java("""        
                package org.openrewrite.java;

                import org.openrewrite.internal.lang.NonNullApi;
                import org.openrewrite.internal.lang.NonNullFields;

                @NonNullApi
                @NonNullFields
                class Test {
                }
                """, """
                package org.openrewrite.java;

                import javax.annotation.Nonnull;

                @Nonnull
                class Test {
                }
                """));
    }

    @Test
    void shouldReplaceOneAnnotationsWithTwo() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(NonNullApi.class.getName(), NonNullFields.class.getName()))), java("""        
                package org.openrewrite.java;

                import javax.annotation.Nonnull;

                @Nonnull
                class Test {
                }
                """, """
                package org.openrewrite.java;

                import org.openrewrite.internal.lang.NonNullApi;
                import org.openrewrite.internal.lang.NonNullFields;

                @NonNullApi
                @NonNullFields
                class Test {
                }
                """));
    }

    @Test
    void shouldReplaceUsingFqnWhenCollidingImportExistsToNotBreakCode() {
        rewriteRun(spec -> spec.recipe(new StandardizeNullabilityAnnotations(List.of(Nullable.class.getName(), NonNull.class.getName()))), java("""        
                package org.openrewrite.java;

                import javax.annotation.Nonnull;
                import org.springframework.lang.NonNull;

                @Nonnull
                class Test {
                }
                """, """
                package org.openrewrite.java;
                
                import org.springframework.lang.NonNull;

                @org.openrewrite.internal.lang.NonNull
                class Test {
                }
                """));
    }
}
