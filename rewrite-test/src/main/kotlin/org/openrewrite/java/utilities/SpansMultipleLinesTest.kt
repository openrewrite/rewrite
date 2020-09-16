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
package org.openrewrite.java.utilities

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J

interface SpansMultipleLinesTest {

    @Test
    fun spansMultipleLines(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int m, n;
                {
                    { int n = 1; }
                    { int n = 2;
                    }

                    if(n == 1) {
                    }

                    if(n == 1 &&
                        m == 2) {
                    }
                }
            }
        """.trimIndent())

        val init = a[0].classes[0].body.statements[1] as J.Block<*>

        assertFalse(SpansMultipleLines(init.statements[0], null).visit(init.statements[0]))
        assertTrue(SpansMultipleLines(init.statements[1], null).visit(init.statements[1]))

        val iff = init.statements[2] as J.If
        assertFalse(SpansMultipleLines(iff, iff.thenPart).visit(iff))

        val iff2 = init.statements[3] as J.If
        assertTrue(SpansMultipleLines(iff2, iff2.thenPart).visit(iff2))
    }

    @Test
    fun classDecl(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                {
                }
            }
        """.trimIndent())

        val aClass = a[0].classes[0]
        assertFalse(SpansMultipleLines(aClass, aClass.body).visit(aClass))
    }
}
