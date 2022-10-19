/*
 * Copyright 2022 the original author or authors.
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.tree.J

/**
 * @author Alex Boyko
 */
interface JavaParserTest : JavaRecipeTest {

    @Test
    fun incompleteAssignment() {

        val source =
            """
           @Deprecated(since=)
           public class A {}
       """.trimIndent()

        val cu = JavaParser.fromJavaVersion().build().parse(source).get(0)

        assertThat(cu.printAll()).isEqualTo(source)

        val newCu = JavaVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext()) as J.CompilationUnit

        assertThat(newCu.printAll()).isEqualTo(source)

    }


    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2313")
    fun annotationCommentWithNoSpaceParsesCorrectly() {

        val source =
            """
                package com.example;
                
                @SuppressWarnings("serial")// fred
                @Deprecated
                public class PersistenceManagerImpl {
                }
            """.trimIndent()

        val cu = JavaParser.fromJavaVersion().build().parse(source).get(0)

        assertThat(cu.printAll()).isEqualTo(source)

        val clazz = cu.classes[0]

        assertThat(clazz.leadingAnnotations).hasSize(2)
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2313")
    fun annotationCommentWithSpaceParsesCorrectly() {

        val source =
            """
                package com.example;
                
                @SuppressWarnings("serial") // fred
                @Deprecated
                public class PersistenceManagerImpl {
                }
            """.trimIndent()

        val cu = JavaParser.fromJavaVersion().build().parse(source).get(0)

        assertThat(cu.printAll()).isEqualTo(source)

        val clazz = cu.classes[0]

        assertThat(clazz.leadingAnnotations).hasSize(2)
    }

}
