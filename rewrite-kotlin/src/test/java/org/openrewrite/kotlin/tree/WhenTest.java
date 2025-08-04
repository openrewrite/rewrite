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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"LiftReturnOrAssignment", "IntroduceWhenSubject"})
class WhenTest implements RewriteTest {

    @Test
    void unaryConditions() {
        rewriteRun(
          kotlin(
            """
              class A {
                val a = 1
                fun method ( ) {
                    val a =   A  ( )
                    val b = a
                }
              }
              """
          )
        );
    }

    @Test
    void binaryConditions() {
        rewriteRun(
          kotlin(
            """
              fun method ( i : Int ) : String {
                  when  {
                      i == 1 || i == 3  ->   return "1"
                      i == 2   ->  return "2"
                      else    ->  {
                          return "42"
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void multiCase() {
        rewriteRun(
          kotlin(
            """
              fun method ( i : Int ) : String {
                  when (  i   ) {
                      1  ,   2    , 3  -> return "1 or 2 or 3"
                      else -> {
                          return "42"
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withContinueInALoop() {
        rewriteRun(
          kotlin(
            """
                  fun method ( ) : String {
                  val list = listOf(1, 2, 3)

                  for (num in list) {
                      when ( num ) {
                          1  -> return "ONE"
                          2 -> return "TWO"
                          3 ->   continue
                          else -> return "42"
                      }
                  }
                  return ""
              }
              """
          )
        );
    }

    @Test
    void twoIsConditions() {
        rewriteRun(
          kotlin(
            """
                fun method ( i : Any ) {
                  when(i) {
                      is Int, is Double -> println("Yeah")
                      else -> {
                          println("42")
                      }
                  }
                }
                """
          )
        );
    }

    @Test
    void inRange() {
        rewriteRun(
          kotlin(
            """
              fun method ( i : Int ) : String {
                  return when ( i ) {
                      in  1 .. 10 -> return "in range 1"
                      !in 10 .. 20  -> return "not in range 2"
                      else -> "42"
                  }
              }
              """
          )
        );
    }

    @Test
    void withOutCondition() {
        rewriteRun(
          kotlin(
            """
              fun method ( i : Int ) : String {
                  when {
                      i . mod ( 2 ) == 0 -> return "even"
                      else -> return "odd"
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/81")
    @Test
    void typeOperatorCondition() {
        rewriteRun(
          kotlin(
            """
              fun method ( i : Any ) : String {
                  when ( i ) {
                      is  Boolean -> return "is"
                      !is   Int -> return "is not"
                      else -> return "42"
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/81")
    @Test
    void typeOperatorWithoutCondition() {
        rewriteRun(
          kotlin(
            """
              fun method ( i : Any ) : String {
                  when {
                      i is Boolean -> return "is"
                      else -> return "is not"
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/86")
    @Test
    void propertyAccessOnWhen() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  val property = false
                  fun method() {
                      when {
                          property -> {
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/86")
    @Test
    void logicalOperatorOnPropertyAccess() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val lhs = true
                  val rhs = true
                  when {
                      lhs && rhs -> {
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/86")
    @Test
    void logicalOperatorOnMixed() {
        rewriteRun(
          kotlin(
            """
              package foo.bar
              import java.util.List
              fun method ( i : Any ) {
                  val lhs = true
                  val rhs = true
                  when ( i ) {
                      1, ( lhs && rhs || isTrue() ) -> {
                      }
                  }
              }
              fun isTrue ( ) = true
              """
          )
        );
    }

    @SuppressWarnings("All")
    @Test
    void branchArrowToLiteral() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val condition: Int = 11
                  when {
                      condition < 20   ->    'c'
                      condition < 10   ->    2
                      condition > 10   ->    (true)
                      else             ->    0.9
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                K.When when = (K.When)((J.MethodDeclaration)(cu.getStatements().getFirst())).getBody().getStatements().get(1);
                boolean allBranchesHasLiteralBody = when.getBranches().getStatements().stream().map(K.WhenBranch.class::cast).allMatch(
                  branch -> branch.getBody() instanceof J.Literal ||
                            branch.getBody() instanceof J.Parentheses<?> && ((J.Parentheses<?>) branch.getBody()).getTree() instanceof J.Literal
                );
                assertThat(allBranchesHasLiteralBody).isTrue();
            })
          )
        );
    }

    @Test
    void trailingComma() {
        rewriteRun(
          kotlin(
            """
              fun isReferenceApplicable(myReference: kotlin.reflect.KClass<*>) = when (myReference) {
                  Comparable::class,
                  Iterable::class,
                  String::class   ,   // trailing comma
                      -> true
                  else -> false
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/240")
    @Test
    void subjectVariable() {
        rewriteRun(
          kotlin(
            """
              val x = when (val response = listOf(1, 2) as Collection<Int>) {
                  is List -> "l"
                  else -> ""
              }
              """,
              spec -> spec.afterRecipe(cu -> {
                  assertThat(cu.getStatements()).satisfiesExactly(
                      stmt -> {
                          J.VariableDeclarations x = (J.VariableDeclarations) stmt;
                          K.When initializer = (K.When) x.getVariables().getFirst().getInitializer();
                          assertThat(initializer.getSelector().getTree()).isInstanceOf(J.VariableDeclarations.class);
                      }
                  );
              })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/436")
    @Test
    void trailingSemiColonOnWhenBranch() {
        rewriteRun(
          kotlin(
            """
              fun test(arg: Any) {
                  when (arg) {
                      is String -> "1" ;
                      is Int -> "2"  /*C1*/ ;
                      else -> "3"  ;
                  }
              }
              """
          )
        );
    }
}
