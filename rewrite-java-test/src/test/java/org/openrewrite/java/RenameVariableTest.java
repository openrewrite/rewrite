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
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"SimplifyStreamApiCallChains", "ConstantConditions", "UnnecessaryLocalVariable", "LocalVariableUsedAndDeclaredInDifferentSwitchBranches", "Convert2Diamond", "UnusedAssignment"})
class RenameVariableTest implements RewriteTest {

    @DocumentExample
    @Test
    void doNotRenameForLoopVariables() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              package org.openrewrite;

              public class A {
                  int fooA() {
                      v++;
                      // creates new scope owned by for loop.
                      for (int v = 0; v < 10; ++v) {
                          int x = v + 1;
                      }
                      v++;
                  }
                  // v is scoped to classDeclaration regardless of statement order.
                  public int v = 1;
              }
              """,
            """
              package org.openrewrite;

              public class A {
                  int fooA() {
                      VALUE++;
                      // creates new scope owned by for loop.
                      for (int v = 0; v < 10; ++v) {
                          int x = v + 1;
                      }
                      VALUE++;
                  }
                  // v is scoped to classDeclaration regardless of statement order.
                  public int VALUE = 1;
              }
              """
          )
        );
    }
    private static Recipe renameVariableTest(String hasName, String toName, boolean includeMethodParameters) {
        return toRecipe(() -> new JavaVisitor<>() {
            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if ("A".equals(classDecl.getSimpleName())) {
                    List<J.VariableDeclarations> variableDecls = classDecl.getBody().getStatements().stream()
                      .filter(J.VariableDeclarations.class::isInstance)
                      .map(J.VariableDeclarations.class::cast)
                      .collect(toList());

                    if (includeMethodParameters) {
                        variableDecls.addAll(
                          classDecl.getBody().getStatements().stream()
                            .filter(J.MethodDeclaration.class::isInstance)
                            .map(J.MethodDeclaration.class::cast)
                            .flatMap(it -> it.getParameters().stream())
                            .filter(J.VariableDeclarations.class::isInstance)
                            .map(J.VariableDeclarations.class::cast)
                            .collect(toList())
                        );
                    }

                    List<J.VariableDeclarations.NamedVariable> namedVariables = new ArrayList<>();
                    for (J.VariableDeclarations variableDecl : variableDecls) {
                        namedVariables.addAll(variableDecl.getVariables());
                    }

                    for (J.VariableDeclarations.NamedVariable namedVariable : namedVariables) {
                        if (namedVariable.getSimpleName().equals(hasName)) {
                            doAfterVisit(new RenameVariable<>(namedVariable, toName));
                        }
                    }
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }
        });
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3772")
    @Test
    void renameFieldWithSameNameAsParameterWithJavaDoc() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("name", "_name", false)),
          java(
            """
              public class A {
                  private String name;

                  /**
                   * The length of <code>name</code> added to the length of {@link #name}.
                   *
                   * @param name My parameter.
                   */
                  int fooA(String name) {
                      return name.length() + this.name.length();
                  }
              }
              """,
            """
              public class A {
                  private String _name;

