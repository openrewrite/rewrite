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

class ForEachLoopTest implements RewriteTest {

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void format() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      for(Integer n: new Integer[] { 0, 1 }) {
                      }
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
                  void test(int[] n) {
                      for(Integer i : n) test();
                  }
                  void test() {
                  }
              }
              """
          )
        );
    }
}
