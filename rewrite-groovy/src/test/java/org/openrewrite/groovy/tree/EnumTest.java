/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class EnumTest implements RewriteTest {

    @Test
    void enumDefinition() {
        rewriteRun(
          groovy(
            """
              enum A {
                  B, C, D
              }
              """
          )
        );
    }

    @Test
    void enumDefinitionUnnecessarilyTerminatedWithSemicolon() {
        rewriteRun(
          groovy(
            """
              enum A {
                  B, C;
              }
              """
          )
        );
    }

    @Test
    void enumDefinitionUnnecessarilyTerminatedWithComma() {
        rewriteRun(
          groovy(
            """
              enum A {
                  B, C,
              }
              """
          )
        );
    }

    @Test
    void innerEnum() {
        rewriteRun(
          groovy(
            """
              class A {
                  enum B {
                      C
                  }
              }
              """
          )
        );
    }

    @Test
    void enumWithAnnotations() {
        rewriteRun(
          groovy(
            """
              enum Test {
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
    void enumWithMethods() {
        rewriteRun(
          groovy(
            """
              enum Test {
                  One, Two;

                  void test() {}
              }
              """
          )
        );
    }

    @ExpectedToFail
    @Test
    void anonymousClassInitializer() {
        rewriteRun(
          groovy(
            """
              enum A {
                  A1(1) {
                      @Deprecated
                      void foo() {}
                  },

                  A2 {
                      @Deprecated
                      void foo() {}
                  };

                  A() {}
                  A(int n) {}
              }
              """
          )
        );
    }

    @Test
    void enumConstructor() {
        rewriteRun(
          groovy(
            """
              enum A {
                  A1;
                  A() {}
              }
              """
          )
        );
    }

    @Test
    void enumConstructorWithStatements() {
        rewriteRun(
          groovy(
            """
              enum A {
                  A1;
                  A() {
                    println "statement"
                    println "statement"
                  }
              }
              """
          )
        );
    }

    @Test
    void enumConstructorWithDynamicallyTypedParam() {
        rewriteRun(
          groovy(
            """
             enum A {
                 A1;
                 A(dynamicVar) {}
              }
             """
          )
        );
    }

    @Test
    void noArguments() {
        rewriteRun(
          groovy(
            """
              enum A {
                  A1, A2();
              }
              """
          )
        );
    }

    @Test
    void enumWithLiteralParameters() {
        rewriteRun(
          groovy(
            """
              enum A {
                  ONE(1, "A"),
                  TWO(2, "B", ")"),
                  THREE(3, $/C/$, 1);

                  A(int n, String s) {
                    this(n, s, "ignore")
                  }
                  A(int n, String s, dynamicVar) {}
              }
              """
          )
        );
    }

    @Test
    void enumWithInvocationParameters() {
        rewriteRun(
          groovy(
            """
              class X {
                static X create() { new X() }
              }

              enum A {
                  ONE(new X()),
                  TWO(X.create())

                  A(X x) {}
              }
              """
          )
        );
    }
}
