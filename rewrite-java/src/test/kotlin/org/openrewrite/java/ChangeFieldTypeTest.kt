/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

interface ChangeFieldTypeTest {
    companion object {
        private val a = """
            import java.util.List;
            public class A {
               List collection;
            }
        """.trimIndent()

        private val refactored = """
            import java.util.Collection;
            
            public class A {
               Collection collection;
            }
        """.trimIndent()
    }

    @Disabled("flaky")
    @Test
    fun changeFieldTypeDeclarative(jp: JavaParser) {
        val cu = jp.parse(a)

        val fixed = cu.refactor()
                .visit(ChangeFieldType().apply { setType("java.util.List"); setTargetType("java.util.Collection") })
                .fix().fixed

        assertRefactored(fixed, refactored)
    }

    @Test
    fun changeFieldType(jp: JavaParser) {
        val cu = jp.parse(a)

        val fixed = cu.refactor()
                .visit(ChangeFieldType.Scoped(cu.classes[0].findFields("java.util.List")[0], "java.util.Collection"))
                .fix().fixed

        assertRefactored(fixed, refactored)
    }
}
