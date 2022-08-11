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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTreeTest
import org.openrewrite.java.JavaTreeTest.NestingLevel.Block
import org.openrewrite.java.JavaTreeTest.NestingLevel.CompilationUnit

interface NewClassTest : JavaTreeTest {

    @Test
    fun anonymousInnerClass(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            class A { static class B {} }
            class C {
                A.B anonB = new A.B() {};
            }
        """
    )

    @Test
    fun concreteInnerClass(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            class A { static class B {} }
            class C {
                A.B anonB = new A.B();
            }
        """
    )

    @Test
    fun concreteClassWithParams(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            Object l = new ArrayList < String > ( 0 ) { };
        """, "java.util.*"
    )

    @Test
    fun rawType(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            List<String> l = new ArrayList < > ();
        """, "java.util.*"
    )

    @Test
    fun anonymousClass(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            class Test {
                List<Integer> l = new ArrayList<Integer>() {
                    /** Javadoc */
                    @Override
                    public boolean isEmpty() {
                        return false;
                    }
                };
            }
        """.trimIndent()
    )
}
