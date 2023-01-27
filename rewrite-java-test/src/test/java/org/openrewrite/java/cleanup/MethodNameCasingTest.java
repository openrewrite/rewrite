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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcTestJava;
import static org.openrewrite.test.RewriteTest.toRecipe;

@Issue("https://github.com/openrewrite/rewrite/issues/466")
class MethodNameCasingTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MethodNameCasing(false, false));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2571")
    @Test
    void noChangesOnMethodsBeginningWithUnderscore() {
        rewriteRun(
          java(
            """
              class Test {
                  void _1() {}
                  void _finally() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2557")
    @Test
    void interfaceMethods() {
        rewriteRun(
          java(
            """
              interface Test {
                  void getFoo_bar() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2557")
    @Test
    void annotationMethods() {
        rewriteRun(
          java(
            """
              @interface Test {
                  String getFoo_bar();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2424")
    @Test
    void correctMethodNameCasing() {
        rewriteRun(
          java(
            """
              class Test {
                  private String getFoo_bar() {
                      return "foobar";
                  }
              }
              """,
            """
              class Test {
                  private String getFooBar() {
                      return "foobar";
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2424")
    @Test
    void doNotRenamePublicMethods() {
        rewriteRun(
          java(
            """
              class Test {
                  public void getFoo_bar() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2424")
    @Test
    void doNotRenamePublicMethodsNullOptions() {
        rewriteRun(
        spec -> spec.recipe(new MethodNameCasing(null, null)),
          java(
            """
              class Test {
                  public void getFoo_bar() {}
              }
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2424")
    @Test
    void okToRenamePublicMethods() {
        rewriteRun(
          spec -> spec.recipe(new MethodNameCasing(true, true)),
          java(
            """
              class Test {
                  public void getFoo_bar(){}
              }
              """,
            """
              class Test {
                  public void getFooBar(){}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1741")
    @Test
    void doNotApplyToTest() {
        rewriteRun(
          srcTestJava(
            java(
              """
                class Test {
                    void MyMethod_with_über() {
                    }
                }
                """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1741")
    @Test
    void applyChangeToTest() {
        rewriteRun(
          spec -> spec.recipe(new MethodNameCasing(true, false)),
          srcTestJava(
            java(
              """
                    class Test {
                        void MyMethod_with_über() {
                        }
                    }
                """,
              """
                    class Test {
                        void myMethodWithUber() {
                        }
                    }
                """
            )
          )
        );
    }

    @Test
    void changeMethodDeclaration() {
        rewriteRun(
          java(
            """
                  class Test {
                      void MyMethod_with_über() {
                      }
                  }
              """,
            """
                  class Test {
                      void myMethodWithUber() {
                      }
                  }
              """
          )
        );
    }

    @Test
    void changeCamelCaseMethodWithFirstLetterUpperCase() {
        rewriteRun(
          java(
            """
                  class Test {
                      void MyMethod() {
                      }
                  }
              """,
            """
                  class Test {
                      void myMethod() {
                      }
                  }
              """
          )
        );
    }

    @Test
    void changeMethodInvocations() {
        rewriteRun(
          java(
            """
                  class Test {
                      void MyMethod_with_über() {
                      }
                  }
              """, """
                  class Test {
                      void myMethodWithUber() {
                      }
                  }
              """
          ),
          java(
            """
                  class A {
                      void test() {
                          new Test().MyMethod_with_über();
                      }
                  }
              """,
            """
                  class A {
                      void test() {
                          new Test().myMethodWithUber();
                      }
                  }
              """
          )
        );
    }

    @Test
    void dontChangeCorrectlyCasedMethods() {
        rewriteRun(
          java(
            """
              class Test {
                  void dontChange() {
                  }
              }
              """
          )
        );
    }

    @Test
    void changeMethodNameWhenOverride() {
        rewriteRun(
          java(
            """
              class ParentClass {
                  void Method() {
                  }
              }
              """,
            """
              class ParentClass {
                  void method() {
                  }
              }
              """
          ),
          java(
            """
              class Test extends ParentClass {
                  @Override
                  void Method() {
                  }
              }
              """,
            """
              class Test extends ParentClass {
                  @Override
                  void method() {
                  }
              }
              """
          )
        );
    }

    @Test
    void newNameExists() {
        rewriteRun(
          java(
            """
              class Test {
                  void Method() {
                  }
                  void method() {
                  }
              }
              """
          )
        );
    }

    @Test
    void nameExistsInInnerClass() {
        rewriteRun(
          java(
            """
              class T {
                  void Method(){}
                  
                  private static class M {
                      void Method(){}
                  }
              }
              """,
            """
              class T {
                  void method(){}
                  
                  private static class M {
                      void method(){}
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Issue("https://github.com/openrewrite/rewrite/issues/2103")
    @Test
    void snakeCaseToCamelCase() {
        rewriteRun(
          java(
            """
              class T {
                  private static int SOME_METHOD() {
                    return 1;
                  }
                  private static int some_method_2() {
                    return 1;
                  }
                  private static int some_über_method() {
                    return 1;
                  }
                  public static void anotherMethod() {
                    int i = SOME_METHOD();
                    i = some_method_2();
                    i = some_über_method();
                  }
              }
              """,
            """
              class T {
                  private static int someMethod() {
                    return 1;
                  }
                  private static int someMethod2() {
                    return 1;
                  }
                  private static int someUberMethod() {
                    return 1;
                  }
                  public static void anotherMethod() {
                    int i = someMethod();
                    i = someMethod2();
                    i = someUberMethod();
                  }
              }
              """
          )
        );
    }

    // This test uses a recipe remove ClassDeclaration types information prior to running the MethodNameCasing recipe.
    // This results in a change with an empty diff, thus before and after sources are identical
    @Issue("https://github.com/openrewrite/rewrite/issues/2103")
    @Test
    void doesNotRenameMethodInvocationsWhenTheMethodDeclarationsClassTypeIsNull() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()).recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                  return super.visitClassDeclaration(classDecl, executionContext).withType(null);
              }
          }).doNext(new MethodNameCasing(true, false))),
          java(
            """
              package abc;
              class T {
                  public static int MyMethod() {return -1;}
                  public static void anotherMethod() {
                      int i = MyMethod();
                  }
              }
              """,
            """
              package abc;
              class T {
                  public static int MyMethod() {return -1;}
                  public static void anotherMethod() {
                      int i = MyMethod();
                  }
              }
              """
          )
        );
    }

    @Test
    void keepCamelCase() {
        rewriteRun(
          java(
            """
              class Test {
                  private void Method() {
                  
                  }
              }
              """,
            """
              class Test {
                  private void method() {
                  
                  }
              }
              """
          )
        );
    }

    @Test
    void keepCamelCase2() {
        rewriteRun(
          java(
            """
              import java.util.*;
                            
              class Test {
                  private List<String> GetNames() {
                      List<String> result = new ArrayList<>();
                      result.add("Alice");
                      result.add("Bob");
                      result.add("Carol");
                      return result;
                  }
                  
                  public void run() {
                      for (String n: GetNames()) {
                          System.out.println(n);
                      }
                  }
              }
              """,
            """
              import java.util.*;
                            
              class Test {
                  private List<String> getNames() {
                      List<String> result = new ArrayList<>();
                      result.add("Alice");
                      result.add("Bob");
                      result.add("Carol");
                      return result;
                  }
                  
                  public void run() {
                      for (String n: getNames()) {
                          System.out.println(n);
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked", "rawtypes"})
    @Test
    void changeNameOfMethodWithArrayArgument() {
        rewriteRun(
          java(
            """
              import java.util.*;
                            
              class Test {
                  private List<String> GetNames(String[] names) {
                      List<String> result = new ArrayList<>(Arrays.asList(names));
                      return result;
                  }
              }
              """,
            """
              import java.util.*;
                            
              class Test {
                  private List<String> getNames(String[] names) {
                      List<String> result = new ArrayList<>(Arrays.asList(names));
                      return result;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2261")
    @Test
    void unknownParameterTypes() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
              class Test {
                  private void Foo(Unknown u) {
                  }
              }
              """,
            """
              class Test {
                  private void foo(Unknown u) {
                  }
              }
              """
          )
        );
    }
}
