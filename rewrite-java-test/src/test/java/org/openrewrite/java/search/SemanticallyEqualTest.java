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
package org.openrewrite.java.search;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"UnnecessaryCallToStringValueOf", "AccessStaticViaInstance", "RedundantCast", "rawtypes", "UnnecessaryLocalVariable", "ComparatorCombinators", "SameParameterValue"})
class SemanticallyEqualTest {

    private final JavaParser javaParser = JavaParser.fromJavaVersion().build();

    @Test
    void abstractMethods() {
        assertEqualToSelf(
          """
            abstract class A {
                abstract void m();
            }
            """
        );
    }

    @Test
    void classModifierLists() {
        assertEqual(
          """
            public abstract class A {
            }
            """,
          """
            abstract public class A {
            }
            """
        );
    }

    @Test
    void staticMethods() {
        assertEqual(
          """
            import static java.lang.String.valueOf;

            class T {
                Object o1 = String.valueOf("1");
                Object o2 = String.valueOf("1");
                Object o3 = String.valueOf("1");
            }
            """,
          """
            import static java.lang.String.valueOf;

            class T {
                Object o1 = String.valueOf("1");
                Object o2 = "1".valueOf("1");
                Object o3 = valueOf("1");
            }
            """
        );
    }

    @Test
    void literals() {
        assertEqual(
          """
            class A {
                int n = 1;
            }
            """,
          """
            class A {
                int n = 1;
            }
            """
        );
    }

    @Test
    void longLiterals() {
        assertEqual(
          """
            class A {
                long n = 1;
            }
            """,
          """
            class A {
                long n = 1L;
            }
            """
        );
    }

    @Test
    void classLiterals() {
        assertExpressionsEqual(
          """
            import java.util.UUID;
            class T {
                Class<?> a = java.util.UUID.class;
                Class<?> b = UUID.class;
            }
            """
        );
        assertExpressionsNotEqual(
          """
            import java.util.UUID;
            class T {
                Class<UUID> u = UUID.class;
                Class<UUID> a = UUID.class;
                Class<UUID> b = this.u;
            }
            """
        );
    }

    @CartesianTest
    void methodInvocationsWithDifferentSelect() {
        assertExpressionsNotEqual(
          """
            class T {
                static T other = new T();
                String a = toString();
                String b = other.toString();
            }
            """
        );
        assertExpressionsNotEqual(
          """
            class T {
                static T other = new T();
                String a = this.toString();
                String b = other.toString();
            }
            """
        );
    }

    @CartesianTest
    void staticFieldAccesses(@CartesianTest.Values(strings = {
      "java.util.regex.Pattern.CASE_INSENSITIVE",
      "Pattern.CASE_INSENSITIVE",
      "CASE_INSENSITIVE",
    }) String a, @CartesianTest.Values(strings = {
      "java.util.regex.Pattern.CASE_INSENSITIVE",
      "Pattern.CASE_INSENSITIVE",
      "CASE_INSENSITIVE",
    }) String b) {
        assertExpressionsEqual(
          "import java.util.regex.Pattern; import static java.util.regex.Pattern.CASE_INSENSITIVE; class T { int a = " + a + "; int b = " + b + "; }"
        );
    }

    @CartesianTest
    void staticMethodAccesses(@CartesianTest.Values(strings = {
      "java.util.regex.Pattern.compile",
      "Pattern.compile",
      "compile",
    }) String a, @CartesianTest.Values(strings = {
      "java.util.regex.Pattern.compile",
      "Pattern.compile",
      "compile",
    }) String b) {
        assertExpressionsEqual(
          "import java.util.regex.Pattern; import static java.util.regex.Pattern.compile; class T { Pattern a = " + a + "(\"\"); Pattern b = " + b + "(\"\"); }"
        );
    }

