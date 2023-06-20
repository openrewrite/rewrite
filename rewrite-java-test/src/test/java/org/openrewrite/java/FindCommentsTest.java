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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.java.search.FindComments;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;

import static org.openrewrite.java.Assertions.java;

class FindCommentsTest implements RewriteTest {

    @DocumentExample
    @Test
    void findText() {
        rewriteRun(
          spec -> spec.recipe(new FindComments(Arrays.asList("test", "12.*"))),
          java(
            """
              // not this one
              // test
              // not this one, either
              // comment 123
              class Test {
                  int n = 123;
                  String s = "test";
                  String s = "mytest";
              }
              """,
            """
              // not this one
              /*~~>*/// test
              // not this one, either
              /*~~>*/// comment 123
              class Test {
                  int n = /*~~>*/123;
                  String s = /*~~>*/"test";
                  String s = /*~~>*/"mytest";
              }
              """
          )
        );
    }

    @Test
    void findSecrets() {
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.java.search.FindSecrets")),
          java(
            """
              class Test {
                  String uhOh = "-----BEGIN RSA PRIVATE KEY-----";
              }
              """,
            """
              class Test {
                  String uhOh = /*~~>*/"-----BEGIN RSA PRIVATE KEY-----";
              }
              """
          )
        );
    }
}
