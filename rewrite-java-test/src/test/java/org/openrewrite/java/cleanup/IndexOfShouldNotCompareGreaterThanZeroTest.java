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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"StatementWithEmptyBody", "ListIndexOfReplaceableByContains", "IndexOfReplaceableByContains"})
class IndexOfShouldNotCompareGreaterThanZeroTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new IndexOfShouldNotCompareGreaterThanZero());
    }

    @Test
    void indexOfOnList() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class Test {
                  static boolean hasIndex(List<String> strList, String str) {
                      if (strList.indexOf(str) > 0) {
                      }
                      return strList.indexOf(str) > 0;
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  static boolean hasIndex(List<String> strList, String str) {
                      if (strList.indexOf(str) >= 1) {
                      }
                      return strList.indexOf(str) >= 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void indexOfOnString() {
        rewriteRun(
          java(
            """
              class Test {
                  static boolean hasIndex(String str) {
                      if (str.indexOf("str") > 0) {
                      }
                      return str.indexOf("str") > 0;
                  }
              }
              """,
            """
              class Test {
                  static boolean hasIndex(String str) {
                      if (str.indexOf("str") >= 1) {
                      }
                      return str.indexOf("str") >= 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeGreaterThanOrEqualToZero() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class Test {
                  static void hasIndex(List<String> strList, String str) {
                      if (strList.indexOf(str) >= 0) {
                      }
                      if (str.indexOf("str") >= 0) {
                      }
                  }
              }
              """
          )
        );
    }
}
