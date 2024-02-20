/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ConstantConditions")
class JavaTemplateSubstitutionsTest implements RewriteTest {

    @DocumentExample
    @SuppressWarnings("InfiniteRecursion")
    @Test
    void any() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                  if (method.getSimpleName().equals("test")) {
                      var s = method.getBody().getStatements().get(0);
                      return JavaTemplate.builder("test(#{any()})")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), s.getCoordinates().replace(), s);
                  }
                  return method;
              }
          }).withMaxCycles(1)),
          java(
            """
              class Test {
                  void test(int n) {
                      value();
                  }

                  int value() {
                      return 0;
                  }
              }
              """,
            """
              class Test {
                  void test(int n) {
                      test(value());
                  }

                  int value() {
                      return 0;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("InfiniteRecursion")
    @Test
    void array() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                  if (method.getSimpleName().equals("test")) {
                      var s = method.getBody().getStatements().get(0);
                      return JavaTemplate.builder("test(#{anyArray()})")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), s.getCoordinates().replace(), s);
                  }
                  return method;
              }
          }).withMaxCycles(1)),
          java(
            """
              class Test {
                  void test(int[][] n) {
                      array();
                  }

                  int[][] array() {
                      return new int[0][0];
                  }
              }
              """,
            """
              class Test {
                  void test(int[][] n) {
                      test(array());
                  }

                  int[][] array() {
                      return new int[0][0];
                  }
              }
              """
          )
        );
    }

    @Test
    void annotation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                  if (method.getSimpleName().equals("test")) {
                      return JavaTemplate.builder("#{} void test2() {}")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getLeadingAnnotations().get(0));
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  @SuppressWarnings("ALL") void test() {
                  }
              }
              """,
            """
              class Test {
                            
                  @SuppressWarnings("ALL")
                  void test2() {
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "InfiniteRecursion"})
    @Test
    void methodInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                  var s = method.getBody().getStatements().get(0);
                  return JavaTemplate.builder("test(#{any(java.util.Collection)}, #{any(int)})")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), s.getCoordinates().replace(), s,
                      ((J.VariableDeclarations) method.getParameters().get(1)).getVariables().get(0).getName());
              }
          }).withMaxCycles(1)),
          java(
            """
              import java.util.*;
              class Test {
                  void test(Collection<?> c, Integer n) {
                      Collections.emptyList();
                  }
              }
              """,
            """
              import java.util.*;
              class Test {
                  void test(Collection<?> c, Integer n) {
                      test(Collections.emptyList(), n);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void block() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                  var s = method.getBody().getStatements().get(0);
                  return JavaTemplate.builder("if(true) #{}")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), s.getCoordinates().replace(), method.getBody());
              }
          }).withMaxCycles(1)),
          java(
            """
              class Test {
                  void test() {
                      int n;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      if (true) {
                          int n;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void arrayAccess() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitArrayAccess(J.ArrayAccess arrayAccess, ExecutionContext executionContext) {
                  return JavaTemplate.builder("Some.method()")
                    .contextSensitive()
                    .javaParser(JavaParser.fromJavaVersion()
                      .dependsOn(
                        """
                          public class Some {
                              public static int method() {
                                  return 0;
                              }
                          }
                          """
                      )
                    )
                    .build()
                    .apply(getCursor(), arrayAccess.getCoordinates().replace());
              }
          })),
          java(
            """
              public class Test {
                  int[] arrayAccess = new int[1];
                  int i = arrayAccess[0];
              }
              """,
            """
              public class Test {
                  int[] arrayAccess = new int[1];
                  int i = Some.method();
              }
              """
          )
        );
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void binary() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext executionContext) {
                  if (binary.getOperator() == J.Binary.Type.Equal) {
                      return JavaTemplate.builder("Some.method()")
                        .contextSensitive()
                        .javaParser(JavaParser.fromJavaVersion()
                          .dependsOn(
                            """
                              public class Some {
                                  public static boolean method() {
                                      return true;
                                  }
                              }
                              """
                          )
                        )
                        .build()
                        .apply(getCursor(), binary.getCoordinates().replace());
                  }
                  return super.visitBinary(binary, executionContext);
              }
          })),
          java(
            """
              public class Test {
                  boolean binary = 1 == 1;
              }
              """,
            """
              public class Test {
                  boolean binary = Some.method();
              }
              """
          )
        );
    }

    @Test
    void literal() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLiteral(J.Literal literal, ExecutionContext executionContext) {
                  if (literal.getValue().equals("literal")) {
                      return JavaTemplate.builder("Some.method()")
                        .contextSensitive()
                        .javaParser(JavaParser.fromJavaVersion()
                          .dependsOn(
                            """
                              public class Some {
                                  public static String method() {
                                      return "";
                                  }
                              }
                              """
                          )
                        )
                        .build()
                        .apply(getCursor(), literal.getCoordinates().replace());
                  }
                  return super.visitLiteral(literal, executionContext);
              }
          })),
          java(
            """
              public class Test {
                  String literal = "literal";
              }
              """,
            """
              public class Test {
                  String literal = Some.method();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1985")
    @Test
    void newArray() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitNewArray(J.NewArray newArray, ExecutionContext executionContext) {
                  if (((J.Literal) newArray.getDimensions().get(0).getIndex()).getValue().equals(1)) {
                      return JavaTemplate.builder("Some.method()")
                        .contextSensitive()
                        .javaParser(JavaParser.fromJavaVersion()
                          .logCompilationWarningsAndErrors(true)
                          .dependsOn("""
                                public class Some {
                                    public static int[] method() {
                                        return new int[0];
                                    }
                                }
                            """)
                        )
                        .build()
                        .apply(getCursor(), newArray.getCoordinates().replace());
                  }
                  return super.visitNewArray(newArray, executionContext);
              }
          })),
          java(
            """
              public class Test {
                  int[] array = new int[1];
              }
              """,
            """
              public class Test {
                  int[] array = Some.method();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2128")
    @Test
    void arrayTernaryToMethodAccess() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitTernary(J.Ternary ternary, ExecutionContext executionContext) {
                  maybeAddImport("java.util.Arrays");
                  return JavaTemplate.builder("Arrays.asList(#{any()})")
                    .imports("java.util.Arrays")
                    .build()
                    .apply(getCursor(), ternary.getCoordinates().replace(), ternary);
              }
          }).withMaxCycles(1)),
          java(
            """
              abstract class Test {
                  abstract String[] array();

                  void test(boolean condition) {
                      Object any = condition ? array() : new String[]{"Hello!"};
                  }
              }
              """,
            """
              import java.util.Arrays;
                            
              abstract class Test {
                  abstract String[] array();
                  
                  void test(boolean condition) {
                      Object any = Arrays.asList(condition ? array() : new String[]{"Hello!"});
                  }
              }
              """
          )
        );
    }

    @Test
    void anyIsGenericWithUnknownType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return JavaTemplate.builder("System.out.println(#{any()})")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace(), method);
              }
          }).withMaxCycles(1)),
          java(
            """
              import java.util.Map;
              class Test {
               void test(Map<String, ?> map) {
                map.get("test");
               }
              }
              """,
            """
              import java.util.Map;
              class Test {
               void test(Map<String, ?> map) {
                   System.out.println(map.get("test"));
               }
              }
              """
          )
        );
    }
}
