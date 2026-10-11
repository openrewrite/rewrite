/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.OrderImports;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.sbt;
import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Tests that common OpenRewrite Java recipes work correctly on Scala LSTs.
 * Since Scala extends from J, these recipes should work out of the box once
 * the Scala parser produces fully type-attributed ASTs.
 */
class RecipeCompatibilityTest implements RewriteTest {

    // ---- FindTypes ----

    @Test
    void findTypesInFieldDeclaration() {
        rewriteRun(
          spec -> spec.recipe(new FindTypes("java.util.ArrayList", false)),
          scala(
            """
              import java.util.ArrayList

              class MyClass {
                val list = new ArrayList[String]()
              }
              """,
            """
              import java.util.ArrayList

              class MyClass {
                val list = new /*~~>*/ArrayList[String]()
              }
              """
          )
        );
    }

    @Test
    void findTypesWithWildcard() {
        rewriteRun(
          spec -> spec.recipe(new FindTypes("java.util..*", false)),
          scala(
            """
              import java.util.List
              import java.util.ArrayList

              class MyClass {
                val list: List[String] = new ArrayList[String]()
              }
              """,
            """
              import java.util.List
              import java.util.ArrayList

              class MyClass {
                val list: /*~~>*/List[String] = new /*~~>*/ArrayList[String]()
              }
              """
          )
        );
    }

    // ---- FindMethods ----

    @Test
    void findMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("java.util.ArrayList add(..)", false)),
          scala(
            """
              import java.util.ArrayList

              class MyClass {
                val list = new ArrayList[String]()
                list.add("hello")
              }
              """,
            """
              import java.util.ArrayList

              class MyClass {
                val list = new ArrayList[String]()
                /*~~>*/list.add("hello")
              }
              """
          )
        );
    }

    @Test
    void findStaticMethodCall() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("java.util.Collections emptyList()", false)),
          scala(
            """
              import java.util.Collections

              class MyClass {
                val list = Collections.emptyList()
              }
              """,
            """
              import java.util.Collections

              class MyClass {
                val list = /*~~>*/Collections.emptyList()
              }
              """
          )
        );
    }

    // ---- ChangeMethodName ----

    @Test
    void changeMethodNameOnInvocation() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodName("java.util.ArrayList add(..)", "insert", null, null)),
          scala(
            """
              import java.util.ArrayList

              class MyClass {
                val list = new ArrayList[String]()
                list.add("hello")
              }
              """,
            """
              import java.util.ArrayList

              class MyClass {
                val list = new ArrayList[String]()
                list.insert("hello")
              }
              """
          )
        );
    }

    // ---- ChangeType ----

    @Test
    void changeTypeInFieldDeclaration() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", true)),
          scala(
            """
              import java.util.ArrayList

              class MyClass {
                val list = new ArrayList[String]()
              }
              """,
            """
              import  java.util.LinkedList

              class MyClass {
                val list = new LinkedList[String]()
              }
              """
          )
        );
    }

    @Test
    void changeJavaLangType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.lang.Integer", "java.lang.Long", true)),
          scala(
            """
              class MyClass {
                val num: Integer = 1
              }
              """,
            """
              class MyClass {
                val num: Long = 1
              }
              """
          )
        );
    }

    // ---- OrderImports ----

    @Test
    void orderImportsAlphabetically() {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(false, null)),
          scala(
            """
              import java.util.Set
              import java.util.ArrayList
              import java.util.List
              import java.util.Map

              class MyClass {
                val list: List[String] = new ArrayList[String]()
                val set: Set[String] = null
                val map: Map[String, String] = null
              }
              """,
            """
              import java.util.ArrayList
              import java.util.List
              import java.util.Map
              import java.util.Set

              class MyClass {
                val list: List[String] = new ArrayList[String]()
                val set: Set[String] = null
                val map: Map[String, String] = null
              }
              """
          )
        );
    }

    @Test
    void orderImportsSamePackage() {
        // Verify OrderImports sorts within the same package
        rewriteRun(
          spec -> spec.recipe(new OrderImports(false, null)),
          scala(
            """
              import java.util.Set
              import java.util.List
              import java.util.Map

              class MyClass {
                val list: List[String] = null
                val set: Set[String] = null
                val map: Map[String, String] = null
              }
              """,
            """
              import java.util.List
              import java.util.Map
              import java.util.Set

              class MyClass {
                val list: List[String] = null
                val set: Set[String] = null
                val map: Map[String, String] = null
              }
              """
          )
        );
    }

    @Nested
    class ExpressionReplacingStatement implements RewriteTest {

        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitUnary(J.Unary unary, ExecutionContext ctx) {
                    J j = super.visitUnary(unary, ctx);
                    if (j instanceof J.Unary u && u.getOperator() == J.Unary.Type.Not &&
                        u.getExpression() instanceof J.Parentheses<?> parens &&
                        parens.getTree() instanceof J.Binary b && b.getOperator() == J.Binary.Type.Equal) {
                        return b.withOperator(J.Binary.Type.NotEqual).withPrefix(u.getPrefix());
                    }
                    return j;
                }
            }));
        }

        @Test
        void isWrapped() {
            rewriteRun(
              scala(
                """
                  class Demo {
                    def a(a: Int, b: Int): Boolean = !(a == b)

                    def b(a: Int, b: Int): Unit = {
                      !(a == b)
                      if (a > b) !(a == b) else !(b == a)
                      while (a > b) !(a == b)
                    }
                  }
                  """,
                """
                  class Demo {
                    def a(a: Int, b: Int): Boolean = a != b

                    def b(a: Int, b: Int): Unit = {
                      a != b
                      if (a > b) a != b else b != a
                      while (a > b) a != b
                    }
                  }
                  """,
                spec -> spec.afterRecipe(cu -> assertThat(notEqualsWrappedInExpressionStatement(cu))
                  .containsExactly(true, true, true, true, true))
              )
            );
        }

        @Test
        void topLevelIsWrapped() {
            rewriteRun(
              sbt(
                """
                  val a = 1
                  val b = 2
                  !(a == b)
                  """,
                """
                  val a = 1
                  val b = 2
                  a != b
                  """,
                spec -> spec.afterRecipe(cu -> assertThat(notEqualsWrappedInExpressionStatement(cu))
                  .containsExactly(true))
              )
            );
        }

        @Test
        void methodArgumentIsNotWrapped() {
            rewriteRun(
              scala(
                """
                  class Demo {
                    def a(a: Int, b: Int): Unit = println(!(a == b))
                  }
                  """,
                """
                  class Demo {
                    def a(a: Int, b: Int): Unit = println(a != b)
                  }
                  """,
                spec -> spec.afterRecipe(cu -> assertThat(notEqualsWrappedInExpressionStatement(cu))
                  .containsExactly(false))
              )
            );
        }

        private List<Boolean> notEqualsWrappedInExpressionStatement(S.CompilationUnit cu) {
            List<Boolean> wrapped = new ArrayList<>();
            new ScalaIsoVisitor<Integer>() {
                @Override
                public J.Binary visitBinary(J.Binary binary, Integer p) {
                    if (binary.getOperator() == J.Binary.Type.NotEqual) {
                        wrapped.add(getCursor().getParentTreeCursor().getValue() instanceof S.ExpressionStatement);
                    }
                    return super.visitBinary(binary, p);
                }
            }.visit(cu, 0);
            return wrapped;
        }
    }
}
