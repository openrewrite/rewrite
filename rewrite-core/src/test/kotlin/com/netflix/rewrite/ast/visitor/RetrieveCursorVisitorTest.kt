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
package com.netflix.rewrite.ast.visitor

import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RetrieveCursorVisitorTest: Parser by OracleJdkParser()  {

    @Test
    fun retrieveCursor() {
        val a = parse("""
            public class A {
                public void test() {
                    String s;
                }
            }
        """)

        val s = a.classes[0].methods()[0].body!!.statements[0]

        val cursor = a.cursor(s)
        assertNotNull(cursor)
        assertEquals("CompilationUnit,ClassDecl,Block,MethodDecl,Block,VariableDecls",
                cursor!!.path.joinToString(",") { it::class.java.simpleName })
    }
}