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
package com.netflix.rewrite.search

import com.netflix.rewrite.ast.hasElementType
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class FindFieldsTest(p: Parser): Parser by p {

    @Test
    fun findPrivateNonInheritedField() {
        val a = parse("""
            import java.util.*;
            public class A {
               private List list;
               private Set set;
            }
        """)

        val fields = a.classes[0].findFields(List::class.java)

        assertEquals(1, fields.size)
        assertEquals("list", fields[0].vars[0].name.printTrimmed())
        assertTrue(fields[0].typeExpr.type.hasElementType("java.util.List"))
    }
    
    @Test
    fun findArrayOfType() {
        val a = parse("""
            import java.util.*;
            public class A {
               private String[] s;
            }
        """)

        val fields = a.classes[0].findFields(String::class.java)

        assertEquals(1, fields.size)
        assertEquals("s", fields[0].vars[0].name.printTrimmed())
        assertTrue(fields[0].typeExpr.type.hasElementType("java.lang.String"))
    }
}

class OracleJdkFindFieldsTest: FindFieldsTest(OracleJdkParser())