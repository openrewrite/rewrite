/*
 * Copyright 2020 the original authors.
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
package com.netflix.rewrite.tree.visitor

import com.netflix.rewrite.parse.OpenJdkParser
import com.netflix.rewrite.parse.Parser
import com.netflix.rewrite.tree.Tr
import com.netflix.rewrite.tree.visitor.refactor.FindIndentVisitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

open class FindIndentVisitorTest : Parser by OpenJdkParser() {
    val a = """
        public class A {
            public void test() {
                String s;
            }
        }
    """.trimIndent()

    @Test
    fun findIndentWholeClass() {
        val findIndentVisitor = FindIndentVisitor(0);
        assertEquals(4, findIndentVisitor.visit(parse(a)))
        assertTrue(findIndentVisitor.isIndentedWithSpaces)
    }

    @Test
    fun findIndentInBlock() {
        val findIndentVisitor = FindIndentVisitor(4)
        findIndentVisitor.visit(parse(a).classes[0].methods[0])
        assertEquals(4, findIndentVisitor.mostCommonIndent)
        assertTrue(findIndentVisitor.isIndentedWithSpaces)
    }
}
