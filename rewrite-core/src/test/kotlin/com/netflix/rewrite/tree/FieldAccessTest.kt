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

import com.netflix.rewrite.fields
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.*
import org.junit.Test

abstract class FieldAccessTest(p: Parser): Parser by p {
    
    @Test
    fun fieldAccess() {
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

        val cu = parse(a, b)

        val acc = cu.fields(0..1).flatMap { it.vars }.find { it.initializer is Tr.FieldAccess }?.initializer as Tr.FieldAccess?
        assertEquals("b . field . field", acc?.printTrimmed())
        assertEquals("field", acc?.simpleName)
        assertEquals("b . field", acc?.target?.printTrimmed())
    }
}