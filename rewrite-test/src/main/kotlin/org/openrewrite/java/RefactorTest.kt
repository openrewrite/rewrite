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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Formatting
import org.openrewrite.Refactor
import org.openrewrite.SourceFile
import org.openrewrite.Tree.randomId
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import java.net.URI

class RefactorTest {
    class RefactorTestException : RuntimeException("")

    val cu = J.CompilationUnit(
            randomId(),
            URI.create("A.java"),
            listOf(),
            null,
            listOf(),
            listOf(),
            Formatting.EMPTY,
            listOf()
    )

    private val throwingVisitor = object : JavaRefactorVisitor() {
        override fun visitCompilationUnit(cu: J.CompilationUnit?): J {
            throw RefactorTestException()
        }
    }

    @Test
    fun throwsEagerly() {
        assertThrows(RefactorTestException::class.java) {
            Refactor(true)
                    .visit(throwingVisitor)
                    .fix(listOf(cu))
        }
    }

    @Test
    fun suppressesExceptions() {
        Refactor()
                .visit(throwingVisitor)
                .fix(listOf(cu))
    }

    @Disabled("https://github.com/openrewrite/rewrite/issues/60")
    @Test
    fun canDelete() {
        val deletingVisitor = object : JavaIsoRefactorVisitor() {
            override fun visitCompilationUnit(cu: J.CompilationUnit?): J.CompilationUnit? {
                return null
            }
        }
        val results = Refactor()
                .visit(deletingVisitor)
                .fix(listOf(cu))

        assertEquals(1, results.size)
        val result = results.first()
        assertNotNull(result.original)
        assertNull(result.fixed)
    }

    @Test
    fun canGenerate() {
        val cuToGenerate = J.CompilationUnit(
                randomId(),
                URI.create("A.java"),
                listOf(),
                null,
                listOf(),
                listOf(),
                Formatting.EMPTY,
                listOf()
        )
        val generatingVisitor = object : JavaIsoRefactorVisitor() {
            var generationComplete = false;
            override fun generate(): MutableCollection<SourceFile> {
                return if (generationComplete) {
                    mutableListOf()
                } else {
                    generationComplete = true
                    mutableListOf(cuToGenerate)
                }
            }
        }
        val results = Refactor()
                .visit(generatingVisitor)
                .fix(listOf(cu))
        assertEquals(1, results.size, "The only change returned should be the generated file")
        val result = results.first()
        assertNull(result.original, "The generated file should have no \"original\"")
        assertNotNull(result.fixed)
        val resultCu = (result.fixed as J.CompilationUnit)
        assertEquals(cuToGenerate.id, resultCu.id)
    }

    @Test
    fun multipleVisitorsHaveCumulativeEffects() {
        val addClassDecl = object : JavaIsoRefactorVisitor() {
            override fun visitCompilationUnit(compilationUnit : J.CompilationUnit?): J.CompilationUnit {
                var cu = super.visitCompilationUnit(compilationUnit);
                if(cu.classes.size == 0) {
                    cu = cu.withClasses(listOf(J.ClassDecl(
                            randomId(),
                            emptyList(),
                            emptyList(),
                            J.ClassDecl.Kind.Class(randomId(), Formatting.EMPTY),
                            J.Ident.buildClassName("Foo"),
                            null,
                            null,
                            null,
                            J.Block(randomId(), null, emptyList(), Formatting.EMPTY, J.Block.End(randomId(), Formatting.EMPTY)),
                            JavaType.Class.build("Foo"),
                            Formatting.EMPTY
                    )))
                }
                return cu
            }
        }
        val addMethod = object : JavaIsoRefactorVisitor() {
            override fun visitClassDecl(classDecl: J.ClassDecl?): J.ClassDecl {
                var cd = super.visitClassDecl(classDecl)
                if(cd.methods.size == 0) {
                    cd = cd.withBody(cd.body.withStatements(listOf(
                            J.MethodDecl(
                                    randomId(),
                                    emptyList(),
                                    emptyList(),
                                    null,
                                    JavaType.Primitive.Void.toTypeTree(),
                                    J.Ident.build(randomId(), "bar", null, Formatting.EMPTY),
                                    J.MethodDecl.Parameters(randomId(), emptyList(), Formatting.EMPTY),
                                    null,
                                    J.Block(randomId(), null, emptyList(), Formatting.EMPTY, J.Block.End(randomId(), Formatting.EMPTY)),
                                    null,
                                    Formatting.EMPTY
                            )
                    )))
                }
                return cd
            }
        }

        val results = Refactor(true)
                .visit(addClassDecl, addMethod)
                .fix(listOf(cu))
        assertEquals(1, results.size)
        val result = results.first().fixed as J.CompilationUnit

        assertEquals(1, result.classes.size, "addClassDecl should have added a class declaration")
        assertEquals(1, result.classes.first().methods.size, "addMethod should have added a method declaration")
    }
}
