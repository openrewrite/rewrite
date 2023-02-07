/*
 * Copyright 2022 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class FinalizePrivateFieldsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FinalizePrivateFields());
    }

    @Test
    void modifierAndVariableTypeFlagSet() {
        rewriteRun(
          java(
            """
              class A {
                  private static String TEST_STRING = "ABC";
              }
              """,
            """
              class A {
                  private static final String TEST_STRING = "ABC";
              }
              """, spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations declarations = (J.VariableDeclarations) cu.getClasses().get(
                  0).getBody().getStatements().get(0);
                assertThat(declarations.getModifiers()).anySatisfy(
                  m -> assertThat(m.getType()).isEqualTo(J.Modifier.Type.Final));
                assertThat(declarations.getVariables().get(0).getVariableType().getFlags()).contains(Flag.Final);
            })));
    }

    @Test
    void fieldWithInitializerMadeFinal() {
        rewriteRun(
          java(
            """
              class A {
                  private String name = "ABC";

                  String getName() {
                      return name;
                  }
              }
              """,
            """
              class A {
                  private final String name = "ABC";

                  String getName() {
                      return name;
                  }
              }
              """));
    }

    @Test
    void fieldWithInitializerViaMethodMadeFinal() {
        rewriteRun(
          java(
            """
              class A {
                  private String name = initName();

                  String initName() {
                      return "A name";
                  }
              }
              """,
            """
              class A {
                  private final String name = initName();

                  String initName() {
                      return "A name";
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldAssignedInConstructorMadeFinal() {
        rewriteRun(
          java(
            """
              class A {
                  private String name;

                  A() {
                      name = "XYZ";
                  }
              }
              """,
            """
              class A {
                  private final String name;

                  A() {
                      name = "XYZ";
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2807")
    @Test
    void fieldAssignedInConstructorMightHaveBeenNotInitializedIgnored() {
        rewriteRun(
          java(
            """
              class A {
                  private int a;
                  private int b;

                  A(boolean condition, int type) {
                      if (condition) {
                          a = 1;
                      }

                      switch (type) {
                          case 0:
                              b = 2;
                              break;
                          default:
                              break;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void multiVariablesMadeFinal() {
        rewriteRun(
          java(
            """
              class A {
                  private int a, b;

                  A() {
                      a = 0;
                      b = 1;
                  }

                  int func() {
                      return a + b;
                  }
              }
              """,
            """
              class A {
                  private final int a, b;

                  A() {
                      a = 0;
                      b = 1;
                  }

                  int func() {
                      return a + b;
                  }
              }
              """
          )
        );
    }

    @Test
    void multiVariablesReassigned() {
        rewriteRun(
          java(
            """
              class A {
                  private int a, b;

                  A() {
                      a = 0;
                      b = 1;
                  }

                  int func(int c) {
                      b += c;
                      return a + b;
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldReassignedByAMethod() {
        rewriteRun(
          java(
            """
              class A {
                  private String name = "ABC";

                  void func() {
                      name = "XYZ";
                  }

                  String getName() {
                      return name;
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldReassignedByAMethodUsingThis() {
        rewriteRun(
          java(
            """
              class A {
                  private String name = "ABC";
                  private int a = 0;

                  void func() {
                      this.name = "XYZ";
                      this.a += 1;
                  }

                  String getName() {
                      return name;
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldReassignedByAMethodUsingClassAndThis() {
        rewriteRun(
          java(
            """
              class A {
                  private String name = "ABC";

                  void func() {
                      A.this.name = "XYZ";
                  }

                  String getName() {
                      return name;
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldOfAFieldReassignedByAMethodUsingThis() {
        rewriteRun(
          java(
            """
              class A {
                  private B b = new B();

                  void func() {
                      this.b.num = 1;
                  }
              }
              """,
            """
              class A {
                  private final B b = new B();

                  void func() {
                      this.b.num = 1;
                  }
              }
              """
          ),
          java(
            """
              class B {
                  public int num = 0;
              }
              """
          )
        );
    }

    @Test
    void fieldReassignedInConstructor() {
        rewriteRun(
          java(
            """
              class A {
                  private String name = "ABC";

                  A() {
                      name = "XYZ";
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldAssignedInConstructorViaThis() {
        rewriteRun(
          java(
            """
              class A {
                  private String name;

                  A() {
                    this.name = "XYZ";
                  }
              }
              """,
            """
              class A {
                  private final String name;

                  A() {
                    this.name = "XYZ";
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldReferencedByNonModifyingUnaryOperator() {
        rewriteRun(
          java(
            """
              class A {
                  private int i = 42;
                  private int j = -i;
              }
              """,
            """
              class A {
                  private final int i = 42;
                  private final int j = -i;
              }
              """
          )
        );
    }

    @Disabled ("Doesn't support multiple constructors, to be enhanced")
    @Test
    void fieldAssignedInAllAlternateConstructors() {
        rewriteRun(
          java(
            """
              class A {
                  private String name;

                  A() {
                    name = "XYZ";
                  }

                  A(String n) {
                    name = n;
                  }
              }
              """,
            """
              class A {
                  private final String name;

                  A() {
                    name = "XYZ";
                  }

                  A(String n) {
                    name = n;
                  }
              }
              """
          )
        );
    }

    @Disabled("Doesn't support if-else conditions analysis, to be enhanced.")
    @Test
    void fieldAssignedInIfElseStatementsInConstructor() {
        rewriteRun(
          java(
            """
              class A {
                  private String name;

                  A(boolean condition) {
                      if (condition) {
                          name = "ABC";
                      } else {
                          name = "XYZ";
                      }
                  }
              }
              """,
            """
              class A {
                  private final String name;

                  A(boolean condition) {
                      if (condition) {
                          name = "ABC";
                      } else {
                          name = "XYZ";
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldAssignedIndirectlyInAllAlternateConstructors() {
        rewriteRun(
          java(
            """
              class A {
                  private String name;

                  A() {
                    this("ABC");
                  }
                  A(String name) {
                    this.name = name;
                  }
              }
              """,
            """
              class A {
                  private final String name;

                  A() {
                    this("ABC");
                  }
                  A(String name) {
                    this.name = name;
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldReassignedInAlternateConstructors() {
        rewriteRun(
          java(
            """
              class A {
                  private String name;

                  A() {
                    this("ABC");
                    name = "XYZ";
                  }
                  A(String name) {
                    this.name = name;
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldReassignedInConstructorMultipleTimes() {
        rewriteRun(
          java(
            """
              class A {
                  private String name;

                  A() {
                      name = "ABC";
                      name = "XYZ";
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldsReassignedInLoops() {
        rewriteRun(
          java(
            """
              class A {
                  private int a;
                  private int b;
                  private int c;

                  A() {
                      for (int i = 0; i< 10; i++) {
                          a = i;
                      }

                      int k = 0;
                      while (k < 10) {
                          b = k;
                          k++;
                      }

                      do {
                          k--;
                          c = k;
                      } while(k > 5);
                  }
              }
              """
          )
        );
    }

    @Test
    void nonPrivateFieldsIgnored() {
        rewriteRun(
          java(
            """
              class A {
                  int a = 0;
                  public int b = 1;
                  protected int c = 2;
              }
              """
          )
        );
    }

    @Test
    void finalFieldsIgnored() {
        rewriteRun(
          java(
            """
              class A {
                  private final int a = 0;
              }
              """
          )
        );
    }

    @Test
    void staticFieldsMadeFinal() {
        rewriteRun(
          java(
            """
              class A {
                  private static int num = 10;
                  int func() {
                      return num;
                  }
              }
              """,
            """
              class A {
                  private static final int num = 10;
                  int func() {
                      return num;
                  }
              }
              """
          )
        );
    }

    @Test
    void initializedByInitializerBlock() {
        rewriteRun(
          java(
            """
              public class Person {
                  {
                      name = "N1";
                      age = 10;
                      address = "CA";
                  }

                  private String name = "N2";
                  private int age = 15;
                  private String address;

                  public Person() {
                      name = "N3";
                      age = 20;
                  }
              }
              """,
            """
              public class Person {
                  {
                      name = "N1";
                      age = 10;
                      address = "CA";
                  }

                  private String name = "N2";
                  private int age = 15;
                  private final String address;

                  public Person() {
                      name = "N3";
                      age = 20;
                  }
              }
              """
          )
        );
    }

    @Test
    void staticInitializerBlock() {
        rewriteRun(
          java(
            """
              class A {
                  static {
                      num = 10;
                  }
                  private static int num;

                  int func() {
                      return num;
                  }
              }
              """,
            """
              class A {
                  static {
                      num = 10;
                  }
                  private static final int num;

                  int func() {
                      return num;
                  }
              }
              """
          )
        );
    }

    @Test
    void innerClassFieldMadeFinal() {
        rewriteRun(
          java(
            """
              class OuterClass {
                  int a;

                  class InnerClass {
                      private int b = 1;
                  }
              }
              """,
            """
              class OuterClass {
                  int a;

                  class InnerClass {
                      private final int b = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void staticNestedClassFieldMadeFinal() {
        rewriteRun(
          java(
            """
              class OuterClass {
                  int a;

                  static class InnerClass {
                      private int b = 1;

                      int func() {
                          return b;
                      }
                  }
              }
              """,
            """
              class OuterClass {
                  int a;

                  static class InnerClass {
                      private final int b = 1;

                      int func() {
                          return b;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void notInitializedByClassIgnored() {
        rewriteRun(
          java(
            """
              class A {
                  private int a;
                  void func() {
                      a = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldAssignedInLambdaInsideConstructor() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class A {
                  private int x;
                  private int y;
                  A() {
                      List.of(1,2,3).forEach(n -> x = n);
                      Runnable r = () -> y = 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void anyFieldAnnotationAppliedIgnored() {
        rewriteRun(
          java(
            """
              import lombok.Setter;

              public class A {
                  @Setter
                  private int num = 1;
                  private @Setter String name = "ABC";

                  static void test() {
                      A a = new A();
                      a.setNum(2);
                      a.setName("XYZ");
                  }
              }
              """
          )
        );
    }

    @Test
    void anyAnnotationAppliedClassIgnored() {
        rewriteRun(
          java(
            """
              import lombok.Data;
                          
              @Data
              public class B {
                  private int num = 0;
                          
                  void func() {
                      B b = new B();
                      b.setNum(1);
                  }
              }
              """
          )
        );
    }
}
