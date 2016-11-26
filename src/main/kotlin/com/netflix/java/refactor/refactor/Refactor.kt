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
package com.netflix.java.refactor.refactor

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.ast.visitor.FormatVisitor
import com.netflix.java.refactor.ast.visitor.TransformVisitor
import com.netflix.java.refactor.refactor.op.*
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.*

class Refactor(val original: Tr.CompilationUnit) {
    private val ops = ArrayList<RefactorVisitor>()

    // -------------
    // Compilation Unit Refactoring
    // -------------

    @JvmOverloads
    fun addImport(clazz: Class<*>, staticMethod: String? = null): Refactor =
        addImport(clazz.name, staticMethod)

    @JvmOverloads
    fun addImport(clazz: String, staticMethod: String? = null): Refactor {
        ops.add(AddImport(clazz, staticMethod))
        return this
    }

    fun removeImport(clazz: Class<*>): Refactor =
            removeImport(clazz.name)

    fun removeImport(clazz: String): Refactor {
        ops.add(RemoveImport(clazz))
        return this
    }

    fun changeType(from: Class<*>, to: Class<*>): Refactor =
            changeType(from.name, to.name)

    fun changeType(from: String, to: String): Refactor {
        ops.add(ChangeType(from, to))
        ops.add(AddImport(to, onlyIfReferenced = true))
        ops.add(RemoveImport(from))
        return this
    }

    // -------------
    // Class Declaration Refactoring
    // -------------

    @JvmOverloads
    fun addField(target: Tr.ClassDecl, clazz: Class<*>, name: String, init: String? = null) =
        addField(target, clazz.name, name, init)

    @JvmOverloads
    fun addField(target: Tr.ClassDecl, clazz: String, name: String, init: String? = null): Refactor {
        ops.add(AddField(listOf(Tr.VariableDecls.Modifier.Private(Formatting.Reified("", " "))),
                target, clazz, name, init))
        ops.add(AddImport(clazz))
        return this
    }

    // -------------
    // Field Refactoring
    // -------------

    fun changeFieldType(targets: Iterable<Tr.VariableDecls>, toType: String): Refactor {
        targets.forEach { target ->
            ops.add(ChangeFieldType(target, toType))
            target.typeExpr.type?.asClass()?.let { ops.add(RemoveImport(it.fullyQualifiedName)) }
        }

        if(targets.any())
            ops.add(AddImport(toType))

        return this
    }

    fun changeFieldType(target: Tr.VariableDecls, toType: Class<*>) =
            changeFieldType(target, toType.name)

    fun changeFieldType(target: Tr.VariableDecls, toType: String) =
            changeFieldType(listOf(target), toType)

    fun changeFieldName(targets: Iterable<Tr.VariableDecls>, toName: String): Refactor {
        targets.forEach { ops.add(ChangeFieldName(it, toName)) }
        return this
    }

    fun changeFieldName(target: Tr.VariableDecls, toName: String) =
            changeFieldName(listOf(target), toName)

    fun delete(targets: Iterable<Tr.VariableDecls>): Refactor {
        targets.forEach { target ->
            ops.add(DeleteField(target))
            target.typeExpr.type?.asClass()?.let { ops.add(RemoveImport(it.fullyQualifiedName)) }
        }
        return this
    }

    fun delete(target: Tr.VariableDecls) = delete(listOf(target))

    // -------------
    // Method Refactoring
    // -------------

    fun changeMethodName(targets: Iterable<Tr.MethodInvocation>, toName: String): Refactor {
        targets.forEach { ops.add(ChangeMethodName(it, toName)) }
        return this
    }

    fun changeMethodName(target: Tr.MethodInvocation, toName: String) =
            changeMethodName(listOf(target), toName)

    /**
     * Change to a static method invocation on <code>toClass</code>
     */
    fun changeMethodTargetToStatic(targets: Iterable<Tr.MethodInvocation>, toClass: String): Refactor {
        targets.forEach { target ->
            ops.add(ChangeMethodTargetToStatic(target, toClass))
            target.declaringType?.fullyQualifiedName?.let { ops.add(RemoveImport(it)) }
        }

        if(targets.any())
            ops.add(AddImport(toClass))

        return this
    }

