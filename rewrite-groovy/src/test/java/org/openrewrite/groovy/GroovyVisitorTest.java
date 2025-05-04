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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class GroovyVisitorTest implements RewriteTest {

    @DocumentExample
    @Test
    void autoFormatIncludesOmitParentheses() {
        rewriteRun(
          spec -> spec
            .recipeExecutionContext(new InMemoryExecutionContext().addObserver(new TreeObserver.Subscription(new TreeObserver() {
                @Override
                public Tree treeChanged(Cursor cursor, Tree newTree) {
                    return newTree;
                }
            }).subscribeToType(J.Lambda.class)))
            .recipe(RewriteTest.toRecipe(() -> new GroovyVisitor<>() {
                @Override
                public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                    return autoFormat(super.visitCompilationUnit(cu, ctx), ctx);
                }
            })),
          groovy(
            """
              Test.test({ it })
              """,
            """
              Test.test {
                  it
              }
              """
          )
        );
    }
  
    @Test
    void newArrayWithSize() {
        rewriteRun(groovy(
          """
          class A {
              static void main(String[] argv) {
                  int[][][] addr = new int[4+3][3][5];
              }
          }
          """
        ));
    }

    @Test
    void newArrayWithEmptyInitializer() {
        rewriteRun(groovy(
          """
          class A {
              static void main(String[] argv) {int[] addr = new int[]{}}
          }
          """
        ));
    }

    @Test
    void newArrayOfListOfStrings() {
        rewriteRun(groovy(
          """
          class A {
              static void main(String[] argv) {
                  List<String>[] l = new List<String>[]{ new ArrayList<String>() };
              }
          }
          """
        ));
    }

    @Test
    void newArrayWithInitializer() {
        rewriteRun(groovy(
          """
          class A {
              static void main(String[] argv) {
                  int[] addr = new int[] {
                      123,
                      new Integer(456).intValue()
                  };
              }
          }
          """
        ));
    }

    @Test
    void dynamicallyTypedNewArrayWithSize() {
        rewriteRun(groovy(
          """
          class A {
              static void main(String[] argv) {
                  def addr = new int[4+3][3][5];
              }
          }
          """
        ));
    }

    @Test
    void returnNewArray() {
        rewriteRun(groovy(
          """
            class TestMe{
              String[] getArgs() {
                return new String[0]
              }
            }
            """
        ));
    }
  
    @Test
    void spreadOperator() {
        rewriteRun(groovy(
          """
            class A {
                static void main(String[] argv) {
                    def l = [1,2,3]
                    System.out.printf("%d, %d, %d", *l);
                }
            }
            """
        ));
    }
}
