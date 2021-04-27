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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

interface DeleteMethodArgumentTest : JavaRecipeTest {
    companion object {
        private const val b = """
            class B {
               public static void foo() {}
               public static void foo(int n) {}
               public static void foo(int n1, int n2) {}
               public static void foo(int n1, int n2, int n3) {}
            }
        """
    }

    @Test
    fun deleteMiddleArgumentDeclarative(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(b),
        recipe = DeleteMethodArgument("B foo(int, int, int)", 1),
        before = "public class A {{ B.foo(0, 1, 2); }}",
        after = "public class A {{ B.foo(0, 2); }}",
        cycles = 1,
        expectedCyclesThatMakeChanges = 1
    )

    @Test
    fun deleteMiddleArgument(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(b),
        recipe = DeleteMethodArgument("B foo(int, int, int)", 1),
        before = "public class A {{ B.foo(0, 1, 2); }}",
        after = "public class A {{ B.foo(0, 2); }}",
        cycles = 1,
        expectedCyclesThatMakeChanges = 1
    )

    @Test
    fun deleteArgumentsConsecutively(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(b),
        recipe = DeleteMethodArgument("B foo(int, int, int)", 1)
            .doNext(DeleteMethodArgument("B foo(int, int, int)", 1)),
        before = "public class A {{ B.foo(0, 1, 2); }}",
        after = "public class A {{ B.foo(0); }}"
    )

    @Test
    fun doNotDeleteEmptyContainingFormatting(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf(b),
        recipe = DeleteMethodArgument("B foo(..)", 0),
        before = "public class A {{ B.foo( ); }}"
    )

    @Test
    fun insertEmptyWhenLastArgumentIsDeleted(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(b),
        recipe = DeleteMethodArgument("B foo(..)", 0),
        before = "public class A {{ B.foo(1); }}",
        after = "public class A {{ B.foo(); }}"
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        val recipe = DeleteMethodArgument(null, 0)
        val valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("methodPattern")
    }
}
