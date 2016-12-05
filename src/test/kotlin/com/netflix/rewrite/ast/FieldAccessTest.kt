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
package com.netflix.rewrite.ast

import com.netflix.rewrite.fields
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class FieldAccessTest(p: Parser): Parser by p {
    
    @Test
    fun fieldAccess() {
        val b = """
            public class B {
                public String field = "foo";
            }
        """
        
        val a = """
            public class A {
                B b = new B();
                String s = b.field;
            }
        """

        val cu = parse(a, whichDependsOn = b)
        val acc = cu.fields(1..1)[0].vars[0].initializer as Tr.FieldAccess
        assertEquals("field", acc.simpleName)
        assertEquals("b", acc.target.printTrimmed())
    }
}