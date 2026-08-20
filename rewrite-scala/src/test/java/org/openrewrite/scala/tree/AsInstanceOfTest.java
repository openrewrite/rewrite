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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class AsInstanceOfTest implements RewriteTest {

    @Test
    void simpleCast() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = "hello"
                val str = obj.asInstanceOf[String]
              }
              """
          )
        );
    }

    @Test
    void castWithMethodCall() {
        rewriteRun(
          scala(
            """
              object Test {
                def getValue(): Any = 42
                val num = getValue().asInstanceOf[Int]
              }
              """
          )
        );
    }

    @Test
    void castInExpression() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = 10
                val result = obj.asInstanceOf[Int] + 5
              }
              """
          )
        );
    }

    @Test
    void castToParameterizedType() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = List(1, 2, 3)
                val list = obj.asInstanceOf[List[Int]]
              }
              """
          )
        );
    }

    @Test
    void nestedCasts() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = "42"
                val num = obj.asInstanceOf[String].toInt
              }
              """
          )
        );
    }

    @Test
    void castInIfCondition() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = true
                if (obj.asInstanceOf[Boolean]) {
                  println("It's true!")
                }
              }
              """
          )
        );
    }

    @Test
    void castWithParentheses() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = 42
                val result = (obj.asInstanceOf[Int]) * 2
              }
              """
          )
        );
    }

    @Test
    void castChain() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = "test"
                val upper = obj.asInstanceOf[String].toUpperCase.asInstanceOf[CharSequence]
              }
              """
          )
        );
    }

    @Test
    void chainedAsInstanceOfOnNewLine() {
        rewriteRun(
          scala(
            """
            val hudiSchemaAsStruct = something.dataType
              .asInstanceOf[StructType]
            """
          )
        );
    }

    @Test
    void castAsFirstOfMultipleArguments() {
        rewriteRun(
          scala(
            """
            object Test {
              def getValue(acc: CountAccumulator): MyPojo = {
                new MyPojo(acc.f0.asInstanceOf[Int], acc.f0.asInstanceOf[Int])
              }
            }
            """
          )
        );
    }

    @Test
    void castToFunctionType() {
        rewriteRun(
          scala(
            """
            val f = x.asInstanceOf[A => B]
            """
          )
        );
    }

    @Test
    void newlineBetweenDotAndKeyword() {
        rewriteRun(
          scala(
            """
            object Test {
              val obj: Any = 1
              val num = obj.
                asInstanceOf[Int]
            }
            """
          )
        );
    }

    @Test
    void isAMethodInvocation() {
        rewriteRun(
          scala(
            """
            object Test {
              val obj: Any = 1
              val num = obj.asInstanceOf[Int]
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration test = (J.ClassDeclaration) cu.getStatements().get(0);
                J.VariableDeclarations num = (J.VariableDeclarations) test.getBody().getStatements().get(1);
                J.MethodInvocation cast = (J.MethodInvocation) num.getVariables().get(0).getInitializer();
                assertThat(cast.getSimpleName()).isEqualTo("asInstanceOf");
                assertThat(cast.getArguments()).isEmpty();
                assertThat(cast.getTypeParameters()).singleElement()
                  .isInstanceOfSatisfying(J.Identifier.class, t -> assertThat(t.getSimpleName()).isEqualTo("Int"));
            })
          )
        );
    }

}
