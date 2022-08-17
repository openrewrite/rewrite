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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RewriteTest

interface RecipeExceptionDemonstrationTest : RewriteTest {
    override fun defaultExecutionContext(): ExecutionContext {
        return InMemoryExecutionContext()
    }

    @BeforeEach
    fun beforeEach() {
        RecipeExceptionDemonstration.DemonstrationException.restrictStackTrace = true
    }

    @AfterEach
    fun afterEach() {
        RecipeExceptionDemonstration.DemonstrationException.restrictStackTrace = false
    }

    @Test
    fun listAdd(jp: JavaParser) = rewriteRun(
        { spec ->
            spec
                .recipe(RecipeExceptionDemonstration("java.util.List add(..)"))
                .parser(jp)
        },
        java(
            """
                import java.util.*;
                class Test {
                    void test(List<Integer> list) {
                        list.add(42);
                    }
                }
            """,
            """
                import java.util.*;
                class Test {
                    void test(List<Integer> list) {
                        /*~~(org.openrewrite.java.RecipeExceptionDemonstration${'$'}DemonstrationException: Demonstrating an exception thrown on a matching method.
                  org.openrewrite.java.RecipeExceptionDemonstration${'$'}1.visitMethodInvocation(RecipeExceptionDemonstration.java:54)
                  org.openrewrite.java.RecipeExceptionDemonstration${'$'}1.visitMethodInvocation(RecipeExceptionDemonstration.java:48))~~>*/list.add(42);
                    }
                }
            """
        )
    )
}
