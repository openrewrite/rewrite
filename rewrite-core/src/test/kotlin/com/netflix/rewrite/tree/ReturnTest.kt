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
import com.netflix.rewrite.Parser
import org.junit.Assert.*
import org.junit.Test

open class ReturnTest : Parser() {
    
    @Test
    fun returnValue() {
        val a = parse("""
            public class A {
                public String test() {
                    return "";
                }
            }
        """)
        
        val rtn = a.firstMethodStatement() as Tr.Return
        assertTrue(rtn.expr is Tr.Literal)
    }

    @Test
    fun returnVoid() {
        val a = parse("""
            public class A {
                public void test() {
                    return;
                }
            }
        """)

        val rtn = a.firstMethodStatement() as Tr.Return
        assertNull(rtn.expr)
    }
    
    @Test
    fun format() {
        val a = parse("""
            public class A {
                public int test() {
                    return 0;
                }
            }
        """)

        val rtn = a.firstMethodStatement() as Tr.Return
        assertEquals("return 0", rtn.printTrimmed())
    }
}