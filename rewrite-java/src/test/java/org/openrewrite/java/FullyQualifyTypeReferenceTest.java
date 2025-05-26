/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class FullyQualifyTypeReferenceTest implements RewriteTest {

    @DocumentExample
    @Test
    void fullyQualify() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyTypeReference<>(
              TypeUtils.asFullyQualified(JavaType.buildType("java.util.List"))
          ))),
          java(
            """
            import java.util.*;

            class Test {
                List<String> list = new ArrayList<>();

                void method(List<Integer> param) {
                    // List should be fully qualified
                }
            }
            """,
            """
            import java.util.*;

            class Test {
                java.util.List<String> list = new ArrayList<>();

                void method(java.util.List<Integer> param) {
                    // List should be fully qualified
                }
            }
            """
          )
        );
    }

    @Test
    void fullyQualifyFieldAccess() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyTypeReference<>(
              TypeUtils.asFullyQualified(JavaType.buildType("java.util.Collections"))
          ))),
          java(
            """
            import java.util.Collections;
            import java.util.List;

            class Test {
                List<String> list = Collections.emptyList();

                void method(Collections foo) {
                    // Collections should be fully qualified
                    List<Integer> empty = Collections.emptyList();
                }
            }
            """,
            """
            import java.util.Collections;
            import java.util.List;

            class Test {
                List<String> list = java.util.Collections.emptyList();

                void method(java.util.Collections foo) {
                    // Collections should be fully qualified
                    List<Integer> empty = java.util.Collections.emptyList();
                }
            }
            """
          )
        );
    }

    @Test
    void fullyQualifyTypeCast() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyTypeReference<>(
              TypeUtils.asFullyQualified(JavaType.buildType("java.util.List"))
          ))),
          java(
            """
            import java.util.*;

            class Test {
                Object getObject() {
                    return new ArrayList<String>();
                }

                void method() {
                    // List should be fully qualified in the cast
                    List<String> list = (List<String>) getObject();
                }
            }
            """,
            """
            import java.util.*;

            class Test {
                Object getObject() {
                    return new ArrayList<String>();
                }

                void method() {
                    // List should be fully qualified in the cast
                    java.util.List<String> list = (java.util.List<String>) getObject();
                }
            }
            """
          )
        );
    }

    @Test
    void fullyQualifyMethodReturnType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyTypeReference<>(
              TypeUtils.asFullyQualified(JavaType.buildType("java.util.List"))
          ))),
          java(
            """
            import java.util.*;

            class Test {
                // List should be fully qualified in the return type
                List<String> getList() {
                    return new ArrayList<>();
                }
            }
            """,
            """
            import java.util.*;

            class Test {
                // List should be fully qualified in the return type
                java.util.List<String> getList() {
                    return new ArrayList<>();
                }
            }
            """
          )
        );
    }

    @Test
    void fullyQualifyExceptionType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyTypeReference<>(
              TypeUtils.asFullyQualified(JavaType.buildType("java.io.IOException"))
          ))),
          java(
            """
            import java.io.IOException;

            class Test {
                void method() {
                    try {
                        throw new IOException("Error");
                    } catch (IOException e) {
                        // IOException should be fully qualified
                        e.printStackTrace();
                    }
                }
            }
            """,
            """
            import java.io.IOException;

            class Test {
                void method() {
                    try {
                        throw new java.io.IOException("Error");
                    } catch (java.io.IOException e) {
                        // IOException should be fully qualified
                        e.printStackTrace();
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void fullyQualifyGenericTypeParameter() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyTypeReference<>(
              TypeUtils.asFullyQualified(JavaType.buildType("java.util.Map"))
          ))),
          java(
            """
            import java.util.*;

            class Test {
                // Map should be fully qualified in the generic type parameter
                void processEntries(List<Map<String, Integer>> maps) {
                    for (Map<String, Integer> map : maps) {
                        // process map
                    }
                }

                // Map should be fully qualified in the type parameter of the class
                class Container<T extends Map<String, Integer>> {
                    T value;
                }
            }
            """,
            """
            import java.util.*;

            class Test {
                // Map should be fully qualified in the generic type parameter
                void processEntries(List<java.util.Map<String, Integer>> maps) {
                    for (java.util.Map<String, Integer> map : maps) {
                        // process map
                    }
                }

                // Map should be fully qualified in the type parameter of the class
                class Container<T extends java.util.Map<String, Integer>> {
                    T value;
                }
            }
            """
          )
        );
    }

    @Test
    void fullyQualifyArrayType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyTypeReference<>(
              TypeUtils.asFullyQualified(JavaType.buildType("java.util.Date"))
          ))),
          java(
            """
            import java.util.Date;

            class Test {
                // Date should be fully qualified in the array type
                Date[] getDates() {
                    return new Date[0];
                }

                void processDateArray(Date[] dates) {
                    // process dates
                }

                void createMultiDimensionalArray() {
                    // Date should be fully qualified in multi-dimensional array
                    Date[][] dateMatrix = new Date[3][3];
                }
            }
            """,
            """
            import java.util.Date;

            class Test {
                // Date should be fully qualified in the array type
                java.util.Date[] getDates() {
                    return new java.util.Date[0];
                }

                void processDateArray(java.util.Date[] dates) {
                    // process dates
                }

                void createMultiDimensionalArray() {
                    // Date should be fully qualified in multi-dimensional array
                    java.util.Date[][] dateMatrix = new java.util.Date[3][3];
                }
            }
            """
          )
        );
    }

    @Test
    void fullyQualifyInstanceOf() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyTypeReference<>(
              TypeUtils.asFullyQualified(JavaType.buildType("java.util.List"))
          ))),
          java(
            """
            import java.util.*;

            class Test {
                void method(Object obj) {
                    // List should be fully qualified in instanceof check
                    if (obj instanceof List) {
                        List<String> list = (List<String>) obj;
                        // process list
                    }
                }
            }
            """,
            """
            import java.util.*;

            class Test {
                void method(Object obj) {
                    // List should be fully qualified in instanceof check
                    if (obj instanceof java.util.List) {
                        java.util.List<String> list = (java.util.List<String>) obj;
                        // process list
                    }
                }
            }
            """
          )
        );
    }
}
