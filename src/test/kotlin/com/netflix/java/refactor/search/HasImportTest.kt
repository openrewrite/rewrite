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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class HasImportTest(p: Parser): Parser by p {
    
    @Test
    fun hasImport() {
        val a = parse("""
            |import java.util.List;
            |class A {}
        """)
        
        assertTrue(a.hasImport(List::class.java))
        assertFalse(a.hasImport(Set::class.java))
    }

    @Test
    fun hasStarImport() {
        val a = parse("""
            |import java.util.*;
            |class A {}
        """)

        assertTrue(a.hasImport(List::class.java))
    }

    @Test
    fun hasStarImportOnInnerClass() {
        val a = """
            |package a;
            |public class A {
            |   public static class B { }
            |}
        """
        
        val c = """
            |import a.*;
            |public class C {
            |    A.B b = new A.B();
            |}
        """

        assertTrue(parse(c, a).hasImport("a.A.B"))
        assertTrue(parse(c, a).hasImport("a.A"))
    }
}

class OracleJdkHasImportTest: HasImportTest(OracleJdkParser())