    /**
     * Change to a static method invocation on <code> toClass
     */
    fun changeMethodTargetToStatic(target: Tr.MethodInvocation, toClass: String) =
            changeMethodTargetToStatic(listOf(target), toClass)

    /**
     * Change to a static method invocation on <code>toClass</code>
     */
    fun changeMethodTargetToStatic(target: Tr.MethodInvocation, toClass: Class<*>) =
        changeMethodTargetToStatic(target, toClass.name)

    @JvmOverloads
    fun changeMethodTarget(targets: Iterable<Tr.MethodInvocation>, namedVar: String, type: Type.Class? = null): Refactor {
        targets.forEach { target ->
            ops.add(ChangeMethodTargetToVariable(target, namedVar, type))

            // if the original is a static method invocation, the import on it's type may no longer be needed
            target.declaringType?.fullyQualifiedName?.let { ops.add(RemoveImport(it)) }
        }

        return this
    }

    fun changeMethodTarget(target: Tr.MethodInvocation, namedVar: Tr.VariableDecls.NamedVar) =
            changeMethodTarget(target, namedVar.simpleName, namedVar.type.asClass())

    @JvmOverloads
    fun changeMethodTarget(target: Tr.MethodInvocation, namedVar: String, type: Type.Class? = null) =
            changeMethodTarget(listOf(target), namedVar, type)

    fun insertArgument(targets: Iterable<Tr.MethodInvocation>, pos: Int, source: String): Refactor {
        targets.forEach { ops.add(InsertMethodArgument(it, pos, source)) }
        return this
    }

    fun insertArgument(target: Tr.MethodInvocation, pos: Int, source: String) =
            insertArgument(listOf(target), pos, source)

    fun deleteArgument(target: Tr.MethodInvocation, pos: Int) =
            deleteArgument(listOf(target), pos)

    fun deleteArgument(targets: Iterable<Tr.MethodInvocation>, pos: Int): Refactor {
        targets.forEach { ops.add(DeleteMethodArgument(it, pos)) }
        return this
    }

    fun reorderArguments(target: Tr.MethodInvocation, vararg byArgumentNames: String): ReorderMethodArguments {
        val reorderOp = ReorderMethodArguments(target, *byArgumentNames)
        ops.add(reorderOp)
        return reorderOp
    }

    // -------------
    // Expression Refactoring
    // -------------

    fun changeLiteral(targets: Iterable<Expression>, transform: (Any?) -> Any?): Refactor {
        targets.forEach { ops.add(ChangeLiteral(it, transform)) }
        return this
    }

    fun changeLiteral(target: Expression, transform: (Any?) -> Any?): Refactor =
            changeLiteral(listOf(target), transform)

    /**
    * @return Transformed version of the AST after changes are applied
    */
    fun fix(): Tr.CompilationUnit {
        val fixed = ops.fold(original) { acc, op ->
            // by transforming the AST for each op, we allow for the possibility of overlapping changes
            TransformVisitor(op.visit(acc)).visit(acc) as Tr.CompilationUnit
        }
        FormatVisitor().visit(fixed)
        return fixed
    }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit
     */
    @JvmOverloads
    fun diff(relativeTo: Path? = null) = InMemoryDiffEntry(original.sourcePath, relativeTo, original.print(), fix().print()).diff

    internal class InMemoryDiffEntry(filePath: Path, relativeTo: Path?, old: String, new: String): DiffEntry() {
        private val repo = InMemoryRepository.Builder().build()
        private val relativePath = relativeTo?.let { filePath.relativize(relativeTo) } ?: filePath

        init {
            changeType = ChangeType.MODIFY
            oldPath = relativePath.toString()
            newPath = relativePath.toString()

            val inserter = repo.objectDatabase.newInserter()
            oldId = inserter.insert(Constants.OBJ_BLOB, old.toByteArray()).abbreviate(40)
            newId = inserter.insert(Constants.OBJ_BLOB, new.toByteArray()).abbreviate(40)
            inserter.flush()

            oldMode = FileMode.REGULAR_FILE
            newMode = FileMode.REGULAR_FILE
            repo.close()
        }

        val diff: String by lazy {
            if(oldId == newId)
                ""
            else {
                val patch = ByteArrayOutputStream()
                val formatter = DiffFormatter(patch)
                formatter.setRepository(repo)
                formatter.format(this)
                String(patch.toByteArray())
            }
        }
    }
}