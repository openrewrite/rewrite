/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.java.Assertions.java;

class BlockTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package testing;
              public class TestMethods {
                  public static void remove() {}
                  public static void keep() {}
                  public static void initialize() {}
                  public static void processItems() {}
                  public static void doSomething() {}
                  public static boolean condition() { return true; }
                  public static boolean anotherCondition() { return true; }
                  public static void rename() {}
                  public static void renamed() {}
              }
              """
          ))
          .recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  Block trait = new Block(getCursor())
                    .filterStatements(statement -> {
                        if (statement instanceof J.MethodInvocation) {
                            return !new MethodMatcher("testing.TestMethods remove(..)").matches((MethodCall) statement);
                        }
                        return true;
                    })
                    .mapStatements(statement -> {
                        if (statement instanceof J.MethodInvocation && new MethodMatcher("testing.TestMethods rename(..)").matches((J.MethodInvocation) statement)) {
                            return ((J.MethodInvocation) statement).withName(((J.MethodInvocation) statement).getName().withSimpleName("renamed"));
                        }
                        return statement;
                    });
                  return super.visitBlock(trait.getTree(), ctx);
              }
          }));


    }

    @DocumentExample
    @Test
    void blockWithStatements() {
        rewriteRun(
          java(
            """
              import testing.TestMethods;
              class Test {
                  void method() {
                      //First comment remains
                      TestMethods.remove();
                      TestMethods.keep(); //Comment at the end of a statement remains
                      TestMethods.keep();         //Even if they are indented with more than a single space
                      TestMethods.remove();//Comment at end of removed ones not put at end of previous one.
                      //Comment in between 2 removed ones also remains
                      TestMethods.remove(); //Comment at end of removed ones removed
                      TestMethods.keep();
                      //Comment before removed ones also remain
                      TestMethods.remove();
                      TestMethods.keep(); /*
                          Multiline comments belong to the line where they where added
                      */
                      TestMethods.remove(); /*
                          So this one gets removed
                      */
                      TestMethods.remove();
                      /*
                          But this one does not
                      */
              
                      //Section 1
                      TestMethods.remove();
                      TestMethods.keep();
              
                      //Section 2
                      TestMethods.keep();
              
              
                      //Section3
                      TestMethods.keep();
                      //comment will also be at end
                      TestMethods.remove();
                      //comment at end
                  }
              }
              """,
            """
              import testing.TestMethods;
              class Test {
                  void method() {
                      //First comment remains
                      TestMethods.keep(); //Comment at the end of a statement remains
                      TestMethods.keep();         //Even if they are indented with more than a single space
                      //Comment in between 2 removed ones also remains
                      TestMethods.keep();
                      //Comment before removed ones also remain
                      TestMethods.keep(); /*
                          Multiline comments belong to the line where they where added
                      */
                      /*
                          But this one does not
                      */
              
                      //Section 1
                      TestMethods.keep();
              
                      //Section 2
                      TestMethods.keep();
              
              
                      //Section3
                      TestMethods.keep();
                      //comment will also be at end
                      //comment at end
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyBlockIsRemoved() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      if (true) {
                          remove();
                          remove();
                      }
                      System.out.println("kept");
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      if (true) {
                      }
                      System.out.println("kept");
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyBlockWithCommentsIsPreserved() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      if (true) {
                          // This comment should preserve the block
                          remove();
                      }
                      System.out.println("kept");
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      if (true) {
                          // This comment should preserve the block
                      }
                      System.out.println("kept");
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedBlocksArePreserved() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      remove();
              
                      if (condition()) {
                          doSomething();
                      } // end of if comment
                      // after if comment
              
                      remove();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
              
                      if (condition()) {
                          doSomething();
                      } // end of if comment
                      // after if comment
                  }
              }
              """
          )
        );
    }

    @Test
    void differentCommentLocations() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep(); // end of line comment
                      /* inline block */ remove();
                      /*
                       * Multiline block comment
                       * before a statement
                       */
                      remove();
                      keep();
                      // End of line comment after last kept statement
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep(); // end of line comment
                      /*
                       * Multiline block comment
                       * before a statement
                       */
                      keep();
                      // End of line comment after last kept statement
                  }
              }
              """
          )
        );
    }

    @Test
    void removeFirstStatement() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment
                      remove();
              
                      // Second kept statement
                      keep();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment
              
                      // Second kept statement
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeMiddleStatement() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment
                      keep();
              
                      // Middle statement removed
                      remove();
              
                      // Another kept statement
                      keep();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment
                      keep();
              
                      // Middle statement removed
              
                      // Another kept statement
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeLastStatement() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment kept
                      keep();
              
                      // Second statement removed
                      remove();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment kept
                      keep();
              
                      // Second statement removed
                  }
              }
              """
          )
        );
    }

    @Test
    void removeFirstStatementWithEOLComment() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment
                      remove(); // End of line comment
              
                      // Second kept statement
                      keep();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment
              
                      // Second kept statement
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeMiddleStatementWithEOLComment() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment
                      keep();
              
                      // Middle statement removed
                      remove(); // End of line comment
              
                      // Another kept statement
                      keep();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment
                      keep();
              
                      // Middle statement removed
              
                      // Another kept statement
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeLastStatementWithEOLComment() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment kept
                      keep();
              
                      // Second statement removed
                      remove(); // End of line comment
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // First statement comment kept
                      keep();
              
                      // Second statement removed
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedFirstStatement() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      remove();
                      keep();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedMiddleStatement() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                      remove();
                      keep();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedLastStatement() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                      remove();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedFirstStatementWithEOLComment() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      remove(); // End of line comment
                      keep();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedMiddleStatementWithEOLComment() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                      remove(); // End of line comment
                      keep();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedLastStatementWithEOLComment() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                      remove(); // End of line comment
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeAllButOneStatement() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      remove();
                      remove();
                      keep();
                      remove();
                      remove();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void commentedOutStatements() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      remove();
                      // keep(); // commented out
                      keep();
                      /* keep(); // also commented */
                      remove();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // keep(); // commented out
                      keep();
                      /* keep(); // also commented */
                  }
              }
              """
          )
        );
    }

    @Test
    void commentInBetweenRemovedOnesRemains() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                      remove();//Comment at end of removed ones not put at end of previous one.
                      //Comment in between 2 removed ones remains
                      remove();
                      keep();
              
                      keep(); //Also if the kept item has a end of line
                      remove();//Comment at end of removed ones not put at end of previous one.
                      //Comment in between 2 removed ones remains
                      remove();
                      keep();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                      //Comment in between 2 removed ones remains
                      keep();
              
                      keep(); //Also if the kept item has a end of line
                      //Comment in between 2 removed ones remains
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void multilineComments() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      /* First comment remains */
                      keep();
                      /* before line comment remains */ keep();
                      keep(); /* Comment at the end of a statement remains */
                      keep();         /* Even if they are indented with more than a single space */
                      keep(); /*
                      Comment at end over multiple lines
                      */
                      keep();
                      /*
                      Comment at new line
                       */
                       /* First comment remains */
                      remove();
                      /* before line comment gets removed also */ remove(); /* end of line gets removed */
                      /* even gets removed if preceded by another comment not on a new line */ remove();
                      remove(); /* Comment at the end of a statement is removed */
                      remove();         /* Even removed if they are indented with more than a single space */
                      remove(); /*
                      Comment at end over multiple lines is removed
                      */
                      remove();
                      /*
                      Comment at new line
                       */
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      /* First comment remains */
                      keep();
                      /* before line comment remains */ keep();
                      keep(); /* Comment at the end of a statement remains */
                      keep();         /* Even if they are indented with more than a single space */
                      keep(); /*
                      Comment at end over multiple lines
                      */
                      keep();
                      /*
                      Comment at new line
                       */
                       /* First comment remains */
                      /*
                      Comment at new line
                       */
                  }
              }
              """
          )
        );
    }

    @Test
    void filterStatements() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      remove();
                      keep();
                      remove();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      keep();
                  }
              }
              """
          )
        );
    }

    @Test
    void returnStatementHasExpression() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  int method() {
                      remove();
                      keep();
                      return 42;
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  int method() {
                      keep();
                      return 42;
                  }
              }
              """
          )
        );
    }

    @Test
    void returnStatementHasRemovedStatement() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  Block trait = new Block(getCursor()).filterStatements(statement -> {
                      if (statement instanceof J.MethodInvocation) {
                          return !new MethodMatcher("testing.TestMethods condition(..)").matches((MethodCall) statement);
                      }
                      return true;
                  });
                  return super.visitBlock(trait.getTree(), ctx);
              }
          })),
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  boolean method() {
                      keep();
                      anotherCondition();
                      return condition();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  boolean method() {
                      keep();
                      return anotherCondition();
                  }
              }
              """
          )
        );
    }

    @Test
    void filterStatementsWithCustomReturnMapperReplacingWithOriginal() {
        AssertionError assertionError = assertThrows(AssertionError.class, () ->
          rewriteRun(
            spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                    Block trait = new Block(getCursor()).filterStatements(
                      statement -> {
                          if (statement instanceof J.MethodInvocation) {
                              return !new MethodMatcher("testing.TestMethods condition(..)").matches((MethodCall) statement);
                          }
                          return true;
                      },
                      (returnStmt, expr) -> {
                          // Custom mapper that keeps the original return statement unchanged
                          return returnStmt;
                      }
                    );
                    return super.visitBlock(trait.getTree(), ctx);
                }
            })),
            java(
              """
                import static testing.TestMethods.*;
                class Test {
                    boolean method() {
                        keep();
                        anotherCondition();
                        return condition();
                    }
                }
                """
            )
          )
        );
        assertThat(assertionError.getCause()).isInstanceOf(RecipeRunException.class);
        assertThat(assertionError.getCause().getMessage()).isEqualTo("java.lang.IllegalArgumentException: The return statement replacement result should not be one that gets filtered out to avoid cyclic changes that result in the entire block being cleared. Did you return something from the old return that still return null when the mapper would be applied?");
    }

    @Test
    void mapStatementsWithCustomReturnMapperReplacingStatement() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  Block trait = new Block(getCursor()).mapStatements(
                    statement -> {
                        if (statement instanceof J.MethodInvocation) {
                            J.MethodInvocation mi = (J.MethodInvocation) statement;
                            if (new MethodMatcher("testing.TestMethods condition(..)").matches(mi)) {
                                return null;
                            }
                        }
                        return statement;
                    },
                    (returnStmt, expr) -> {
                        // Custom mapper that wraps the expression in a NOT operation
                        J.Unary notExpr = new J.Unary(
                          Tree.randomId(),
                          Space.SINGLE_SPACE,
                          Markers.EMPTY,
                          JLeftPadded.build(J.Unary.Type.Not),
                          expr.withPrefix(Space.EMPTY),
                          null
                        );
                        return returnStmt.withExpression(notExpr);
                    }
                  );
                  return super.visitBlock(trait.getTree(), ctx);
              }
          })),
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  boolean method() {
                      keep();
                      anotherCondition();
                      return condition();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  boolean method() {
                      keep();
                      return !anotherCondition();
                  }
              }
              """
          )
        );
    }

    @Test
    void complexNestedStructure() {
        rewriteRun(
          java(
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // Outer comment
                      if (true) {
                          // Inner comment before removed
                          remove();
                          // Between statements
                          for (int i = 0; i < 10; i++) {
                              doSomething();
                          }
                          // After for loop
                          remove();
                      }
                      // After if
                      keep();
                  }
              }
              """,
            """
              import static testing.TestMethods.*;
              class Test {
                  void method() {
                      // Outer comment
                      if (true) {
                          // Inner comment before removed
                          // Between statements
                          for (int i = 0; i < 10; i++) {
                              doSomething();
                          }
                          // After for loop
                      }
                      // After if
                      keep();
                  }
              }
              """
          )
        );
    }
}
