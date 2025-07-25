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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ChangeMethodNameTest implements RewriteTest {
    @Language("java")
    String b = """
      package com.abc;
      class B {
         public void singleArg(String s) {}
         public void arrArg(String[] s) {}
         public void varargArg(String... s) {}
         public static void static1(String s) {}
         public static void static2(String s) {}
      }
      """;

    @DocumentExample
    @Test
    void changeMethodNameForMethodWithSingleArg() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.B singleArg(String)", "bar", null, null)),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              class A {
                 public void test() {
                     new B().singleArg("boo");
                 }
              }
              """,
            """
              package com.abc;
              class A {
                 public void test() {
                     new B().bar("boo");
                 }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/605")
    @SuppressWarnings({"ConstantConditions", "RedundantOperationOnEmptyContainer"})
    @Test
    void changeMethodNameForOverriddenMethod() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.B singleArg(String)", "bar", true, null)),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              class C extends B {
                  @Override
                  public void singleArg(String s) {}
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package com.abc;
              class A {
                  public void test() {
                      new C().singleArg("boo");
                      new java.util.ArrayList<String>().forEach(new C()::singleArg);
                  }
              }
              """,
            """
              package com.abc;
              class A {
                  public void test() {
                      new C().bar("boo");
                      new java.util.ArrayList<String>().forEach(new C()::bar);
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration testMethodDecl = (J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst();
                List<Statement> statements = testMethodDecl.getBody().getStatements();
                J.MethodInvocation barInvocation = (J.MethodInvocation) statements.getFirst();
                assertThat(barInvocation.getName().getSimpleName()).isEqualTo("bar");
                assertThat(barInvocation.getMethodType().getName()).isEqualTo("bar");
                J.MemberReference barReference = (J.MemberReference) ((J.MethodInvocation) statements.get(1)).getArguments().getFirst();
                JavaType.Method barRefType = barReference.getMethodType();
                assertThat(barRefType.getName()).isEqualTo("bar");
            })
          )
        );
    }

    @Test
    void changeMethodNameForOverriddenMethodAnonymousClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.B singleArg(String)", "bar", true, null)),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              class A {
                   public void test() {
                       new B() {
                           @Override
                           public void singleArg(String s) {}
                       };
                   }
              }
              """,
            """
              package com.abc;
              class A {
                   public void test() {
                       new B() {
                           @Override
                           public void bar(String s) {}
                       };
                   }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1215")
    @SuppressWarnings("MethodMayBeStatic")
    @Test
    void changeMethodNameForOverriddenMethodMatchOverridesFalse() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.Parent method(String)", "changed", false, null)),
          java(
            """
              package com.abc;
              class Parent {
                  public void method(String s) {
                  }
              }
              class Test extends Parent {
                  @Override
                  public void method(String s) {
                  }
              }
              """,
            """
              package com.abc;
              class Parent {
                  public void changed(String s) {
                  }
              }
              class Test extends Parent {
                  @Override
                  public void method(String s) {
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("RedundantOperationOnEmptyContainer")
    @Test
    void changeMethodNameForMethodWithSingleArgDeclarative() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.B singleArg(String)", "bar", null, null)),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              class A {
                  public void test() {
                      new B().singleArg("boo");
                      new java.util.ArrayList<String>().forEach(new B()::singleArg);
                  }
              }
              """,
            """
              package com.abc;
              class A {
                  public void test() {
                      new B().bar("boo");
                      new java.util.ArrayList<String>().forEach(new B()::bar);
                  }
              }
              """
          )
        );
    }

    @Test
    void changeMethodNameForMethodWithArrayArg() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.B arrArg(String[])", "bar", null, null)),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              class A {
                 public void test() {
                     new B().arrArg(new String[] {"boo"});
                 }
              }
              """,
            """
              package com.abc;
              class A {
                 public void test() {
                     new B().bar(new String[] {"boo"});
                 }
              }
              """
          )
        );
    }

    @Test
    void changeMethodNameForMethodWithVarargArg() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.B varargArg(String...)", "bar", null, null)),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              class A {
                 public void test() {
                     new B().varargArg("boo", "again");
                 }
              }
              """,
            """
              package com.abc;
              class A {
                 public void test() {
                     new B().bar("boo", "again");
                 }
              }
              """
          )
        );
    }

    @Test
    void changeMethodNameWhenMatchingAgainstMethodWithNameThatIsAnAspectjToken() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.B error()", "foo", null, null)),
          java(
            """
              package com.abc;
              class B {
                 public void error() {}
                 public void foo() {}
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package com.abc;
              class A {
                 public void test() {
                     new B().error();
                 }
              }
              """,
            """
              package com.abc;
              class A {
                 public void test() {
                     new B().foo();
                 }
              }
              """
          )
        );
    }

    @Test
    void changeMethodDeclarationForMethodWithSingleArg() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.A foo(String)", "bar", null, null)),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              class A {
                 public void foo(String s) {
                 }
              }
              """,
            """
              package com.abc;
              class A {
                 public void bar(String s) {
                 }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeMethodNameOnDeclaringClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.A foo(String)", "bar", null, true)),
          java(
            """
              package com.abc;
              class A {
                 public void foo(String s) {
                 }
              }
              """
          )
        );
    }

    @Test
    void changeStaticMethodTest() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.B static1(String)", "static2", null, null)),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              class A {
                 public void test() {
                     B.static1("boo");
                 }
              }
              """,
            """
              package com.abc;
              class A {
                 public void test() {
                     B.static2("boo");
                 }
              }
              """
          )
        );
    }

    @Test
    void changeStaticImportTest() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.B static1(String)", "static2", null, null)),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              import static com.abc.B.static1;
              class A {
                 public void test() {
                     static1("boo");
                 }
              }
              """,
            """
              package com.abc;
              import static com.abc.B.static2;
              class A {
                 public void test() {
                     static2("boo");
                 }
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedStaticMemberReference() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("com.abc.B static1(String)", "static2", null, null)),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              class A {
                 public void test() {
                     com.abc.B.static1("boo");
                 }
              }
              """,
            """
              package com.abc;
              class A {
                 public void test() {
                     com.abc.B.static2("boo");
                 }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "  ", "\t", "this", "class", "null", "true"})
    void ignoreInvalidMethodNamesWhenCalledDownstream(String newMethodName) {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J visit(Tree tree, ExecutionContext ctx) {
                  return (J) new ChangeMethodName("com.abc.B static1(String)", newMethodName, null, null).getVisitor().visitNonNull(tree, ctx);
              }
          })),
          java(b, SourceSpec::skip),
          java(
            """
              package com.abc;
              
              import java.util.ArrayList;
              
              class A {
                 public void test() {
                     com.abc.B.static1("boo");
                     new java.util.ArrayList<String>().forEach(B::static1);
                 }
              }
              """
          )
        );
    }

    @Test
    void validation() {
        Validated<Object> validObject = new ChangeMethodName("a.Clazz method(..)", "method2", null, null).validate();
        assertThat(validObject.isValid()).isTrue();
    }

    @Test
    void validateReservedKeyword() {
        Validated<Object> reservedKeyword = new ChangeMethodName("a.Clazz method(..)", "this", null, null).validate();
        assertThat(reservedKeyword.isValid()).isFalse();
        assertThat(reservedKeyword.failures()).singleElement()
          .matches(f -> f.getProperty().equals("newMethodName"))
          .matches(f -> f.getInvalidValue().equals("this"))
          .matches(f -> f.getMessage().equals("should not be a Java Reserved Keyword."));
    }

    @Test
    void validateReservedLiteral() {
        Validated<Object> reservedLiteral = new ChangeMethodName("a.Clazz method(..)", "null", null, null).validate();
        assertThat(reservedLiteral.isValid()).isFalse();
        assertThat(reservedLiteral.failures()).singleElement()
          .matches(f -> f.getProperty().equals("newMethodName"))
          .matches(f -> f.getInvalidValue().equals("null"))
          .matches(f -> f.getMessage().equals("should not be a Java Reserved Literal."));
    }

    @Test
    void validatePattern() {
        Validated<Object> invalidPattern = new ChangeMethodName("a.Clazz method(..)", "123", null, null).validate();
        assertThat(invalidPattern.isValid()).isFalse();
        assertThat(invalidPattern.failures()).singleElement()
          .matches(f -> f.getProperty().equals("newMethodName"))
          .matches(f -> f.getInvalidValue().equals("123"))
          .matches(f -> f.getMessage().equals("should be a valid Java method name."));
    }
}
