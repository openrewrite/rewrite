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
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class FullyQualifyMemberReferenceTest implements RewriteTest {

    @DocumentExample
    @Test
    void fullyQualifyStaticMethod() {
        // Create a method type for java.util.Collections.emptyList()
        JavaType.Method emptyListMethod = createMethodType(
            "java.util.Collections", "emptyList", "java.util.List"
        );

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyMemberReference<>(emptyListMethod))),
          java(
            """
            import static java.util.Collections.emptyList;
            import java.util.List;

            class Test {
                List<String> list = emptyList();

                void method() {
                    // emptyList should be fully qualified
                    List<Integer> empty = emptyList();
                }
            }
            """,
            """
            import static java.util.Collections.emptyList;
            import java.util.List;

            class Test {
                List<String> list = java.util.Collections.emptyList();

                void method() {
                    // emptyList should be fully qualified
                    List<Integer> empty = java.util.Collections.emptyList();
                }
            }
            """
          )
        );
    }

    @Test
    void fullyQualifyStaticVariable() {
        // Create a variable type for java.lang.System.out
        JavaType.Variable outVariable = createVariableType(
            "java.lang.System", "out", "java.io.PrintStream"
        );

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyMemberReference<>(outVariable))),
          java(
            """
            import static java.lang.System.out;

            class Test {
                void method() {
                    // out should be fully qualified
                    out.println("Hello, world!");
                }
            }
            """,
            """
            import static java.lang.System.out;

            class Test {
                void method() {
                    // out should be fully qualified
                    java.lang.System.out.println("Hello, world!");
                }
            }
            """
          )
        );
    }

    @Test
    void doNotQualifyNonStaticMethods() {
        // Create a method type for java.util.Collections.emptyList()
        JavaType.Method emptyListMethod = createMethodType(
            "java.util.Collections", "emptyList", "java.util.List"
        );

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyMemberReference<>(emptyListMethod))),
          java(
            """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
                void method() {
                    // add is not a static method, should not be qualified
                    List<String> list = new ArrayList<>();
                    list.add("test");
                }
            }
            """
          )
        );
    }

    @Test
    void doNotQualifyNonStaticVariables() {
        // Create a variable type for java.lang.System.out
        JavaType.Variable outVariable = createVariableType(
            "java.lang.System", "out", "java.io.PrintStream"
        );

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyMemberReference<>(outVariable))),
          java(
            """
            class Test {
                private String name;

                void method() {
                    // name is not a static variable, should not be qualified
                    this.name = "test";
                }
            }
            """
          )
        );
    }

    @Test
    void fullyQualifyMultipleStaticMethods() {
        // Create a method type for java.lang.Math.max(int, int)
        JavaType.Method maxMethod = createMethodType(
            "java.lang.Math", "max", "int", "int", "int"
        );

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new FullyQualifyMemberReference<>(maxMethod))),
          java(
            """
            import static java.lang.Math.max;
            import static java.lang.Math.min;

            class Test {
                void method() {
                    // max should be fully qualified, min should remain as is
                    int maximum = max(5, 10);
                    int minimum = min(5, 10);
                }
            }
            """,
            """
            import static java.lang.Math.max;
            import static java.lang.Math.min;

            class Test {
                void method() {
                    // max should be fully qualified, min should remain as is
                    int maximum = java.lang.Math.max(5, 10);
                    int minimum = min(5, 10);
                }
            }
            """
          )
        );
    }

    /**
     * Helper method to create a JavaType.Method instance for testing
     */
    private JavaType.Method createMethodType(String declaringType, String methodName, String returnType, String... parameterTypes) {
        JavaType.FullyQualified declaringClass = JavaType.ShallowClass.build(declaringType);

        JavaType returnTypeObj = JavaType.buildType(returnType);

        JavaType[] paramTypes = new JavaType[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            paramTypes[i] = JavaType.buildType(parameterTypes[i]);
        }

        return new JavaType.Method(
            null,
            Flag.Public.getBitMask() | Flag.Static.getBitMask(),
            declaringClass,
            methodName,
            returnTypeObj,
            emptyList(),
            List.of(paramTypes),
            emptyList(),
            emptyList(),
            null,
            emptyList()
        );
    }

    /**
     * Helper method to create a JavaType.Variable instance for testing
     */
    private JavaType.Variable createVariableType(String ownerType, String variableName, String variableType) {
        JavaType.FullyQualified ownerClass = JavaType.ShallowClass.build(ownerType);

        JavaType type = JavaType.buildType(variableType);

        return new JavaType.Variable(
            null,
            Flag.Public.getBitMask() | Flag.Static.getBitMask(),
            variableName,
            ownerClass,
            type,
            emptyList()
        );
    }
}
