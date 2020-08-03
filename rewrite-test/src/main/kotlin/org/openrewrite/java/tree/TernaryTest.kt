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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass
import org.openrewrite.java.firstMethodStatement

interface TernaryTest {

    @Test
    fun ternary(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int n;
                public void test() {
                    String evenOrOdd = n % 2 == 0 ? "even" : "odd";
                }
            }
        """)[0]

        val evenOrOdd = a.firstMethodStatement() as J.VariableDecls
        val ternary = evenOrOdd.vars[0].initializer as J.Ternary

        assertEquals("java.lang.String", ternary.type.asClass()?.fullyQualifiedName)
        assertTrue(ternary.condition is J.Binary)
        assertTrue(ternary.truePart is J.Literal)
        assertTrue(ternary.falsePart is J.Literal)
        assertEquals("""n % 2 == 0 ? "even" : "odd"""", ternary.printTrimmed())
    }
}
