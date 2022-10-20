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
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.tree.J
import org.openrewrite.test.RewriteTest

/**
 * @author Alex Boyko
 */
interface JavaParserTest : RewriteTest {

    @Suppress("Since15")
    @Test
    fun incompleteAssignment() {
        rewriteRun(
          java(
            """
           @Deprecated(since=)
           public class A {}
        """
          )
        )
    }

    @Suppress("RedundantSuppression")
    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2313")
    fun annotationCommentWithNoSpaceParsesCorrectly() {
        rewriteRun(
          java(
            """
                @SuppressWarnings("serial")// fred
                @Deprecated
                public class PersistenceManagerImpl {
                }
            """
          ) { spec ->
              spec.afterRecipe { cu ->
                  assertThat(cu.classes[0].leadingAnnotations).hasSize(2)
              }
          }
        )
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2313")
    fun annotationCommentWithSpaceParsesCorrectly() {
        rewriteRun(
          java(
            """
                @SuppressWarnings("serial") // fred
                @Deprecated
                public class PersistenceManagerImpl {
                }
            """
          ) { spec ->
              spec.afterRecipe { cu ->
                  assertThat(cu.classes[0].leadingAnnotations).hasSize(2)
              }
          }
        )
    }
}
