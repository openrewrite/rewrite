/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveAnnotationAttributeTest implements RewriteTest {

    @Test
    void removeNamedAttribute() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotationAttribute("java.lang.Deprecated", "since")),
          java(
            """
              @Deprecated(since = "1.0", forRemoval = true)
              class A {}
              """,
            """
              @Deprecated(forRemoval = true)
              class A {}
              """
          )
        );
    }

    @Test
    void removeLastAttribute() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotationAttribute("java.lang.Deprecated", "since")),
          java(
            """
              @Deprecated(since = "1.0")
              class A {}
              """,
            """
              @Deprecated
              class A {}
              """
          )
        );
    }

    @Test
    void doNotChangeIfNameDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotationAttribute("java.lang.Deprecated", "forRemoval")),
          java(
            """
              @Deprecated(since = "1.0")
              class A {}
              """
          )
        );
    }

    @Test
    void removeValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotationAttribute("java.lang.SuppressWarnings", "value")),
          java(
            """
              @SuppressWarnings({"foo", "bar"})
              class A {}
              """,
            """
              @SuppressWarnings
              class A {}
              """
          )
        );
    }
}
