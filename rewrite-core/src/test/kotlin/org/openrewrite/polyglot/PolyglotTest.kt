/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.polyglot

import org.assertj.core.api.Assertions.assertThat
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext

class PolyglotTest {

    private val parser: PolyglotParser = PolyglotParser()

    @Language("js")
    private val testConstructor = """
        var TestInstance = function() { 
            return { hello: 'world' }; 
        }
    """.trimIndent()

    @Test
    fun `must recognize instantiable values`() {
        val ex = InMemoryExecutionContext()

        val srcs = parser.parse(ex, Source.newBuilder("js", testConstructor, "TestInstance").build())

        val visitor = object : PolyglotVisitor<ExecutionContext>() {
            override fun visitInstantiable(instantiable: Polyglot.Instantiable, ctx: ExecutionContext): Polyglot {
                return instantiable.instantiate()
            }
        }

        val objs = srcs.map { visitor.visit(it, ex) }

        assertThat(objs).isNotEmpty
    }
}