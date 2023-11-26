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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
class ForLoopTest implements RewriteTest {

    @Test
    void forLoopMultipleInit() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      int i;
                      int j;
                      for(i = 0, j = 0;;) {
                      }
                  }
                }
              """
          )
        );
    }

    @Test
    void forLoop() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      for(int i = 0; i < 10; i++) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void infiniteLoop() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      for(;;) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void format() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      for ( int i = 0 ; i < 10 ; i++ ) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void formatInfiniteLoop() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      for ( ; ; ) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void formatLoopNoInit() {
        rewriteRun(
          java(
            """
              class Test {
                  void test(int i) {
                      for ( ; i < 10 ; i++ ) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void formatLoopNoCondition() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      int i = 0;
                      for(; i < 10; i++) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void statementTerminatorForSingleLineForLoops() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      for(;;) test();
                  }
              }
              """
          )
        );
    }

    @Test
    void initializerIsAnAssignment() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                  
                      int[] a;
                      int i=0;
                      for(i=0; i<a.length; i++) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void multiVariableInitialization() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      for(int i, j = 0;;) {}
                  }
              }
              """
          )
        );
    }
}
