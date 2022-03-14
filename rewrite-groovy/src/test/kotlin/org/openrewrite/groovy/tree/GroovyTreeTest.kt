/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.groovy.GroovyVisitor
import org.openrewrite.internal.StringUtils

interface GroovyTreeTest {
    fun assertParsePrintAndProcess(@Language("groovy") code: String, withAst: (G.CompilationUnit)->Unit = {}) {
        val trimmed = StringUtils.trimIndent(code)
        val cu = GroovyParser.builder().build().parse(
            InMemoryExecutionContext { t: Throwable -> throw t },
            trimmed
        ).iterator().next()
        val processed = GroovyVisitor<Any>().visit(cu, Any())
        assertThat(processed).`as`("Parsing is idempotent").isSameAs(cu)
        assertThat(cu.printAll()).`as`("Prints back to the original code").isEqualTo(trimmed)
        assertThat(cu).`as`("Snippet expected to parse into a G.CompilationUnit").isInstanceOf(G.CompilationUnit::class.java)
        withAst.invoke(cu as G.CompilationUnit)
    }
}