                  /**
                   * The length of <code>name</code> added to the length of {@link #_name}.
                   *
                   * @param name My parameter.
                   */
                  int fooA(String name) {
                      return name.length() + this._name.length();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2285")
    @Test
    void variableNameExistsMultipleScopes() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "myVal", false)),
          java(
            """
              class A {
                  String v = "";
                  String getVal(String v) {
                      return this.v + v;
                  }
              }
              """,
            """
              class A {
                  String myVal = "";
                  String getVal(String v) {
                      return this.myVal + v;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2285")
    @Test
    void staticVariableReferencedByClassName() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "aVal", false)),
          java(
            """
              class A {
                  static String v = "";
                  String getVal(String v) {
                      return A.v;
                  }
                  class B {
                      String v = "x";
                      String getVal() {
                          return A.v + v;
                      }
                  }
              }
              """,
            """
              class A {
                  static String aVal = "";
                  String getVal(String v) {
                      return A.aVal;
                  }
                  class B {
                      String v = "x";
                      String getVal() {
                          return A.aVal + v;
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2285")
    @Test
    void isParameterizedClass() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("_val", "v", true)),
          java(
            """
              package org.openrewrite;

              public class A<T> {
                  private String _val;
                  private String name;

                  A(String name, String _val) {
                      this._val = _val;
                      this.name = name;
                  }
              }
              """,
            """
              package org.openrewrite;

              public class A<T> {
                  private String v;
                  private String name;

                  A(String name, String v) {
                      this.v = v;
                      this.name = name;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeToJavaKeyword() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "int", true)),
          java(
            """
              package org.openrewrite;

              public class A {
                  int fooA() {
                      v++;
                      // creates new scope owned by for loop.
                      for (int v = 0; v < 10; ++v) {
                          int x = v + 1;
                      }
                      v++;
                  }
                  // v is scoped to classDeclaration regardless of statement order.
                  public int v = 1;
              }
              """
          )
        );
    }

    @Test
    void renameForLoopVariables() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              package org.openrewrite;

              public class A {
                  int fooA() {
                      // refers to class declaration scope.
                      for (v = 0; v < 10; ++v) {
                          int x = v + 1;
                          int v = 10;
                          v++;
                      }
                  }
                  // v is scoped to classDeclaration regardless of statement order.
                  public int v = 1;
              }
              """,
            """
              package org.openrewrite;

              public class A {
                  int fooA() {
                      // refers to class declaration scope.
                      for (VALUE = 0; VALUE < 10; ++VALUE) {
                          int x = VALUE + 1;
                          int v = 10;
                          v++;
                      }
                  }
                  // v is scoped to classDeclaration regardless of statement order.
                  public int VALUE = 1;
              }
              """
          )
        );
    }

    @Test
    void doNotRenameInnerClassVariables() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              package org.openrewrite;

              public class A {
                  // Scoped to ClassDeclaration regardless of statement order.
                  public int v = 1;

                  class X {
                      int v = 0;
                  }
              }
              """,
            """
              package org.openrewrite;

              public class A {
                  // Scoped to ClassDeclaration regardless of statement order.
                  public int VALUE = 1;

                  class X {
                      int v = 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void renameInnerClassVariables() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              package org.openrewrite;

              public class A {
                  public int v = 0;

                  class X {
                      int foo() {
                          v = 10;
                          return v;
                      }
                  }
              }
              """,
            """
              package org.openrewrite;

              public class A {
                  public int VALUE = 0;

                  class X {
                      int foo() {
                          VALUE = 10;
                          return VALUE;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void renameTry() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              package org.openrewrite;

              import java.io.FileInputStream;
              import java.io.FileDescriptor;

              public class A {
                  public int v = 0;

                  int foo() {
                      try (FileInputStream fs = new FileInputStream("file")) {
                          FileDescriptor fd = fs.getFD();
                          v++;
                      } catch (Exception ex) {
                          throw new RuntimeException("" + v, ex);
                      }
                      return v;
                  }
              }
              """,
            """
              package org.openrewrite;

              import java.io.FileInputStream;
              import java.io.FileDescriptor;

              public class A {
                  public int VALUE = 0;

                  int foo() {
                      try (FileInputStream fs = new FileInputStream("file")) {
                          FileDescriptor fd = fs.getFD();
                          VALUE++;
                      } catch (Exception ex) {
                          throw new RuntimeException("" + VALUE, ex);
                      }
                      return VALUE;
                  }
              }
              """
          )
        );
    }

    @Test
    void renameCatchWithoutResource() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              package org.openrewrite;

              import java.io.FileDescriptor;
              import java.io.FileInputStream;

              public class A {
                  public int v = 0;

                  int foo() {
                      try {
                          v++;
                          String v = "1234";
                          Integer.valueOf(v);
                      } catch (Exception ex) {
                          throw new RuntimeException("" + v, ex);
                      }
                      return v;
                  }
              }
              """,
            """
              package org.openrewrite;

              import java.io.FileDescriptor;
              import java.io.FileInputStream;

              public class A {
                  public int VALUE = 0;

                  int foo() {
                      try {
                          VALUE++;
                          String v = "1234";
                          Integer.valueOf(v);
                      } catch (Exception ex) {
                          throw new RuntimeException("" + VALUE, ex);
                      }
                      return VALUE;
                  }
              }
              """
          )
        );
    }

    @Test
    void renameCatchWithTryResource() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              package org.openrewrite;

              import java.io.FileDescriptor;
              import java.io.FileInputStream;

              public class A {
                  public int v = 0;

                  int foo() {
                      FileDescriptor fd;
                      try (FileInputStream v = new FileInputStream("file")) {
                          fd = v.getFD();
                      } catch (Exception ex) {
                          throw new RuntimeException("" + v, ex);
                      }
                      return fd.hashCode();
                  }
              }
              """,
            """
              package org.openrewrite;

              import java.io.FileDescriptor;
              import java.io.FileInputStream;

              public class A {
                  public int VALUE = 0;

                  int foo() {
                      FileDescriptor fd;
                      try (FileInputStream v = new FileInputStream("file")) {
                          fd = v.getFD();
                      } catch (Exception ex) {
                          throw new RuntimeException("" + VALUE, ex);
                      }
                      return fd.hashCode();
                  }
              }
              """
          )
        );
    }

    @Test
    void renameVariablesInLambda() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              package org.openrewrite;

              import java.util.function.BiFunction;

              public class A {
                  public Integer v = 1;

                  int onlyChangeInScope() {
                      BiFunction<String, Integer, Integer> x = (String j, Integer k) ->
                            v == null ? 42 : v + 41;

                      // Class scope.
                      v = 10;
                      // Method scope.
                      int v = 10;
                      return v;
                  }
              }
              """,
            """
              package org.openrewrite;

              import java.util.function.BiFunction;

              public class A {
                  public Integer VALUE = 1;

                  int onlyChangeInScope() {
                      BiFunction<String, Integer, Integer> x = (String j, Integer k) ->
                            VALUE == null ? 42 : VALUE + 41;

                      // Class scope.
                      VALUE = 10;
                      // Method scope.
                      int v = 10;
                      return v;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotRenameVariablesInLambda() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              package org.openrewrite;

              import java.util.function.BiFunction;

              public class A {
                  public Integer v = 1;

                  int onlyChangeInScope() {
                      BiFunction<String, Integer, Integer> x = (String k, Integer v) ->
                      v == null ? 42 : v + 41;

                      // Class scope.
                      v = 10;
                  }
              }
              """,
            """
              package org.openrewrite;

              import java.util.function.BiFunction;

              public class A {
                  public Integer VALUE = 1;

                  int onlyChangeInScope() {
                      BiFunction<String, Integer, Integer> x = (String k, Integer v) ->
                      v == null ? 42 : v + 41;

                      // Class scope.
                      VALUE = 10;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void renameSwitchCases() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              package org.openrewrite;

              public class A {
                  public int v = 1;

                  int newScopeDoNotChange() {
                      switch (v) {
                          case 1:
                              v++;
                              break;
                          case 2:
                              int v = 100;
                              v += 2;
                              break;
                          case 3:
                              v = 0;
                              break;
                          default:
                              break;
                      }
                      return v;
                  }
              }
              """,
            """
              package org.openrewrite;

              public class A {
                  public int VALUE = 1;

                  int newScopeDoNotChange() {
                      switch (VALUE) {
                          case 1:
                              VALUE++;
                              break;
                          case 2:
                              int v = 100;
                              v += 2;
                              break;
                          case 3:
                              VALUE = 0;
                              break;
                          default:
                              break;
                      }
                      return VALUE;
                  }
              }
              """
          )
        );
    }

