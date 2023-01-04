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
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.HiddenFieldStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("UnnecessaryLocalVariable")
class HiddenFieldTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new HiddenField());
    }

    private static Consumer<RecipeSpec> hiddenFieldStyle(UnaryOperator<HiddenFieldStyle> with) {
        return spec -> spec.parser(JavaParser.fromJavaVersion().styles(
          singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              singletonList(with.apply(Checkstyle.hiddenFieldStyle())))))
        );
    }

    @Test
    void ignoreUnaffectedVariables() {
        rewriteRun(
          java(
            """
              class Test {
                  private String field;
                          
                  public Test(String someField) {
                  }
                          
                  static void method(String someField) {
                      String localVariable = someField;
                  }
              }
                """
          )
        );
    }

    @Test
    void renameHiddenFields() {
        rewriteRun(
          java(
            """
              public class B {
                  protected int n2;
                  int n3;
                  private int n4;
              }
              """
          ),
          java(
            """
              public class A extends B {
                  int n;
                  int n1;
                          
                  class C {
                      public void method(int n) {
                          int n1 = n;
                      }
                  }
                          
                  static class D {
                      public void method(int n) {
                      }
                  }
              }
              """,
            """
              public class A extends B {
                  int n;
                  int n1;
                          
                  class C {
                      public void method(int n2) {
                          int n3 = n2;
                      }
                  }
                          
                  static class D {
                      public void method(int n) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void constructorParameter() {
        rewriteRun(
          hiddenFieldStyle(style -> style.withIgnoreConstructorParameter(false)),
          java(
            """
              public class A {
                  private String field;
                          
                  public A(String field) {
                  }
              }
              """,
            """
              public class A {
                  private String field;
                          
                  public A(String field1) {
                  }
              }
              """
          )
        );
    }

    @Test
    void ignoreConstructorParameter() {
        rewriteRun(
          hiddenFieldStyle(style -> style.withIgnoreConstructorParameter(true)),
          java(
            """
              public class A {
                  private String field;
                      
                  public A(String field) {
                  }
              }
              """
          )
        );
    }

    @Test
    void methodParameter() {
        rewriteRun(
          java(
            """
              public class A {
                  private String field;
                          
                  public void method(String field) {
                  }
              }
              """,
            """
              public class A {
                  private String field;
                          
                  public void method(String field1) {
                  }
              }
              """
          )
        );
    }

    @Test
    void methodBodyLocalVariable() {
        rewriteRun(
          java(
            """
              public class A {
                  private String field;
                          
                  public void method(String param) {
                      String field = param;
                  }
              }
              """,
            """
              public class A {
                  private String field;
                          
                  public void method(String param) {
                      String field1 = param;
                  }
              }
              """
          )
        );
    }

    @Test
    void forLoops() {
        rewriteRun(
          java(
            """
              public class A {
                  int n;
                          
                  public void standardForLoop() {
                      for (int n = 0; n < 1; n++) {
                          int x = n;
                      }
                  }
                          
                  public void enhancedForLoop(int[] arr) {
                      for (int n : arr) {
                          int x = n;
                      }
                  }
              }
              """,
            """
              public class A {
                  int n;
                          
                  public void standardForLoop() {
                      for (int n1 = 0; n1 < 1; n1++) {
                          int x = n1;
                      }
                  }
                          
                  public void enhancedForLoop(int[] arr) {
                      for (int n1 : arr) {
                          int x = n1;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void blocks() {
        rewriteRun(
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
                  int n;

                  public void blocks() {
                      {
                          int n1 = 0;
                          int x = n1;
                      }
                      {
                          int n1 = 0;
                          int x = n1;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    @SuppressWarnings("ConstantValue")
    void tryResources() {
        rewriteRun(
          java(
            """
              public class A {
                  int n;

                  public void tryWithResources(int n4) {
                      Object n1 = null;
                      try (java.io.InputStream n = null; java.io.OutputStream n2 = null) {
                          Object n3 = null;
                          Object x = n;
                      }
                  }
              }
              """,
            """
              public class A {
                  int n;

                  public void tryWithResources(int n4) {
                      Object n1 = null;
                      try (java.io.InputStream n5 = null; java.io.OutputStream n2 = null) {
                          Object n3 = null;
                          Object x = n5;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    @SuppressWarnings({"EmptyTryBlock", "CatchMayIgnoreException", "TryWithIdenticalCatches"})
    void catchClause() {
        rewriteRun(
          java(
            """
              public class A {
                  int e;

                  public void tryCatch() {
                      try (java.io.InputStream e1 = null) {
                      } catch (RuntimeException e) {
                      } catch (java.io.IOException e) {
                      } catch (Exception e) {
                      }
                  }
              }
              """,
            """
              public class A {
                  int e;

                  public void tryCatch() {
                      try (java.io.InputStream e1 = null) {
                      } catch (RuntimeException e2) {
                      } catch (java.io.IOException e3) {
                      } catch (Exception e4) {
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"Convert2MethodRef", "ResultOfMethodCallIgnored"})
    @Test
    void lambdaWithTypedParameterHides() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.Arrays;
                          
              public class A {
                  List<Integer> numbers = Arrays.asList(1, 2, 3);
                  Integer value = 0;
                  {
                      numbers.forEach((Integer value) -> String.valueOf(value));
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.Arrays;
                          
              public class A {
                  List<Integer> numbers = Arrays.asList(1, 2, 3);
                  Integer value = 0;
                  {
                      numbers.forEach((Integer value1) -> String.valueOf(value1));
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedClasses() {
        rewriteRun(
          java(
            """
              public class Outer {
                  int outer;
                            
                  public class Inner {
                      int inner;
                          
                      public Inner() {
                          int inner = 0;
                      }
                            
                      public Inner(int inner) {
                      }
                            
                      public void method() {
                          int outer = 0;
                      }
                  }
              }
              """,
            """
              public class Outer {
                  int outer;
                            
                  public class Inner {
                      int inner;
                            
                      public Inner() {
                          int inner1 = 0;
                      }
                            
                      public Inner(int inner) {
                      }
                            
                      public void method() {
                          int outer1 = 0;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void incrementRenamedVariableNameUntilUnique() {
        rewriteRun(
          java(
            """
              public class A {
                  int n, n1;
                          
                  public void method(int n) {
                      int n1 = 0;
                  }
              }
              """,
            """
              public class A {
                  int n, n1;
                          
                  public void method(int n2) {
                      int n3 = 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void incrementRenamedVariableNameShouldNotCollideWithExistingVariablesInUse() {
        rewriteRun(
          java(
            """
              public class A {
                  int n, n1;
                          
                  public void method(int n) {
                      int n2 = 0;
                  }
              }
              """,
            """
              public class A {
                  int n, n1;
                          
                  public void method(int n3) {
                      int n2 = 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void ignoreEnums() {
        rewriteRun(
          java(
            """
              enum ExampleEnum {
                  A(0),
                  B(1),
                  C(2) {
                      int hidden;
                          
                      public void method() {
                          int hidden = 0;
                      }
                  };
                          
                  int hidden;
                  static int hiddenStatic;
                          
                  ExampleEnum(int hidden) {
                  }
                          
                  public void method() {
                      int hidden = 0;
                  }
                          
                  public static void methodStatic() {
                      int hiddenStatic = 0;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void ignoreStaticMethodsAndInitializers() {
        rewriteRun(
          java(
            """
              public class StaticMethods {
                  private int notHidden;
                          
                  public static void method() {
                      // local variables of static methods don't hide instance fields.
                      int notHidden;
                  }
                          
                  static {
                      // local variables of static initializers don't hide instance fields.
                      int notHidden;
                  }
                          
                  private int x;
                  private static int y;
                          
                  static class Inner {
                      void useX(int x) {
                          x++;
                      }
                          
                      void useY(int y) {
                          y++;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void ignoreInterfaces() {
        rewriteRun(
          java(
            """
              interface A {
                  int n = 0;
                          
                  void method(int n);
              }
              """
          )
        );
    }

    @Test
    void renamesSetters() {
        rewriteRun(
          hiddenFieldStyle(style -> style.withIgnoreSetter(false)),
          java(
            """
              class A {
                  int n;
                          
                  public void setN(int n) {
                      this.n = n;
                  }
              }
                          
              class B {
                  int n;
                          
                  public B setN(int n) {
                      this.n = n;
                      return this;
                  }
              }
              """,
            """
              class A {
                  int n;
                          
                  public void setN(int n1) {
                      this.n = n1;
                  }
              }
                          
              class B {
                  int n;
                          
                  public B setN(int n1) {
                      this.n = n1;
                      return this;
                  }
              }
              """
          )
        );
    }

    @Test
    void ignoreVoidSettersAndChangeSettersThatReturnItsClass() {
        rewriteRun(
          hiddenFieldStyle(style -> style.withIgnoreSetter(true).withSetterCanReturnItsClass(false)),
          java(
            """
              class A {
                  int n;
                          
                  public void setN(int n) {
                      this.n = n;
                  }
              }
                          
              class B {
                  int n;
                          
                  public B setN(int n) {
                      this.n = n;
                      return this;
                  }
              }
              """,
            """
              class A {
                  int n;
                          
                  public void setN(int n) {
                      this.n = n;
                  }
              }
                          
              class B {
                  int n;
                          
                  public B setN(int n1) {
                      this.n = n1;
                      return this;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1129")
    @Test
    void ignoreVoidSettersAndSettersThatReturnItsClass() {
        rewriteRun(
          hiddenFieldStyle(style -> style.withIgnoreSetter(true).withSetterCanReturnItsClass(true)),
          java(
            """
              class A {
                  int n;
                          
                  public void setN(int n) {
                      this.n = n;
                  }
              }
                          
              class B {
                  int n;
                          
                  public B setN(int n) {
                      this.n = n;
                      return this;
                  }
              }
              """
          )
        );
    }

    @Test
    void renamesAbstractMethodParameters() {
        rewriteRun(
          java(
            """
              public abstract class A {
                  int n;
                          
                  public abstract void method(int n);
              }
              """,
            """
              public abstract class A {
                  int n;
                          
                  public abstract void method(int n1);
              }
              """
          )
        );
    }

    @Test
    void ignoreAbstractMethodParameters() {
        rewriteRun(
          hiddenFieldStyle(style -> style.withIgnoreAbstractMethods(true)),
          java(
            """
              public abstract class A {
                  int n;
                          
                  public abstract void method(int n);
              }
                """
          )
        );
    }

    @Test
    void updateJavaDocParamName() {
        rewriteRun(
          java(
            """
              public abstract class A {
                  int n;
                          
                  /**
                   * @param n rename param
                   */
                  public abstract void method(int n);
              }
              """,
            """
              public abstract class A {
                  int n;
                          
                  /**
                   * @param n1 rename param
                   */
                  public abstract void method(int n1);
              }
              """
          )
        );
    }

    @Test
    void renamingVariableInSubclassShouldNotTakeSuperclassFieldsIntoAccount() {
        rewriteRun(
          java(
            """
              public class B {
                  protected Integer n2;
                  Integer n3;
                  private Integer n4;
              }
              """
          ),
          java(
            """
              public class A extends B {
                  Integer n;
                  Integer n1;
                          
                  class C {
                      public void method(Integer n) {
                          Integer n1 = n;
                      }
                  }
              }
              """,
            """
              public class A extends B {
                  Integer n;
                  Integer n1;
                          
                  class C {
                      public void method(Integer n2) {
                          Integer n3 = n2;
                      }
                  }
              }
              """
          )
        );
    }
}
