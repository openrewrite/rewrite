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
package com.netflix.rewrite.tree.visitor.refactor

import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.parse.OpenJdkParser
import com.netflix.rewrite.parse.Parser
import com.netflix.rewrite.tree.Tr
import org.junit.Test
import java.util.*

class TransformVisitorTest : Parser by OpenJdkParser() {
    @Test
    fun multipleTransformations() {
        val a = parse("""
            public class A {
                public void test() {
                }
            }
        """)

        val method = a.classes[0].methods[0]

        val changeMethodName = { to: String ->
            object : RefactorVisitor<Tr.MethodDecl>() {
                override fun getRuleName(): String = "rename s"

                override fun visitMethod(method: Tr.MethodDecl): MutableList<AstTransform<Tr.MethodDecl>> =
                        transform { m: Tr.MethodDecl -> m.withName(m.name.withName(to)) }
            }
        }

        assertRefactored(a.refactor()
                .run(method, changeMethodName("test2"))
                .run(method, changeMethodName("test3"))
                .fix(),
                """
                    public class A {
                        public void test3() {
                        }
                    }
                """.trimIndent())
    }
}
