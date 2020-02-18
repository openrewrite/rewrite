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

import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.parse.OpenJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Test

open class AddFieldTest : Parser by OpenJdkParser() {
    
    @Test
    fun addFieldDefaultIndent() {
        val a = parse("""
            class A {
            }
        """.trimIndent())

        val fixed = a.refactor()
            .addField(a.classes[0], "java.util.List", "list", "new ArrayList<>()")
            .fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            class A {
                private List list = new ArrayList<>();
            }
        """)
    }

    @Test
    fun addFieldMatchSpaces() {
        val a = parse("""
            import java.util.List;
            
            class A {
              List l;
            }
        """.trimIndent())

        val fixed = a.refactor()
            .addField(a.classes[0], "java.util.List", "list")
            .fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            class A {
              private List list;
              List l;
            }
        """)
    }

    @Test
    fun addFieldMatchTabs() {
        val a = parse("""
            import java.util.List;
            
            class A {
                       List l;
            }
        """.trimIndent())

        val fixed = a.refactor()
            .addField(a.classes[0], "java.util.List", "list")
            .fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            class A {
                       private List list;
                       List l;
            }
        """)
    }
}
