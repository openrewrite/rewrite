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
import org.junit.Assert.assertEquals
import org.junit.Test

open class ForEachLoopTest : Parser() {

    val a: Tr.CompilationUnit by lazy { parse("""
            public class A {
                public void test() {
                    for(Integer n: new Integer[] { 0, 1 }) {
                    }
                }
            }
        """)
    }

    private val forEachLoop by lazy { a.firstMethodStatement() as Tr.ForEachLoop }

    @Test
    fun format() {
        assertEquals("for(Integer n: new Integer[] { 0, 1 }) {\n}", forEachLoop.printTrimmed())
    }

    @Test
    fun statementTerminatorForSingleLineForLoops() {
        val a = parse("""
            public class A {
                Integer[] n;
                public void test() {
                    for(Integer i : n) test();
                }
            }
        """)

        val forLoop = a.classes[0].methods[0].body!!.statements[0] as Tr.ForEachLoop
        assertEquals("for(Integer i : n) test();", forLoop.printTrimmed())
    }
}