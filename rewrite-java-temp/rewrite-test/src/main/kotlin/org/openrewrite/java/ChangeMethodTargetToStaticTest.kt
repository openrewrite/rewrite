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

interface ChangeMethodTargetToStaticTest: RefactorVisitorTest {

    @Test
    fun refactorTargetToStatic(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                """
                    package a;
                    public class A {
                       public void nonStatic() {}
                    }
                """,
                """
                    package b;
                    public class B {
                       public static void foo() {}
                    }
                """
            ),
            visitors = listOf(
                ChangeMethodTargetToStatic().apply {
                    setMethod("a.A nonStatic()")
                    setTargetType("b.B")
                },
                ChangeMethodName().apply {
                    setMethod("b.B nonStatic()")
                    name = "foo"
                }
            ),
            before = """
                import a.*;
                class C {
                   public void test() {
                       new A().nonStatic();
                   }
                }
            """,
            after = """
                import b.B;
                
                class C {
                   public void test() {
                       B.foo();
                   }
                }
            """
    )

    @Test
    fun refactorStaticTargetToStatic(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                """
                    package b;
                    public class B {
                       public static void foo() {}
                    }
                """,
                """
                    package a;
                    public class A {
                       public static void foo() {}
                    }
                """
            ),
            visitors = listOf(
                ChangeMethodTargetToStatic().apply {
                    setMethod("a.A foo()")
                    setTargetType("b.B")
                }
            ),
            before = """
                import static a.A.*;
                class C {
                   public void test() {
                       foo();
                   }
                }
            """,
            after = """
                import b.B;
                
                class C {
                   public void test() {
                       B.foo();
                   }
                }
            """
    )
}
