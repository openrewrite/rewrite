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

import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class CyclicTypeTest(p: Parser): Parser by p {
    
    @Test
    fun cyclicType() {
        val a = parse("""
            public class A {
                A[] nested = new A[0];
            }
        """)
        
        val fieldType = a.fields()[0].vars[0].type.asArray()
        assertTrue(fieldType is Type.Array)

        val elemType = fieldType!!.elemType.asClass()
        assertTrue(elemType is Type.Class)

        assertTrue(elemType!!.members[0].type?.asClass()?.isCyclicRef() ?: false)
    }
}