    @Test
    void renameMethodVariables() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", false)),
          java(
            """
              package org.openrewrite;

              public class A {
                  public int v = 1;

                  int newScopeDoNotChange(int v) {
                      return v;
                  }
                  int onlyChangeInScope() {
                      // Class scope.
                      v = 10;
                      // Method scope.
                      int v = 10;
                      return v;
                  }
              }
              """,
            """
              package org.openrewrite;

              public class A {
                  public int VALUE = 1;

                  int newScopeDoNotChange(int v) {
                      return v;
                  }
                  int onlyChangeInScope() {
                      // Class scope.
                      VALUE = 10;
                      // Method scope.
                      int v = 10;
                      return v;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/1603")
    @Test
    void renameFieldAccessVariables() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              class ClassWithPublicField {
                  public int publicField = 10;
              }
              """
          ),
          java(
            """
              public class A {
                  public ClassWithPublicField v = new ClassWithPublicField();

                  int getNumberTwice() {
                      v.publicField = this.v.publicField + 10;
                      return v.publicField;
                  }
              }
              """,
            """
              public class A {
                  public ClassWithPublicField VALUE = new ClassWithPublicField();

                  int getNumberTwice() {
                      VALUE.publicField = this.VALUE.publicField + 10;
                      return VALUE.publicField;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/1603")
    @Test
    void renameLocalFieldAccessInStaticMethod() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("v", "VALUE", true)),
          java(
            """
              public class A {
                  private int v;

                  static A getInstance() {
                      A a = new A();
                      a.v = 12;
                      return a;
                  }
              }
              """,
            """
              public class A {
                  private int VALUE;

                  static A getInstance() {
                      A a = new A();
                      a.VALUE = 12;
                      return a;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"StatementWithEmptyBody", "ConstantConditions", "UnusedAssignment"})
    @Test
    void renameVariable() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (getCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration) {
                      doAfterVisit(new RenameVariable<>(multiVariable.getVariables().getFirst(), "n2"));
                  } else if (!(getCursor().getParentTreeCursor().getParentTreeCursor().getValue() instanceof J.ClassDeclaration)) {
                      doAfterVisit(new RenameVariable<>(multiVariable.getVariables().getFirst(), "n1"));
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          java(
            """
              public class B {
                  int n;

                  {
                      n++; // do not change.
                      int n;
                      n = 1;
                      n /= 2;
                      if(n + 1 == 2) {}
                      n++;
                  }

                  public int foo(int n) {
                      return n + this.n;
                  }
              }
              """,
            """
              public class B {
                  int n;

                  {
                      n++; // do not change.
                      int n1;
                      n1 = 1;
                      n1 /= 2;
                      if(n1 + 1 == 2) {}
                      n1++;
                  }

                  public int foo(int n2) {
                      return n2 + this.n;
                  }
              }
              """
          )
        );
    }

