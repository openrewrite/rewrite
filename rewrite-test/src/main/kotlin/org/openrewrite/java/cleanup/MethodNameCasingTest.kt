/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Issue("https://github.com/openrewrite/rewrite/issues/466")
interface MethodNameCasingTest: JavaRecipeTest {
    override val recipe: Recipe?
        get() = MethodNameCasing()

    @Test
    fun changeMethodDeclaration(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                void MyMethod_with_über() {
                }
            }
        """,
        after = """
            class Test {
                void myMethodWithBer() {
                }
            }
        """
    )

    @Test
    fun changeMethodInvocations(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("""
            class Test {
                void MyMethod_with_über() {
                }
            }
        """),
        before = """
            class A {
                void test() {
                    new Test().MyMethod_with_über();
                }
            }
        """,
        after = """
            class A {
                void test() {
                    new Test().myMethodWithBer();
                }
            }
        """
    )

    @Test
    fun dontChangeCorrectlyCasedMethods(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                void dontChange() {
                }
            }
        """
    )
}
