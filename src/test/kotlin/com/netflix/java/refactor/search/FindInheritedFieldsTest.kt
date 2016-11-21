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
package com.netflix.java.refactor.search

import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class FindInheritedFieldsTest(p: Parser): Parser by p {

    @Test
    fun findInheritedField() {
        val a = """
            import java.util.*;
            public class A {
               protected List list;
               private Set set;
            }
        """

        val b = parse("public class B extends A { }", a)

        assertEquals("list", b.classes[0].findInheritedFields(List::class.java).firstOrNull()?.name)

        // the Set field is not considered to be inherited because it is private
        val fields = b.classes[0].findInheritedFields(Set::class.java)
        assertTrue(fields.isEmpty())
    }

    @Test
    fun findArrayOfType() {
        val a = """
            public class A {
               String[] s;
            }
        """

        val b = parse("public class B extends A { }", a)

        val fields = b.classes[0].findInheritedFields(String::class.java)
        assertEquals(1, fields.size)
        assertEquals("s", fields[0].name)

        assertTrue(b.classes[0].findInheritedFields(Set::class.java).isEmpty())
    }
}

class OracleFindInheritedFieldsTest: FindInheritedFieldsTest(OracleJdkParser())