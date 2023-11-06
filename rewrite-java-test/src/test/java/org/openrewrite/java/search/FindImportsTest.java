/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License")
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
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class FindImportsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindImports("java.util..*", null));
    }

    @Test
    void typeMatcherOnImports() {
        assertThat(new TypeMatcher("java.util..*").matchesPackage("java.util.List")).isTrue();
        assertThat(new TypeMatcher("java.util..*").matchesPackage("java.util.concurrent.*")).isTrue();
        assertThat(new TypeMatcher("java.util..*").matchesPackage("java.util.*")).isTrue();
        assertThat(new TypeMatcher("java.util.List").matchesPackage("java.util.*")).isTrue();
    }

    @DocumentExample
    @Test
    void exactMatch() {
        rewriteRun(
          java(
            """
              import java.util.concurrent.atomic.AtomicBoolean;
              class Test {
              }
              """,
            """
              /*~~>*/import java.util.concurrent.atomic.AtomicBoolean;
              class Test {
              }
              """
          )
        );
    }

    @Test
    void starImport() {
        rewriteRun(
          java(
            """
              import java.util.concurrent.atomic.*;
              class Test {
              }
              """,
            """
              /*~~>*/import java.util.concurrent.atomic.*;
              class Test {
              }
              """
          )
        );
    }

    @Test
    void starImportMatchesExact() {
        rewriteRun(
          spec -> spec.recipe(new FindImports("java.util.List", null)),
          java(
            """
              import java.util.*;
              class Test {
              }
              """,
            """
              /*~~>*/import java.util.*;
              class Test {
              }
              """
          )
        );
    }
}
