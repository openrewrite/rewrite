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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

class HasSourceSetTest implements RewriteTest {

    @Test
    void main() {
        rewriteRun(
          spec -> spec.recipe(new HasSourceSet("main")),
          srcMainJava(
            java(
              """
                class Test {
                }
                """,
              """
                /*~~>*/class Test {
                }
                """
            )
          )
        );
    }

    @Test
    void test() {
        rewriteRun(
          spec -> spec.recipe(new HasSourceSet("test")),
          srcTestJava(
            java(
              """
                class Test {
                }
                """,
              """
                /*~~>*/class Test {
                }
                """
            )
          )
        );
    }
}
