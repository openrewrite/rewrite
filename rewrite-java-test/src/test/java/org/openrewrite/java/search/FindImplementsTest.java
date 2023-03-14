/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindImplementsTest implements RewriteTest {

    @Test
    void found() {
        rewriteRun(
          spec -> spec.recipe(new FindImplements("java.lang.Runnable")),
          java(
            """
              class Test implements Runnable {
                  @Override
                  public void run() {
                  }
              }
              """,
            """
              /*~~>*/class Test implements Runnable {
                  @Override
                  public void run() {
                  }
              }
              """
          )
        );
    }

    @Test
    void notFound() {
        rewriteRun(
          spec -> spec.recipe(new FindImplements("java.lang.Runnable")),
          java(
            """
              class Test  {
                  public void run() {
                  }
              }
              """
          )
        );
    }

    @Test
    void genericType() {
        rewriteRun(
          spec -> spec.recipe(new FindImplements("java.lang.Comparable<java.lang.String>")),
          java(
            """
              class Test implements Comparable<String> {
                  @Override
                  public int compareTo(String o) {
                      return 0;
                  }
              }
              """,
            """
              /*~~>*/class Test implements Comparable<String> {
                  @Override
                  public int compareTo(String o) {
                      return 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void unmatchedGenericType() {
        rewriteRun(
          spec -> spec.recipe(new FindImplements("java.lang.Comparable<java.lang.Runnable>")),
          java(
            """
              class Test implements Comparable<String> {
                  @Override
                  public int compareTo(String o) {
                      return 0;
                  }
              }
              """
          )
        );
    }
}
