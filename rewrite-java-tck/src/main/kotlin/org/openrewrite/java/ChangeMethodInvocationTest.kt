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
import org.openrewrite.Issue

interface ChangeMethodInvocationTest : JavaRecipeTest {

    @Test
    fun changeName() = assertChanged(
        recipe = ChangeMethodInvocation("java.io.PrintStream print(..)", "println", true),
        before = """
            abstract class Test {
                void test() {
                    System.out.print("Hello");
                    System.out.print("World");
                }
            }
        """,
        after = """
            abstract class Test {
                void test() {
                    System.out.println("Hello");
                    System.out.println("World");
                }
            }
        """
    )
}
