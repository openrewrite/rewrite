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
package org.openrewrite.java.search

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaParser

interface FindInheritedFieldsTest {
    @Test
    fun findInheritedField(jp: JavaParser) {
        val a = """
            import java.util.*;
            public class A {
               protected List list;
               private Set set;
            }
        """

        val b = jp.parse(InMemoryExecutionContext { t: Throwable ->
            fail<Any>("Failed to run parse sources or recipe", t)
        }, "public class B extends A { }", a)[0]

        assertThat(FindInheritedFields.find(b.classes[0], "java.util.List").firstOrNull()?.name)
            .isEqualTo("list")

        assertThat(FindInheritedFields.find(b.classes[0], "java.util.Collection").firstOrNull()?.name)
            .isEqualTo("list")

        // the Set field is not considered to be inherited because it is private
        assertThat(FindInheritedFields.find(b.classes[0], "java.util.Set")).isEmpty()
    }

    @Test
    fun findArrayOfType(jp: JavaParser) {
        val a = """
            public class A {
               String[] s;
            }
        """

        val b = jp.parse(InMemoryExecutionContext { t: Throwable ->
            fail<Any>("Failed to run parse sources or recipe", t)
        }, "public class B extends A { }", a)[0]

        assertThat(FindInheritedFields.find(b.classes[0], "java.lang.String").firstOrNull()?.name)
            .isEqualTo("s")

        assertThat(FindInheritedFields.find(b.classes[0], "java.util.Set")).isEmpty()
    }
}
