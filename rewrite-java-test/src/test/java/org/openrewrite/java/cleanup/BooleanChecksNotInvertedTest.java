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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("DoubleNegation")
class BooleanChecksNotInvertedTest implements RewriteTest {

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void rspec1940() {
        rewriteRun(
          spec -> spec.recipe(new BooleanChecksNotInverted()),
          java(
            """
                  public class Test {
                      int i;
                      int a;
                      void test() {
                          if ( !(a == 2)) {
                          }
                          boolean b = !(i < 10);
                      }
                  }
              """,
            """
                  public class Test {
                      int i;
                      int a;
                      void test() {
                          if ( a != 2) {
                          }
                          boolean b = i >= 10;
                      }
                  }
              """
          )
        );
    }

    @Test
    void doubleNegation() {
        rewriteRun(
          spec -> spec.recipe(new BooleanChecksNotInverted()),
          java(
            """
                  public class Test {
                      boolean test(boolean b) {
                          return !(!b);
                      }
                  }
              """,
            """
                  public class Test {
                      boolean test(boolean b) {
                          return b;
                      }
                  }
              """
          )
        );
    }
}
