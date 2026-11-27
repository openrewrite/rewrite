/*
 * Copyright 2026 the original author or authors.
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

class HasMethodTest implements RewriteTest {

    @DocumentExample
    @Test
    void markFileUsingMethod() {
        rewriteRun(
          spec -> spec.recipe(new HasMethod("java.util.List add(..)", null)),
          java(
            """
              import java.util.List;

              class Test {
                  void test(List<String> list) {
                      list.add("a");
                  }
              }
              """,
            """
              /*~~>*/import java.util.List;

              class Test {
                  void test(List<String> list) {
                      list.add("a");
                  }
              }
              """
          ),
          java(
            """
              import java.util.List;

              class Other {
                  int test(List<String> list) {
                      return list.size();
                  }
              }
              """
          )
        );
    }

    @Test
    void markFileUsingOverride() {
        rewriteRun(
          spec -> spec.recipe(new HasMethod("java.util.Collection add(..)", true)),
          java(
            """
              import java.util.ArrayList;

              class Test {
                  void test(ArrayList<String> list) {
                      list.add("a");
                  }
              }
              """,
            """
              /*~~>*/import java.util.ArrayList;

              class Test {
                  void test(ArrayList<String> list) {
                      list.add("a");
                  }
              }
              """
          )
        );
    }
}
