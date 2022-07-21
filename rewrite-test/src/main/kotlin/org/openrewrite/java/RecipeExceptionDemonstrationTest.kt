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

import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.test.RewriteTest

interface RecipeExceptionDemonstrationTest : RewriteTest {

    @Test
    fun listAdd(jp: JavaParser) = rewriteRun(
        { spec ->
            spec
                .recipe(RecipeExceptionDemonstration("java.util.List add(..)"))
                .parser(jp)
                .executionContext(InMemoryExecutionContext())
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
                        /*~~(java.lang.RuntimeException: Demonstrating an exception thrown on a matching method.
                  org.openrewrite.java.RecipeExceptionDemonstration${'$'}1.visitMethodInvocation(RecipeExceptionDemonstration.java:43)
                  org.openrewrite.java.RecipeExceptionDemonstration${'$'}1.visitMethodInvocation(RecipeExceptionDemonstration.java:37)
                  org.openrewrite.java.tree.J${'$'}MethodInvocation.acceptJava(J.java:3393)
                  org.openrewrite.java.tree.J.accept(J.java:55)
                  org.openrewrite.TreeVisitor.visit(TreeVisitor.java:222)
                  org.openrewrite.TreeVisitor.visitAndCast(TreeVisitor.java:306)
                  org.openrewrite.java.JavaVisitor.visitRightPadded(JavaVisitor.java:1228)
                  org.openrewrite.java.JavaVisitor.lambda${'$'}visitBlock${'$'}4(JavaVisitor.java:371)
                  ...)~~>*/list.add(42);
                    }
                }
            """
        )
    )
}
