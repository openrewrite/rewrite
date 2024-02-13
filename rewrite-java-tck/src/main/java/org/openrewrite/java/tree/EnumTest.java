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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class EnumTest implements RewriteTest {

    @SuppressWarnings("UnnecessarySemicolon")
    @Issue("https://github.com/openrewrite/rewrite/issues/379")
    @Test
    void enumWithAnnotations() {
        rewriteRun(
          java(
            """
              public enum Test {
                  @Deprecated(since = "now")
                  One,
                  
                  @Deprecated(since = "now")
                  Two;
              }
              """
          )
        );
    }

    @Test
    void anonymousClassInitializer() {
        rewriteRun(
          java(
            """
              public enum A {
                  A1(1) {
                      @Override
                      void foo() {}
                  };

                  A(int n) {}
                  
                  abstract void foo();
              }
              """
          )
        );
    }

    @Test
    void anonymousClassInitializerNoParentheses() {
        rewriteRun(
          java(
            """
              public enum A {
                  A2 {
                      @Override
                      void foo() {}
                  };
                  
                  abstract void foo();
              }
              """
          )
        );
    }

    @Test
    void enumConstructor() {
        rewriteRun(
          java(
            """
              public class Outer {
                  public enum A {
                      A1(1);
                            
                      A(int n) {}
                  }
                  
                  private static final class ContextFailedToStart {
                      private static Object[] combineArguments(String context, Throwable ex, Object[] arguments) {
                          return new Object[arguments.length + 2];
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void noArguments() {
        rewriteRun(
          java(
            """
              public enum A {
                  A1, A2();
              }
              """
          )
        );
    }

    @Test
    void enumWithParameters() {
        rewriteRun(
          java(
            """
              public enum A {
                  ONE(1),
                  TWO(2);
                            
                  A(int n) {}
              }
              """
          )
        );
    }

    @Test
    void enumWithoutParameters() {
        rewriteRun(
          java("public enum A { ONE, TWO }")
        );
    }

    @SuppressWarnings("UnnecessarySemicolon")
    @Test
    void enumUnnecessarilyTerminatedWithSemicolon() {
        rewriteRun(
          java("public enum A { ONE ; }")
        );
    }

    @Test
    @Disabled("enum A { ONE~~(non-whitespace)~~>, <~~}")
    void enumValuesTerminatedWithComma() {
        rewriteRun(
          java("enum A { ONE, }")
        );
    }

    @Test
    void enumWithEmptyParameters() {
        rewriteRun(
          java("public enum A { ONE ( ), TWO ( ) }")
        );
    }
}
