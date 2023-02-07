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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"ConstantConditions", "FunctionName", "PointlessBooleanExpression", "StatementWithEmptyBody", "LoopStatementThatDoesntLoop", "InfiniteLoopStatement", "DuplicateCondition"})
class SimplifyConstantIfBranchExecutionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SimplifyConstantIfBranchExecution());
    }

    @Test
    void doNotChangeNonIf() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      boolean b = true;
                      if (!b) {
                          System.out.println("hello");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfTrue() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (true) {
                          System.out.println("hello");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfTrueInParens() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if ((true)) {
                          System.out.println("hello");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfNotFalse() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (!false) {
                          System.out.println("hello");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    @SuppressWarnings("DuplicateCondition")
    void simplifyConstantIfTrueOrTrue() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (true || true) {
                          System.out.println("hello");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfFalse() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (false) {
                          System.out.println("hello");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfTrueElse() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (true) {
                      } else {
                          System.out.println("hello");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfFalseElse() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (false) {
                      } else {
                          System.out.println("hello");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfTrueNoBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (true) System.out.println("hello");
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfFalseNoBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (false) System.out.println("hello");
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfTrueEmptyBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (true) {}
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfFalseEmptyBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (false) {}
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfTrueElseIf() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test(boolean a) {
                      if (true) {
                          System.out.println("hello");
                      } else if (a) {
                          System.out.println("goodbye");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test(boolean a) {
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfFalseElseIf() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test(boolean a) {
                      if (false) {
                          System.out.println("hello");
                      } else if (a) {
                          System.out.println("goodbye");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test(boolean a) {
                      if (a) {
                          System.out.println("goodbye");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfTrueElseIfFalse() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (true) {
                      } else if (false) {
                          System.out.println("hello");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfFalseElseIfTrue() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (false) {
                      } else if (true) {
                          System.out.println("hello");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfTrueElseIfFalseNoBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (true) System.out.println("hello");
                      else if (false) System.out.println("goodbye");
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfFalseElseIfNoBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (false) System.out.println("hello");
                      else if (true) System.out.println("goodbye");
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      System.out.println("goodbye");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfTrueElseIfFalseEmptyBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (true) {}
                      else if (false) {}
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfFalseElseIfTrueEmptyBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (false) {}
                      else if (true) {}
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfVariableElseIfTrueEmptyBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test(boolean a) {
                      if (a) {
                          System.out.println("hello");
                      } else if (true) {
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test(boolean a) {
                      if (a) {
                          System.out.println("hello");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfVariableElseIfTruePrint() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test(boolean a) {
                      if (a) {
                          System.out.println("hello");
                      } else if (true) {
                          System.out.println("goodbye");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test(boolean a) {
                      if (a) {
                          System.out.println("hello");
                      } else {
                          System.out.println("goodbye");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyConstantIfVariableElseIfFalseEmptyBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test(boolean a) {
                      if (a) {
                          System.out.println("hello");
                      } else if (false) {
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test(boolean a) {
                      if (a) {
                          System.out.println("hello");
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Test
    void simplifyConstantIfFalseElseWhileTrueEmptyBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (false) {}
                      else while (true) {
                          System.out.println("hello");
                      }
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      while (true) {
                          System.out.println("hello");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotFormatCodeOutsideRemovedBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      int[] a = new int[] { 1, 2, 3 };
                      int[] b = new int[] {4,5,6};
                      if (true) {
                          System.out.println("hello");
                      }
                      int[] c = new int[] { 1, 2, 3 };
                      int[] d = new int[] {4,5,6};
                  }
              }
              """,
            """
              public class A {
                  public void test() {
                      int[] a = new int[] { 1, 2, 3 };
                      int[] b = new int[] {4,5,6};
                      System.out.println("hello");
                      int[] c = new int[] { 1, 2, 3 };
                      int[] d = new int[] {4,5,6};
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotRemoveWhenReturnInIfBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (true) {
                          System.out.println("hello");
                          return;
                      }
                      System.out.println("goodbye");
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotRemoveWhenThrowsInIfBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      if (true) {
                          System.out.println("hello");
                          throw new RuntimeException();
                      }
                      System.out.println("goodbye");
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotRemoveWhenBreakInIfBlockWithinWhile() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      while (true){
                          if (true) {
                              System.out.println("hello");
                              break;
                          }
                          System.out.println("goodbye");
                      }
                      System.out.println("goodbye");
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotRemoveWhenContinueInIfBlockWithinWhile() {
        rewriteRun(
          java(
            """
              public class A {
                  public void test() {
                      while (true) {
                          if (true) {
                              System.out.println("hello");
                              continue;
                          }
                          System.out.println("goodbye");
                      }
                      System.out.println("goodbye");
                  }
              }
              """
          )
        );
    }

    @Test
    void binaryOrIsAlwaysFalse() {
        rewriteRun(
          java(
            """
              public class A {
                  void test() {
                      if (!true || !true) {
                          throw new RuntimeException();
                      }
                  }
              }
              """,
            """
              public class A {
                  void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void negatedTrueTrue() {
        rewriteRun(
          java(
            """
              public class A {
                  void test() {
                      if (!(true && true)) {
                          throw new RuntimeException();
                      }
                  }
              }
              """,
            """
              public class A {
                  void test() {
                  }
              }
              """
          )
        );
    }
}
