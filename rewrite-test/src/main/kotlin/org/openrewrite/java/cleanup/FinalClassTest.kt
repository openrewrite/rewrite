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
import org.openrewrite.java.JavaRecipeTest

interface FinalClassTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = FinalClass()

    @Test
    fun finalizeClass() = assertChanged(
        before = """
            public class A {
                private A(String s) {
                }
            
                private A() {
                }
            }
        """,
        after = """
            public final class A {
                private A(String s) {
                }
            
                private A() {
                }
            }
        """
    )

    @Test
    fun hasPublicConstructor() = assertUnchanged(
        before = """
            public class A {
                private A(String s) {
                }
                
                public A() {
                }
            }
        """
    )

    @Test
    fun hasImplicitConstructor() = assertUnchanged(
        before = """
            public class A {
            }
        """
    )

    @Test
    fun innerClass() = assertChanged(
        before = """
            class A {
            
                class B {
                    private B() {}
                }
            }
        """,
        after = """
            class A {
            
                final class B {
                    private B() {}
                }
            }
        """
    )

    @Test
    fun classInsideInterfaceIsImplicitlyFinal() = assertUnchanged(
         before = """
             public interface A {
                class B {
                    private B() { }
                }
             }
         """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1061")
    @Test
    fun abstractClass() = assertUnchanged(
        before = """
            public abstract class A {
                
                public static void foo() {
                }
                
                private A() {
                }
            }
        """
    )
}
