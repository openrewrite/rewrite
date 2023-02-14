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

package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({
  "ClassInitializerMayBeStatic", "StatementWithEmptyBody", "ConstantConditions",
  "SynchronizationOnLocalVariableOrMethodParameter", "CatchMayIgnoreException", "EmptyFinallyBlock",
  "InfiniteLoopStatement", "UnnecessaryContinue", "EmptyClassInitializer", "EmptyTryBlock",
  "resource"
})
class EmptyBlockTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new EmptyBlock());
    }

    @SuppressWarnings("ClassInitializerMayBeStatic")
    @Test
    void emptySwitch() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      int i = 0;
                      switch(i) {
                      }
                  }
              }
              """,
            """
              public class A {
                  {
                      int i = 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyBlockWithComment() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      // comment
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("EmptySynchronizedStatement")
    @Test
    void emptySynchronized() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      final Object o = new Object();
                      synchronized(o) {
                      }
                  }
              }
              """,
            """
              public class A {
                  {
                      final Object o = new Object();
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyTry() {
        rewriteRun(
          java(
            """
              import java.io.*;

              public class A {
                  {
                      final String fileName = "fileName";
                      try(FileInputStream fis = new FileInputStream(fileName)) {
                      } catch (IOException e) {
                      }
                  }
              }
              """,
            """
              public class A {
                  {
                      final String fileName = "fileName";
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyCatchBlockWithIOException() {
        rewriteRun(
          java(
            """
              import java.io.FileInputStream;
              import java.io.IOException;
              import java.nio.file.*;

              public class A {
                  public void foo() {
                      try {
                          new FileInputStream(new File("somewhere"));
                      } catch (IOException e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyCatchBlockWithExceptionAndEmptyFinally() {
        rewriteRun(
          java(
            """
              import java.io.File;
              import java.io.FileInputStream;
              import java.nio.file.*;

              public class A {
                  public void foo() {
                      try {
                          new FileInputStream(new File("somewhere"));
                      } catch (Throwable t) {
                      } finally {
                      }
                  }
              }
              """,
            """
              import java.io.File;
              import java.io.FileInputStream;
              import java.nio.file.*;

              public class A {
                  public void foo() {
                      try {
                          new FileInputStream(new File("somewhere"));
                      } catch (Throwable t) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyLoops() {
        rewriteRun(
          java(
            """
              public class A {
                  public void foo() {
                      while(true) {
                      }
                      do {
                      } while(true);
                  }
              }
              """,
            """
              public class A {
                  public void foo() {
                      while(true) {
                          continue;
                      }
                      do {
                          continue;
                      } while(true);
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyInstanceAndStaticInit() {
        rewriteRun(
          java(
            """
              public class A {
                  static {}
                  {}
              }
              """,
            """
              public class A {
              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void extractSideEffectsFromEmptyIfsWithNoElse() {
        rewriteRun(
          java(
            """
              public class A {
                  int n = sideEffect();

                  int sideEffect() {
                      return new java.util.Random().nextInt();
                  }

                  boolean boolSideEffect() {
                      return sideEffect() == 0;
                  }

                  public void lotsOfIfs() {
                      if(sideEffect() == 1) {}
                      if(sideEffect() == sideEffect()) {}
                      int n;
                      if((n = sideEffect()) == 1) {}
                      if((n /= sideEffect()) == 1) {}
                      if(new A().n == 1) {}
                      if(!boolSideEffect()) {}
                      if(1 == 2) {}
                  }
              }
              """,
            """
              public class A {
                  int n = sideEffect();

                  int sideEffect() {
                      return new java.util.Random().nextInt();
                  }

                  boolean boolSideEffect() {
                      return sideEffect() == 0;
                  }

                  public void lotsOfIfs() {
                      sideEffect();
                      sideEffect();
                      sideEffect();
                      int n;
                      n = sideEffect();
                      n /= sideEffect();
                      new A();
                      boolSideEffect();
                  }
              }
              """
          )
        );
    }

    @Test
    void invertIfWithOnlyElseClauseAndBinaryOperator() {
        rewriteRun(
          // extra spaces after the original if condition to ensure that we preserve the if statement's block formatting
          java(
            """
              public class A {
                  {
                      if("foo".length() > 3)   {
                      } else {
                          System.out.println("this");
                      }
                  }
              }
              """,
            """
              public class A {
                  {
                      if("foo".length() <= 3)   {
                          System.out.println("this");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void invertIfWithElseIfElseClause() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      if("foo".length() > 3) {
                      } else if("foo".length() > 4) {
                          System.out.println("longer");
                      }
                      else {
                          System.out.println("this");
                      }
                  }
              }
              """,
            """
              public class A {
                  {
                      if("foo".length() <= 3) {
                          if("foo".length() > 4) {
                              System.out.println("longer");
                          }
                          else {
                              System.out.println("this");
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyElseBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      if (true) {
                          System.out.println("this");
                      } else {
                      }
                  }
              }
              """,
            """
              public class A {
                  {
                      if (true) {
                          System.out.println("this");
                      }
                  }
              }
              """
          )
        );
    }
}
