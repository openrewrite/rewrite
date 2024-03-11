/*
 * Copyright 2022 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.test.SourceSpecs.text;

class HasMinimumJavaVersionTest implements RewriteTest {

    @Test
    void matches() {
        rewriteRun(
          spec -> spec.recipe(new HasMinimumJavaVersion("8-21", false)),
          java(
            """
              class Test {
              }
              """,
            """
              /*~~(Java version 8)~~>*/class Test {
              }
              """,
            spec -> spec.markers(javaVersion(8))
          ),
          java(
            """
              class Higher {
              }
              """,
            spec -> spec.markers(javaVersion(17))
          )
        );
    }

    @Test
    void noMatch() {
        rewriteRun(
          spec -> spec.recipe(new HasMinimumJavaVersion("17-21", false)),
          java(
            """
              class Test {
              }
              """,
            spec -> spec.markers(javaVersion(8))
          ),
          java(
            """
              class Higher {
              }
              """,
            spec -> spec.markers(javaVersion(19))
          )
        );
    }
}
