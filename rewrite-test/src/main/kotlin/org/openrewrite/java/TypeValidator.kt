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
package org.openrewrite.java

import org.junit.jupiter.api.fail
import org.openrewrite.Cursor
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Tree
import org.openrewrite.internal.lang.Nullable
import org.openrewrite.java.TypeValidator.InvalidTypeResult
import org.openrewrite.java.search.FindMissingTypes
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaSourceFile
import org.openrewrite.marker.SearchResult
import java.util.stream.Collectors

/**
 * Produces a report about missing type attributions within a CompilationUnit.
 */
class TypeValidator(
    private val options: ValidationOptions = defaultOptions
) : JavaIsoVisitor<MutableList<InvalidTypeResult>>() {

    data class InvalidTypeResult(
        val cursor: Cursor,
        val tree: Tree,
        val message: String
    )

    data class ValidationOptions(
        val classDeclarations: Boolean = true,
        val identifiers: Boolean = true,
        val methodDeclarations: Boolean = true,
        val methodInvocations: Boolean = true,
    ) {
        companion object {
            class Builder(
                var classDeclarations: Boolean = true,
                var identifiers: Boolean = true,
                var methodDeclarations: Boolean = true,
                var methodInvocations: Boolean = true,
            )

            fun builder(init: Builder.() -> Unit): ValidationOptions {
                val builder = Builder()
                init.invoke(builder)
                return ValidationOptions(
                    classDeclarations = builder.classDeclarations,
                    identifiers = builder.identifiers,
                    methodDeclarations = builder.methodDeclarations,
                    methodInvocations = builder.methodInvocations
                )
            }
        }
    }

    companion object {
        val defaultOptions = ValidationOptions()

        @JvmStatic
        fun analyzeTypes(
            cu: J.CompilationUnit,
            options: ValidationOptions = defaultOptions
        ): List<InvalidTypeResult> {
            val report = mutableListOf<InvalidTypeResult>()
            TypeValidator(options).visit(cu, report)
            return report
        }

        @JvmStatic
        fun assertTypesValid(
            cu: J.CompilationUnit,
            options: ValidationOptions = defaultOptions
        ) {
            val report = analyzeTypes(cu, options)
            if (report.isNotEmpty()) {

                val reportText = report.asSequence()
                    .map {
                        """
                            |  AT:  ${it.cursor.pathAsStream.filter(J::class.java::isInstance).map{ j -> j.javaClass.simpleName }.collect(Collectors.joining("->"))}
                            |       ${it.tree.printTrimmed(it.cursor)}
                        """.trimMargin()
                    }
                    .joinToString("\n")
                fail("AST contains missing or invalid type information: \n$reportText")
            }
        }
    }

    override fun visitJavaSourceFile(cu: JavaSourceFile, p: MutableList<InvalidTypeResult>): JavaSourceFile {
        val sf = FindMissingTypes().visitor.visit(cu, InMemoryExecutionContext())
        return super.visitJavaSourceFile(sf as JavaSourceFile, p)
    }

    override fun visitIdentifier(identifier: J.Identifier, p: MutableList<InvalidTypeResult>): J.Identifier {
        val ident = super.visitIdentifier(identifier, p)
        if (options.identifiers) {
            maybeAddResult(ident, p)
        }
        return ident
    }

    override fun visitMethodInvocation(
        method: J.MethodInvocation,
        p: MutableList<InvalidTypeResult>
    ): J.MethodInvocation {
        val mi = super.visitMethodInvocation(method, p)
        if (options.methodInvocations) {
            maybeAddResult(mi, p)
        }
        return mi
    }

    override fun visitMethodDeclaration(
        method: J.MethodDeclaration,
        p: MutableList<InvalidTypeResult>
    ): J.MethodDeclaration {
        val md = super.visitMethodDeclaration(method, p)
        maybeAddResult(md, p)
        return md
    }

    override fun visitClassDeclaration(
        classDecl: J.ClassDeclaration,
        p: MutableList<InvalidTypeResult>
    ): J.ClassDeclaration {
        val cd = super.visitClassDeclaration(classDecl, p)
        if (options.classDeclarations) {
            maybeAddResult(cd, p)
        }
        return cd
    }

    private fun maybeAddResult(j: J?, p: MutableList<InvalidTypeResult>) {
        val m = j?.markers!!.findFirst(SearchResult::class.java).orElse(null)
        if (m != null) {
            val message: @Nullable String? = m.description
            if (message != null && message.startsWith(FindMissingTypes.markerDescriptionPrefix)) {
                p.add(InvalidTypeResult(cursor, j, message))
            }

        }
    }
}
