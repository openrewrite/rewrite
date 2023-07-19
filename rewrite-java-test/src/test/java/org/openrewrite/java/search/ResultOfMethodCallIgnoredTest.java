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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ResultOfMethodCallIgnoredTest implements RewriteTest {

    @DocumentExample
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void resultOfMethodCallIgnored() {
        rewriteRun(
          spec -> spec.recipe(new ResultOfMethodCallIgnored("java.io.File mkdir*()", false)),
          java(
            """
              import java.io.File;
              class Test {
                  void test() {
                      new File("dir").mkdirs();
                      new File("dir").mkdir();
                      boolean b1 = new File("dir").mkdirs();
                      if(!new File("dir").mkdirs()) {
                          throw new IllegalStateException("oops");
                      }
                  }
              }
              """,
            """
              import java.io.File;
              class Test {
                  void test() {
                      /*~~>*/new File("dir").mkdirs();
                      /*~~>*/new File("dir").mkdir();
                      boolean b1 = new File("dir").mkdirs();
                      if(!new File("dir").mkdirs()) {
                          throw new IllegalStateException("oops");
                      }
                  }
              }
              """
          )
        );
    }
}
