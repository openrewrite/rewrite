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
package org.openrewrite.java

import org.junit.jupiter.api.Test

interface SimplifyMethodChainTest : JavaRecipeTest {

    @Test
    fun simplify(jp: JavaParser) = assertChanged(
        jp,
        recipe = SimplifyMethodChain(
            listOf(
                "A b()",
                "B c()"
            ),
            "c2"
        ),
        dependsOn = arrayOf(
            """
                class A {
                    static B b() { return new B(); }
                    static C c2() { return new C(); }
                }
                
                class B {
                    C c() { return new C(); }
                }
                
                class C {
                }
            """
        ),
        before = """
            class Test {
                C c = A.b().c();
            }
        """,
        after = """
            class Test {
                C c = A.c2();
            }
        """
    )
}
