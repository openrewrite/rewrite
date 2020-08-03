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
package org.openrewrite.java.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.hasElementType

interface FindFieldsTest {

    @Test
    fun findPrivateNonInheritedField(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            public class A {
               private List list;
               private Set set;
            }
        """)[0]

        val fields = a.classes[0].findFields("java.util.List")

        assertEquals(1, fields.size)
        assertEquals("list", fields[0].vars[0].name.printTrimmed())
        assertTrue(fields[0].typeExpr?.type.hasElementType("java.util.List"))
    }
    
    @Test
    fun findArrayOfType(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            public class A {
               private String[] s;
            }
        """)[0]

        val fields = a.classes[0].findFields("java.lang.String")

        assertEquals(1, fields.size)
        assertEquals("s", fields[0].vars[0].name.printTrimmed())
        assertTrue(fields[0].typeExpr?.type.hasElementType("java.lang.String"))
    }

    @Test
    fun skipsMultiCatches(jp: JavaParser) {
        val a = jp.parse("""
            import java.io.*;
            public class A {
                File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) {}
                    catch(FileNotFoundException | RuntimeException e) {}
                }
            }
        """)[0]

        assertEquals(1, a.classes[0].findFields("java.io.File").size)
    }
}
