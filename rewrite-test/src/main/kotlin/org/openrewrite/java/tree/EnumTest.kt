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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

interface EnumTest {
    @Test
    fun anonymousClassInitializer(jp: JavaParser) {
        val aSource = """
            public enum A {
                A1(1) {
                    @Override
                    void foo() {}
                },

                A2 {
                    @Override
                    void foo() {}
                };
                
                A() {}
                A(int n) {}
                
                abstract void foo();
            }
        """.trimIndent()

        val a = jp.parse(aSource)

        assertEquals(aSource, a.printTrimmed())
    }

    @Test
    fun noArguments(jp: JavaParser) {
        val aSource = """
            public enum A {
                A1, A2();
            }
        """.trimIndent()

        val a = jp.parse(aSource)

        assertEquals(aSource, a.printTrimmed())
    }
}