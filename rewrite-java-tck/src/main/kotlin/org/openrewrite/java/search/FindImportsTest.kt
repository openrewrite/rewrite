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
package org.openrewrite.java.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.TypeMatcher
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface FindImportsTest: RewriteTest {

    @Override
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(FindImports("java.util..*"))
    }

    @Test
    fun typeMatcherOnImports() {
        assertThat(TypeMatcher("java.util..*").matchesPackage("java.util.List")).isTrue()
        assertThat(TypeMatcher("java.util..*").matchesPackage("java.util.concurrent.*")).isTrue()
        assertThat(TypeMatcher("java.util..*").matchesPackage("java.util.*")).isTrue()
        assertThat(TypeMatcher("java.util.List").matchesPackage("java.util.*")).isTrue()
    }

    @Test
    fun exactMatch() {
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
        )
    }

    @Test
    fun starImport() {
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
        )
    }

    @Test
    fun starImportMatchesExact() {
        rewriteRun(
          { spec -> spec.recipe(FindImports("java.util.List")) },
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
        )
    }
}
