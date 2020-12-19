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

import org.junit.jupiter.api.Test
import org.openrewrite.RefactorVisitorTest

interface ImplementInterfaceTest : RefactorVisitorTest {
    companion object {
        const val b = "package b;\ninterface B {}"
        const val c = "package c;\ninterface C {}"
    }

    @Test
    fun firstImplementedInterface(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(b),
            visitorsMapped = listOf { cu ->
                ImplementInterface.Scoped(cu.classes[0], "b.B")
            },
            before = """
                class A {
                }
            """,
            after = """
                import b.B;
                
                class A implements B {
                }
            """
    )

    @Test
    fun addAnImplementedInterface(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(b),
            visitorsMapped = listOf { cu ->
                ImplementInterface.Scoped(cu.classes[0], "c.C")
            },
            before = """
                import b.B;
                
                class A implements B {
                }
            """,
            after = """
                import b.B;
                import c.C;
                
                class A implements C, B {
                }
            """
    )
}
