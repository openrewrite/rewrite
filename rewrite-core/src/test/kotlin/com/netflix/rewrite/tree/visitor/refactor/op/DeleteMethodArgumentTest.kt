/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.tree.visitor.refactor.op

import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.parse.OpenJdkParser
import com.netflix.rewrite.parse.Parser
import com.netflix.rewrite.tree.Tr
import org.junit.Assert.assertTrue
import org.junit.Test

open class DeleteMethodArgumentTest : Parser by OpenJdkParser() {

    val b = """
        class B {
           public static void foo() {}
           public static void foo(int n) {}
           public static void foo(int n1, int n2) {}
           public static void foo(int n1, int n2, int n3) {}
        }
    """.trimIndent()

    @Test
    fun deleteMiddleArgument() {
        val a = parse("public class A {{ B.foo(0, 1, 2); }}", b)
        val fixed = a.refactor().deleteArgument(a.findMethodCalls("B foo(..)"), 1).fix().fixed
        assertRefactored(fixed, "public class A {{ B.foo(0, 2); }}")
    }

    @Test
    fun deleteArgumentsConsecutively() {
        val a = parse("public class A {{ B.foo(0, 1, 2); }}", b)
        val foos = a.findMethodCalls("B foo(..)")
        val fixed = a.refactor().deleteArgument(foos, 1).deleteArgument(foos, 1).fix().fixed
        assertRefactored(fixed, "public class A {{ B.foo(0); }}")
    }

    @Test
    fun doNotDeleteEmptyContainingFormatting() {
        val a = parse("public class A {{ B.foo( ); }}", b)
        val fixed = a.refactor().deleteArgument(a.findMethodCalls("B foo(..)"), 0).fix().fixed
        assertRefactored(fixed, "public class A {{ B.foo( ); }}")
    }

    @Test
    fun insertEmptyWhenLastArgumentIsDeleted() {
        val a = parse("public class A {{ B.foo( ); }}", b)
        val fixed = a.refactor().deleteArgument(a.findMethodCalls("B foo(..)"), 0).fix().fixed
        assertTrue(fixed.findMethodCalls("B foo(..)").first().args.args[0] is Tr.Empty)
    }
}