    @Test
    void renameMethodParam() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("c", "foobar", true)),
          java(
            """
              package org.openrewrite;

              public class A {
                  public boolean contains(String string, String c) {
                      return string.contains(c);
                  }
              }
              """,
            """
              package org.openrewrite;

              public class A {
                  public boolean contains(String string, String foobar) {
                      return string.contains(foobar);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3051")
    @Test
    void doNotRenameMethods() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("contains", "foobar", true)),
          java(
            """
              package org.openrewrite;

              public class A {
                  public boolean contains(String string, String contains) {
                      return string.contains(contains) && string.contains(string);
                  }
              }
              """,
            """
              package org.openrewrite;

              public class A {
                  public boolean contains(String string, String foobar) {
                      return string.contains(foobar) && string.contains(string);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotRenameNewClass() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("FooBarBaz", "fooBarBaz", true)),
          java(
            """
              class FooBarBaz {}
              class A {
                  FooBarBaz FooBarBaz = new FooBarBaz();
              }
              """,
            """
              class FooBarBaz {}
              class A {
                  FooBarBaz fooBarBaz = new FooBarBaz();
              }
              """
          )
        );
    }

    @Test
    void doNotRenameTypeParameter() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("FooBarBaz", "fooBarBaz", true)),
          java(
            """
              import java.util.ArrayList;
              class FooBarBaz {}
              class A {
                  ArrayList<FooBarBaz> FooBarBaz = new ArrayList<FooBarBaz>();
              }
              """,
            """
              import java.util.ArrayList;
              class FooBarBaz {}
              class A {
                  ArrayList<FooBarBaz> fooBarBaz = new ArrayList<FooBarBaz>();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4059")
    @Test
    void renameTypeCastedField() {
        rewriteRun(
          spec -> spec.recipe(renameVariableTest("objArray", "objects", false)),
          java(
            """
              import java.util.Arrays;

              public class A {
                private Object[] objArray;

                public boolean isEqualTo(Object object) {
                  return Arrays.equals(objArray, ((A) object).objArray);
                }

                public int length() {
                  return objArray.length;
                }
              }
              """,
            """
              import java.util.Arrays;

              public class A {
                private Object[] objects;

                public boolean isEqualTo(Object object) {
                  return Arrays.equals(objects, ((A) object).objects);
                }

                public int length() {
                  return objects.length;
                }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/5369")
    @Test
    void hiddenVariablesGetRenamedCorrectly() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                    if (getCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration) {
                        doAfterVisit(new RenameVariable<>(multiVariable.getVariables().getFirst(), "n1"));
                    }
                    return super.visitVariableDeclarations(multiVariable, ctx);
                }
            })),
          java(
            """
              public class A {
                private int n;

                public A setN(int n) {
                  this.n = n;
                  return this;
                }
              }
              """,
            """
              public class A {
                private int n;

                public A setN(int n1) {
                  this.n = n1;
                  return this;
                }
              }
              """
          )
        );
    }

    @Test
    void hiddenVariablesHierarchyRenameBase() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  J target = getCursor().getParentTreeCursor().getParentTreeCursor().getValue();
                  if ("hidden".equals(multiVariable.getVariables().getFirst().getSimpleName()) &&
                    target instanceof J.ClassDeclaration && "Base".equals(((J.ClassDeclaration) target).getSimpleName())) {
                      doAfterVisit(new RenameVariable<>(multiVariable.getVariables().getFirst(), "changed"));
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          java(
            """
              class Base {
                  protected Base visible;
                  protected Base hidden;

                  Base base() {
                      return hidden;
                  }
              }

              class Middle extends Base {
                  Middle middle() {
                      return (Middle) hidden;
                  }
              }

              class Extended extends Middle {
                  private Base hidden;

                  Extended extended() {
                      return (Extended) hidden;
                  }

                  public Extended test(Base hidden) {
                      this.hidden = super.hidden;
                      this.hidden = hidden.hidden;
                      this.hidden = ((Middle) hidden).hidden;
                      this.hidden = ((Extended) hidden).hidden;
                      base().hidden = this;
                      middle().hidden = this;
                      extended().hidden = this;
                      super.hidden = visible.hidden;
                      visible.hidden = hidden;
                      hidden.hidden.hidden = hidden.hidden = this.hidden = hidden;
                      return this;
                  }
              }
              """,
            """
              class Base {
                  protected Base visible;
                  protected Base changed;

                  Base base() {
                      return changed;
                  }
              }

              class Middle extends Base {
                  Middle middle() {
                      return (Middle) changed;
                  }
              }

              class Extended extends Middle {
                  private Base hidden;

                  Extended extended() {
                      return (Extended) hidden;
                  }

                  public Extended test(Base hidden) {
                      this.hidden = super.changed;
                      this.hidden = hidden.changed;
                      this.hidden = ((Middle) hidden).changed;
                      this.hidden = ((Extended) hidden).hidden;
                      base().changed = this;
                      middle().changed = this;
                      extended().hidden = this;
                      super.changed = visible.changed;
                      visible.changed = hidden;
                      hidden.changed.changed = hidden.changed = this.hidden = hidden;
                      return this;
                  }
              }
              """
          )
        );
    }

    @Test
    void hiddenVariablesHierarchyRenameExtended() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  J target = getCursor().getParentTreeCursor().getParentTreeCursor().getValue();
                  if ("hidden".equals(multiVariable.getVariables().getFirst().getSimpleName()) &&
                    target instanceof J.ClassDeclaration && "Extended".equals(((J.ClassDeclaration) target).getSimpleName())) {
                      doAfterVisit(new RenameVariable<>(multiVariable.getVariables().getFirst(), "changed"));
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          java(
            """
              class Base {
                  protected Base visible;
                  protected Base hidden;

                  Base base() {
                      return hidden;
                  }
              }

              class Middle extends Base {
                  Middle middle() {
                      return (Middle) hidden;
                  }
              }

              class Extended extends Middle {
                  private Base hidden;

                  Extended extended() {
                      return (Extended) hidden;
                  }

                  public Extended test(Base hidden) {
                      this.hidden = super.hidden;
                      this.hidden = hidden.hidden;
                      this.hidden = ((Middle) hidden).hidden;
                      this.hidden = ((Extended) hidden).hidden;
                      base().hidden = this;
                      middle().hidden = this;
                      extended().hidden = this;
                      super.hidden = visible.hidden;
                      visible.hidden = hidden;
                      hidden.hidden.hidden = hidden.hidden = this.hidden = hidden;
                      return this;
                  }
              }
              """,
            """
              class Base {
                  protected Base visible;
                  protected Base hidden;

                  Base base() {
                      return hidden;
                  }
              }

              class Middle extends Base {
                  Middle middle() {
                      return (Middle) hidden;
                  }
              }

              class Extended extends Middle {
                  private Base changed;

                  Extended extended() {
                      return (Extended) changed;
                  }

                  public Extended test(Base hidden) {
                      this.changed = super.hidden;
                      this.changed = hidden.hidden;
                      this.changed = ((Middle) hidden).hidden;
                      this.changed = ((Extended) hidden).changed;
                      base().hidden = this;
                      middle().hidden = this;
                      extended().changed = this;
                      super.hidden = visible.hidden;
                      visible.hidden = hidden;
                      hidden.hidden.hidden = hidden.hidden = this.changed = hidden;
                      return this;
                  }
              }
              """
          )
        );
    }

    @Test
    void hiddenVariablesHierarchyRenameLocal() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  J target = getCursor().getParentTreeCursor().getValue();
                  if ("hidden".equals(multiVariable.getVariables().getFirst().getSimpleName()) &&
                    target instanceof J.MethodDeclaration) {
                      doAfterVisit(new RenameVariable<>(multiVariable.getVariables().getFirst(), "changed"));
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          java(
            """
              class Base {
                  protected Base visible;
                  protected Base hidden;

                  Base base() {
                      return hidden;
                  }
              }

              class Middle extends Base {
                  Middle middle() {
                      return (Middle) hidden;
                  }
              }

              class Extended extends Middle {
                  private Base hidden;

                  Extended extended() {
                      return (Extended) hidden;
                  }

                  public Extended test(Base hidden) {
                      this.hidden = super.hidden;
                      this.hidden = hidden.hidden;
                      this.hidden = ((Middle) hidden).hidden;
                      this.hidden = ((Extended) hidden).hidden;
                      base().hidden = this;
                      middle().hidden = this;
                      extended().hidden = this;
                      super.hidden = visible.hidden;
                      visible.hidden = hidden;
                      hidden.hidden.hidden = hidden.hidden = this.hidden = hidden;
                      return this;
                  }
              }
              """,
            """
              class Base {
                  protected Base visible;
                  protected Base hidden;

                  Base base() {
                      return hidden;
                  }
              }

              class Middle extends Base {
                  Middle middle() {
                      return (Middle) hidden;
                  }
              }

              class Extended extends Middle {
                  private Base hidden;

                  Extended extended() {
                      return (Extended) hidden;
                  }

                  public Extended test(Base changed) {
                      this.hidden = super.hidden;
                      this.hidden = changed.hidden;
                      this.hidden = ((Middle) changed).hidden;
                      this.hidden = ((Extended) changed).hidden;
                      base().hidden = this;
                      middle().hidden = this;
                      extended().hidden = this;
                      super.hidden = visible.hidden;
                      visible.hidden = changed;
                      changed.hidden.hidden = changed.hidden = this.hidden = changed;
                      return this;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/5369")
    @Test
    void hiddenVariablesGetRenamedCorrectlyInBlocks() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().getFirst().isField(getCursor()) || multiVariable.getPrefix().getComments().isEmpty()) {
                      return multiVariable;
                  }
                  doAfterVisit(new RenameVariable<>(multiVariable.getVariables().getFirst(), "n1"));
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          java(
            """
              public class A {
                  int n;

                  public void blocks() {
                      {
                          //only this one is in scope
                          int n = 0;
                          int x = n;
                      }
                      {
                          int n = 0;
                          int x = n;
                      }
                  }
              }
              """,
            """
              public class A {
                  int n;

                  public void blocks() {
                      {
                          //only this one is in scope
                          int n1 = 0;
                          int x = n1;
                      }
                      {
                          int n = 0;
                          int x = n;
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/5369")
    @Test
    void hiddenVariablesGetRenamedCorrectlyInClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {

              @Override
              public J visitBlock(J.Block block, ExecutionContext ctx) {
                  if (getCursor().getParent() != null && getCursor().getParent().getValue() instanceof J.ClassDeclaration) {
                      for (Statement statement : block.getStatements()) {
                          if (statement instanceof J.VariableDeclarations) {
                              doAfterVisit(new RenameVariable<>(((J.VariableDeclarations) statement).getVariables().getFirst(), "n1"));
                          }
                      }
                  }
                  return super.visitBlock(block, ctx);
              }
          })),
          java(
            """
              public class A {
                  int n;

                  public void blocks() {
                      {
                          int n = 0;
                          int x = n;
                      }
                      {
                          int n = 0;
                          int x = n;
                      }
                  }
              }
              """,
            """
              public class A {
                  int n1;

                  public void blocks() {
                      {
                          int n = 0;
                          int x = n;
                      }
                      {
                          int n = 0;
                          int x = n;
                      }
                  }
              }
              """
          )
        );
    }
}
