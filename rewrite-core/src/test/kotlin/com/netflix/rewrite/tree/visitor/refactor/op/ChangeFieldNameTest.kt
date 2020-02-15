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
package com.netflix.rewrite.tree.visitor.refactor.op

import com.netflix.rewrite.asClass
import com.netflix.rewrite.assertRefactored
import org.junit.Test
import com.netflix.rewrite.parse.OpenJdkParser
import com.netflix.rewrite.parse.Parser
import com.netflix.rewrite.tree.Type

open class ChangeFieldNameTest : Parser by OpenJdkParser() {

    @Test
    fun changeFieldName() {
        val a = parse("""
            import java.util.List;
            public class A {
               List collection = null;
            }
        """.trimIndent())

        val fixed = a.refactor()
                .changeFieldName(a.classes[0].type.asClass()!!, "collection", "list")
                .fix()

        assertRefactored(fixed, """
            import java.util.List;
            public class A {
               List list = null;
            }
        """)
    }

    @Test
    fun changeFieldNameReferences() {
        val b = """
            public class B {
               int n;
            }
        """.trimIndent()

        val a = parse("""
            public class A {
                B b = new B();
                {
                    b.n = 1;
                }
            }
        """.trimIndent())

        val fixed = a.refactor()
                .changeFieldName(Type.Class.build("B"), "n", "n1")
                .fix()

        assertRefactored(fixed, """
            public class A {
                B b = new B();
                {
                    b.n1 = 1;
                }
            }
        """)
    }
}