    @CartesianTest
    void methodReferences(@CartesianTest.Values(strings = {
      "java.lang.Object::toString",
      "Object::toString",
    }) String a, @CartesianTest.Values(strings = {
      "java.lang.Object::toString",
      "Object::toString",
    }) String b) {
        assertExpressionsEqual(
          "import java.util.function.Function; class T { Function<Object, String> a = " + a + "; Function<Object, String> b = " + b + "; }"
        );
    }

    @Test
    void typeCasts() {
        assertExpressionsEqual(
          """
            class T {
                Number a = (java.lang.Number) null;
                Number b = (java.lang.Number) null;
            }
            """
        );
        assertExpressionsEqual(
          """
            class T {
                Number a = (java.lang.Number) null;
                Number b = (Number) null;
            }
            """
        );
        assertExpressionsEqual(
          """
            import java.util.List;
            import java.util.UUID;
            class T {
                List a = (List<UUID>) null;
                List b = (List<java.util.UUID>) null;
            }
            """
        );
        assertExpressionsEqual(
          """
            import java.util.List;
            import java.util.UUID;
            class T {
                List a = (List<java.util.UUID>) null;
                List b = (java.util.List<UUID>) null;
            }
            """
        );
    }

    @Test
    void fieldAccesses() {
        assertExpressionsEqual(
          """
            class T {
                int n = 1;
                int a = T.this.n;
                int b = T.this.n;
            }
            """
        );
        assertExpressionsNotEqual(
          """
            class T {
                int n = 1;
                void m(int n) {
                    int a = T.this.n;
                    int b = n;
                }
            }
            """
        );
        assertExpressionsNotEqual(
          """
            class T {
                static T t1 = new T();
                int n = 1;
                int a = t1.n;
                int b = n;
            }
            """
        );
        assertExpressionsNotEqual(
          """
           import java.math.BigDecimal;
           class T {
               BigDecimal a = BigDecimal.ONE;
               BigDecimal b = BigDecimal.ZERO;
           }
           """
        );
    }

    @ExpectedToFail
    @Test
    void fieldAccessesWithQualifiedThisReference() {
        assertExpressionsEqual(
          """
            class T {
                int n = 1;
                int a = T.this.n;
                int b = this.n;
            }
            """
        );
        assertExpressionsEqual(
          """
            class T {
                int n = 1;
                int a = T.this.n;
                int b = n;
            }
            """
        );
    }

    @Test
    void identifiers() {
        assertExpressionsNotEqual(
          """
            class T {
                static T t1 = new T();
                int n = 1;
                int a = n;
                int b = t1.n;
            }
            """
        );
    }

    @Test
    void lambdaParameterNames() {
        assertExpressionsEqual(
          """
            import java.util.Comparator;
            class T {
                Comparator<Integer> a = (x1, y1) -> x1 - y1;
                Comparator<Integer> b = (x2, y2) -> x2 - y2;
            }
            """
        );
        assertExpressionsNotEqual(
          """
            import java.util.Comparator;
            class T {
                Comparator<Integer> a = (x1, y1) -> x1 - y1;
                Comparator<Integer> b = (x2, y2) -> y2 - x2;
            }
            """
        );
    }

    @SuppressWarnings({"DataFlowIssue", "OptionalGetWithoutIsPresent", "Convert2Diamond"})
    @Nested
    class Generics {
        @Test
        void noneEmpty() {
            assertExpressionsEqual(
              """
                import java.util.List;
                class T {
                    List<String> a = new java.util.ArrayList<String>();
                    List<String> b = new java.util.ArrayList<String>();
                }
                """
            );
        }

        @Test
        void firstEmpty() {
            assertExpressionsEqual(
              """
                import java.util.List;
                class T {
                    List<String> a = new java.util.ArrayList<String>();
                    List<String> b = new java.util.ArrayList<>();
                }
                """
            );
        }

        @Test
        void secondEmpty() {
            assertExpressionsEqual(
              """
                import java.util.List;
                class T {
                    List<String> a = new java.util.ArrayList<>();
                    List<String> b = new java.util.ArrayList<String>();
                }
                """
            );
        }

