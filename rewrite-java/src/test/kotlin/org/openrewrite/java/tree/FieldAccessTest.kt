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
import org.openrewrite.java.fields

interface FieldAccessTest {
    
    @Test
    fun fieldAccess(jp: JavaParser) {
        val b = """
            public class B {
                public B field = new B();
            }
        """
        
        val a = """
            public class A {
                B b = new B();
                String s = b . field . field;
            }
        """

        val cu = jp.parse(a, b)

        val acc = cu.fields(0..1).flatMap { it.vars }.find { it.initializer is J.FieldAccess }?.initializer as J.FieldAccess?
        assertEquals("b . field . field", acc?.printTrimmed())
        assertEquals("field", acc?.simpleName)
        assertEquals("b . field", acc?.target?.printTrimmed())
    }
}