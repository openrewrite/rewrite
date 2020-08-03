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
package org.openrewrite.java

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.tree.J
import org.openrewrite.whenParsedBy

interface DeleteMethodArgumentTest {
    companion object {
        val b = """
            class B {
               public static void foo() {}
               public static void foo(int n) {}
               public static void foo(int n1, int n2) {}
               public static void foo(int n1, int n2, int n3) {}
            }
        """.trimIndent()
    }

    @Test
    fun deleteMiddleArgumentDeclarative(jp: JavaParser) {
        "public class A {{ B.foo(0, 1, 2); }}"
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(DeleteMethodArgument().apply { setMethod("B foo(..)"); setIndex(1) })
                .isRefactoredTo("public class A {{ B.foo(0, 2); }}")
    }

    @Test
    fun deleteMiddleArgument(jp: JavaParser) {
        "public class A {{ B.foo(0, 1, 2); }}"
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(DeleteMethodArgument().apply {
                    setMethod("B foo(..)")
                    setIndex(1)
                })
                .isRefactoredTo("public class A {{ B.foo(0, 2); }}")
    }

    @Test
    fun deleteArgumentsConsecutively(jp: JavaParser) {
        "public class A {{ B.foo(0, 1, 2); }}"
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(DeleteMethodArgument().apply {
                    setMethod("B foo(..)")
                    setIndex(1)
                })
                .whenVisitedBy(DeleteMethodArgument().apply {
                    setMethod("B foo(..)")
                    setIndex(1)
                })
                .isRefactoredTo("public class A {{ B.foo(0); }}")
    }

    @Test
    fun doNotDeleteEmptyContainingFormatting(jp: JavaParser) {
        "public class A {{ B.foo( ); }}"
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(DeleteMethodArgument().apply {
                    setMethod("B foo(..)")
                    setIndex(0)
                })
                .isUnchanged()
    }

    @Test
    fun insertEmptyWhenLastArgumentIsDeleted(jp: JavaParser) {
        val fixed = "public class A {{ B.foo(1); }}"
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(DeleteMethodArgument().apply {
                    setMethod("B foo(..)")
                    setIndex(0)
                })
                .isRefactoredTo("public class A {{ B.foo(); }}")
                .fixed()[0]

        assertTrue(fixed.findMethodCalls("B foo(..)").first().args.args[0] is J.Empty)
    }
}