        @Test
        void bothEmpty() {
            assertExpressionsEqual(
              """
                import java.util.List;
                class T {
                    List<String> a = new java.util.ArrayList<>();
                    List<String> b = new java.util.ArrayList<>();
                }
                """
            );
        }

        @Test
        void bothEmptyButDifferent() {
            assertExpressionsNotEqual(
              """
                import java.util.List;
                class T {
                    List<String> a = new java.util.ArrayList<>();
                    List<Integer> b = new java.util.ArrayList<>();
                }
                """
            );
        }

        @Test
        void fieldNotEqualToShadowedNameVariable() {
            J.CompilationUnit cu = javaParser.parse(
                """
                  class T {
                      int a = 1;
                      void m(int a) {
                          this.a = a;
                      }
                  }
                  """
              ).findFirst()
              .map(J.CompilationUnit.class::cast)
              .get();
            J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().get(1);
            J.Assignment a = (J.Assignment) m.getBody().getStatements().getFirst();
            assertFalse(SemanticallyEqual.areEqual(a.getVariable(), a.getAssignment()));
            assertFalse(SemanticallyEqual.areEqual(((J.FieldAccess) a.getVariable()).getName(), a.getAssignment()));
        }

        @Test
        void fieldsFromDifferentInstances() {
            J.CompilationUnit cu = javaParser.parse(
                """
                  class T {
                      int n;
                      boolean m(T a, T b) {
                          return a.n == b.n;
                      }
                  }
                  """
              ).findFirst()
              .map(J.CompilationUnit.class::cast)
              .get();
            J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().get(1);
            J.Binary b = (J.Binary) ((J.Return) m.getBody().getStatements().getFirst()).getExpression();
            assertFalse(SemanticallyEqual.areEqual(b.getLeft(), b.getRight()));
        }
    }

    private void assertEqualToSelf(@Language("java") String a) {
        assertEqual(a, a);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void assertEqual(@Language("java") String a, @Language("java") String b) {
        J.CompilationUnit cua = (J.CompilationUnit) javaParser.parse(a).findFirst().get();
        javaParser.reset();
        J.CompilationUnit cub = (J.CompilationUnit) javaParser.parse(b).findFirst().get();
        javaParser.reset();
        assertEqual(cua, cub);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void assertExpressionsEqual(@Language("java") String source) {
        J.CompilationUnit cu = (J.CompilationUnit) javaParser.parse(source).findFirst().get();
        javaParser.reset();

        JavaIsoVisitor<Map<String, J.VariableDeclarations.NamedVariable>> visitor = new JavaIsoVisitor<>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Map<String, J.VariableDeclarations.NamedVariable> map) {
                map.put(variable.getSimpleName(), variable);
                return super.visitVariable(variable, map);
            }
        };

        Map<String, J.VariableDeclarations.NamedVariable> result = visitor.reduce(cu, new HashMap<>());
        assertThat(SemanticallyEqual.areEqual(
          Objects.requireNonNull(result.get("a").getInitializer()),
          Objects.requireNonNull(result.get("b").getInitializer()))
        ).isTrue();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void assertExpressionsNotEqual(@Language("java") String source) {
        J.CompilationUnit cu = (J.CompilationUnit) javaParser.parse(source).findFirst().get();
        javaParser.reset();

        JavaIsoVisitor<Map<String, J.VariableDeclarations.NamedVariable>> visitor = new JavaIsoVisitor<>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Map<String, J.VariableDeclarations.NamedVariable> map) {
                map.put(variable.getSimpleName(), variable);
                return super.visitVariable(variable, map);
            }
        };

        Map<String, J.VariableDeclarations.NamedVariable> result = visitor.reduce(cu, new HashMap<>());
        assertThat(SemanticallyEqual.areEqual(
          Objects.requireNonNull(result.get("a").getInitializer()),
          Objects.requireNonNull(result.get("b").getInitializer()))
        ).isFalse();
    }

    private void assertEqual(J a, J b) {
        assertTrue(SemanticallyEqual.areEqual(a, b));
    }
}
