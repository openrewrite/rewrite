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

open class BlockTest : JavaParser() {
    
    @Test
    fun methodBlock() {
        val a = parse("""
            public class A {
                public void foo() {
                    System.out.println("foo");
                }
            }
        """)
        
        assertEquals(1, a.classes[0].methods[0].body!!.statements.size)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                public void foo() {  }
            }
        """)
        
        assertEquals("{  }", a.classes[0].methods[0].body!!.printTrimmed())
    }

    @Test
    fun staticInitBlock() {
        val a = parse("""
            public class A {
                static {}
            }
        """)

        assertEquals("static {}", a.classes[0].body.statements[0].printTrimmed())
    }
}