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
import org.assertj.core.api.Assertions.fail
import org.intellij.lang.annotations.Language
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.groovy.GroovyVisitor
import org.openrewrite.internal.StringUtils
import org.openrewrite.java.tree.Space

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
        val cu2 = NoTextOutsideOfComments().visitNonNull(cu, 0) as G.CompilationUnit
        if(cu2 !== cu) {
            fail<Any>("Found non-whitespace characters inside of whitespace. Something didn't parse correctly:\n%s",
                cu2.printAll())
        }
        withAst.invoke(cu as G.CompilationUnit)
    }

    class NoTextOutsideOfComments : GroovyVisitor<Int>() {
        override fun visitSpace(space: Space, loc: Space.Location, p: Int): Space {
            var i = 0
            val chars = space.whitespace.toCharArray()
            var inSingleLineComment = false
            var inMultilineComment = false
            while(i < chars.size) {
                val c = chars[i]
                if(inSingleLineComment && c == '\n') {
                    inSingleLineComment = false
                    continue
                }
                if(i < chars.size - 1) {
                    val s = c.toString() + chars[i + 1]
                    when(s) {
                        "//" -> {
                            inSingleLineComment = true
                            i += 2
                            continue
                        }
                        "/*" -> {
                            inMultilineComment = true
                            i += 2
                            continue
                        }
                        "*/" -> {
                            inMultilineComment = false
                            i += 2
                            continue
                        }
                    }
                }
                if(!inSingleLineComment && !inMultilineComment && !Character.isWhitespace(c)) {
                    return space.withWhitespace("/*whitespace ->*/${space.whitespace}/*<-*/")
                }
                i++
            }
            return space
        }
    }
}
