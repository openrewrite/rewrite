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
@file:Suppress("UnnecessaryEnumModifier")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface NestedEnumsAreNotStaticTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = NestedEnumsAreNotStatic()

    @Test
    fun enumIsNotNested() = assertUnchanged(
        before = """
            static enum ABC {
                A, B, C
            }
        """
    )

    @Test
    fun nestedEnumIsNotStatic() = assertUnchanged(
        before = """
            class A {
                enum ABC {
                    A, B, C
                }
            }
        """
    )

    @Test
    fun nestedEnumIsStatic() = assertChanged(
        before = """
            class A {
            
                static enum ABC {
                    A, B, C
                }
            
                private static enum DEF {
                    D, E, F
                }
            }
        """,
        after = """
            class A {
            
                enum ABC {
                    A, B, C
                }
            
                private enum DEF {
                    D, E, F
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1222")
    fun doesNotReformatWholeEnum() = assertChanged(
        before = """
            public class Test {
                public static enum testEnum {
                    Account;
            
                    public final String field;
            
                    private testEnum() {this.field = this.name();}
                }
            }
        """,
        after = """
            public class Test {
                public enum testEnum {
                    Account;
            
                    public final String field;
            
                    private testEnum() {this.field = this.name();}
                }
            }
        """
    )
}
