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
package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class LambdaTest(p: Parser): Parser by p {

    val a by lazy {
        parse("""
            import java.util.function.Function;
            public class A {
                Function<String, String> func = (String s) -> "";
            }
        """)
    }

    val lambda by lazy { a.fields()[0].vars[0].initializer as Tr.Lambda }

    @Test
    fun lambda() {
        assertEquals(1, lambda.params.size)
        assertTrue(lambda.body is Tr.Literal)
    }

    @Test
    fun format() {
        assertEquals("(String s) -> \"\"", lambda.printTrimmed())
    }
}