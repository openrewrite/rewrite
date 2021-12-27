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
import org.openrewrite.java.TypeValidator.InvalidTypeResult
import org.openrewrite.java.tree.J

/**
 * Produces a report about missing type attributions within a CompilationUnit.
 */
class TypeValidator(
    private val options: ValidationOptions = defaultOptions
) : JavaIsoVisitor<MutableList<InvalidTypeResult>>() {

    data class InvalidTypeResult(
        val cursor: Cursor,
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
            fun builder(init: Builder.()->Unit): ValidationOptions {
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
            if(report.isNotEmpty()) {

                val reportText = report.asSequence()
                        .map { """
                             |  ${it.message}
                             |    At : ${it.cursor}
                             |    AST:
                             ${it.cursor.getValue<J>().printTrimmed(it.cursor).prependIndent("|      ")}
                             |
                        """.trimMargin() }
                        .joinToString("\n")
                fail("AST contains missing or invalid type information: \n$reportText")
            }
        }

        /**
         * Convenience method for creating an InvalidType result without having to manually specify anything except the message.
         * And a convenient place to put a breakpoint if you want to catch an invalid type in the debugger.
         */
        private fun JavaVisitor<*>.invalidTypeResult(message: String) = InvalidTypeResult(cursor, message)
    }

    override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: MutableList<InvalidTypeResult>): J.ClassDeclaration {
        val c = super.visitClassDeclaration(classDecl, p)
        if(!options.classDeclarations) {
            return c
        }

        val t = c.type
        if(t == null) {
            p.add(invalidTypeResult("J.ClassDeclaration type is null"))
            return c
        }
        if(c.kind.name != t.kind.name) {
            p.add(invalidTypeResult("J.ClassDeclaration kind \"${c.kind}\" does not match the kind in its type information \"${t.kind}\""))
        }
        val cu = cursor.firstEnclosing(J.CompilationUnit::class.java)!!
        val pack = cu.packageDeclaration
        if(pack != null && !t.packageName.equals(pack.expression.printTrimmed(cursor))) {
            p.add(invalidTypeResult("J.ClassDeclaration package \"${t.packageName}\" does not match the enclosing J.CompilationUnit's package declaration \"${pack.expression.printTrimmed(cursor)}\""))
        }

        return c
    }

    override fun visitMethodInvocation(method: J.MethodInvocation, p: MutableList<InvalidTypeResult>): J.MethodInvocation {
        val m = super.visitMethodInvocation(method, p)
        if(!options.methodInvocations) {
            return m
        }
        val mt = method.methodType
        if(mt == null) {
            p.add(invalidTypeResult("J.MethodInvocation type is null"))
            return m
        }
        if(!m.simpleName.equals(mt.name) && m.methodType?.isConstructor != true) {
            p.add(invalidTypeResult("J.MethodInvocation name \"${m.simpleName}\" does not match the name in its type information \"${mt.name}\""))
        }

        return m
    }

    override fun visitMethodDeclaration(method: J.MethodDeclaration, p: MutableList<InvalidTypeResult>): J.MethodDeclaration {
        val m = super.visitMethodDeclaration(method, p)
        if(!options.methodDeclarations) {
            return m
        }
        val mt = m.methodType
        if(mt == null) {
            p.add(invalidTypeResult("J.MethodDeclaration type is null"))
            return m
        }
        if(!m.simpleName.equals(mt.name) && !m.isConstructor) {
            p.add(invalidTypeResult("J.MethodDeclaration name \"${m.simpleName}\" does not match the name in its type information \"${mt.name}\""))
        }
        return m
    }

    override fun visitIdentifier(identifier: J.Identifier, p: MutableList<InvalidTypeResult>): J.Identifier {
        val i = super.visitIdentifier(identifier, p)
        if(!options.identifiers) {
            return i
        }
        val t = i.type

        // The non-nullability of J.Identifier.getType() in our AST is a white lie
        // J.Identifier.getType() is allowed to be null in places where the containing AST element fully specifies the type
        @Suppress("SENSELESS_COMPARISON")
        if(t == null) {
            if(!i.isAllowedToHaveNullType()) {
                p.add(invalidTypeResult("J.Identifier type is null"))
            }
            return i
        }
        return i
    }

    private fun J.Identifier.isAllowedToHaveNullType() = inPackageDeclaration() || inImport() || isClassName()
            || isMethodName() || isMethodInvocationName() || isFieldAccess() || isBeingDeclared() || isParameterizedType()
            || isNewClass() || isTypeParameter() || isMemberReference() || isCaseLabel() || isLabel() || isAnnotationField()

    private fun inPackageDeclaration(): Boolean {
        return cursor.firstEnclosing(J.Package::class.java) != null
    }

    private fun inImport(): Boolean {
        return cursor.firstEnclosing(J.Import::class.java) != null
    }

    private fun isClassName(): Boolean {
        return cursor.parent!!.getValue<Any>() is J.ClassDeclaration
    }

    private fun isMethodName(): Boolean {
        return cursor.parent!!.getValue<Any>() is J.MethodDeclaration
    }

    private fun isMethodInvocationName(): Boolean {
        return cursor.parent!!.getValue<Any>() is J.MethodInvocation
    }

    private fun J.Identifier.isFieldAccess(): Boolean {
        val parent = cursor.firstEnclosing(J.FieldAccess::class.java)
        return parent is J.FieldAccess && (parent.name == this || parent.target == this)
    }

    private fun J.Identifier.isBeingDeclared(): Boolean {
        val parent = cursor.firstEnclosing(J.VariableDeclarations.NamedVariable::class.java)
        return parent is J.VariableDeclarations.NamedVariable && parent.name == this
    }

    private fun J.Identifier.isParameterizedType(): Boolean {
        val parent = cursor.firstEnclosing(J.ParameterizedType::class.java)
        return parent is J.ParameterizedType && parent.clazz == this
    }

    private fun J.Identifier.isNewClass(): Boolean {
        val parent = cursor.firstEnclosing(J.NewClass::class.java)
        return parent is J.NewClass && parent.clazz == this
    }

    private fun isTypeParameter(): Boolean {
        return cursor.parent!!.getValue<Any>() is J.TypeParameter
    }

    private fun isMemberReference(): Boolean {
        return cursor.firstEnclosing(J.MemberReference::class.java) != null
    }

    private fun isCaseLabel(): Boolean {
        return cursor.parent!!.getValue<Any>() is J.Case
    }

    private fun isLabel(): Boolean {
        return cursor.firstEnclosing(J.Label::class.java) != null
    }

    private fun J.Identifier.isAnnotationField(): Boolean {
        val parent = cursor.parent!!.getValue<Any>()
        return parent is J.Assignment && parent.variable == this && cursor.firstEnclosing(J.Annotation::class.java) != null
    }
}
