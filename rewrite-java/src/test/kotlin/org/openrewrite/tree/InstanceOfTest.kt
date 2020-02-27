/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import org.openrewrite.asClass
import org.openrewrite.firstMethodStatement

open class InstanceOfTest : Parser() {

    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                Object o;
                public void test() {
                    boolean b = o instanceof String;
                }
            }
        """)
    }

    private val variable by lazy { a.firstMethodStatement() as J.VariableDecls }
    private val instanceof by lazy { variable.vars[0].initializer as J.InstanceOf }

    @Test
    fun instanceOf() {
        assertEquals("java.lang.String", (instanceof.clazz as J.Ident).type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun format() {
        assertEquals("o instanceof String", instanceof.printTrimmed())
    }
}