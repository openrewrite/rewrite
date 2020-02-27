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
package org.openrewrite.visitor.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import org.openrewrite.tree.Statement

open class RefactorVisitorTest : Parser() {
    @Test
    fun traversalIsDepthFirst() {
        val a = parse("""
            public class A {
                {
                    for(;;) {
                        String s = "s";
                    }
                }
            }
        """.trimIndent())

        a.refactor().visit(object: RefactorVisitor() {
            override fun getRuleName(): String = "traversal"

            override fun visitStatement(statement: Statement?): MutableList<AstTransform> {
                println(cursor)
                return super.visitStatement(statement)
            }
        }).fix().fixed
    }
}