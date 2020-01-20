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
package com.netflix.rewrite.tree

import com.netflix.rewrite.firstMethodStatement
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class AssignTest(p: Parser): Parser by p {
    
    @Test
    fun assignmentToField() {
        val a = parse("""
            public class A {
                String s;
                public void test() {
                    s = "foo";
                }
            }
        """)
        
        val assign = a.firstMethodStatement() as Tr.Assign
        assertEquals("s", (assign.variable as Tr.Ident).simpleName)
        assertTrue(assign.assignment is Tr.Literal)
    }
    
    @Test
    fun format() {
        val a = parse("""
            @SuppressWarnings(value = "ALL")
            public class A {}
        """)
        
        val assign = a.classes[0].annotations[0].args!!.args[0] as Tr.Assign
        
        assertEquals("value = \"ALL\"", assign.printTrimmed())
    }
}