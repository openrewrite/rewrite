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
package com.netflix.rewrite.refactor

import com.netflix.rewrite.ast.*
import com.netflix.rewrite.ast.visitor.FormatVisitor
import com.netflix.rewrite.ast.visitor.RetrieveTreeVisitor
import com.netflix.rewrite.ast.visitor.TransformVisitor
import com.netflix.rewrite.refactor.op.*
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

open class Refactor(val original: Tr.CompilationUnit) {
    /**
     * The operation's target may change if another operation transforms the original target first
     */
    private data class RefactorOperation(var id: Long, val visitor: RefactorVisitor<*>)
    private val ops = ArrayList<RefactorOperation>()

    private fun addOp(target: Tree, visitor: RefactorVisitor<*>) {
        ops.add(RefactorOperation(target.id, visitor))
    }

    private fun addOp(visitor: RefactorVisitor<*>) {
        ops.add(RefactorOperation(original.id, visitor))
    }

    // -------------
    // Compilation Unit Refactoring
    // -------------

    @JvmOverloads
    fun addImport(clazz: Class<*>, staticMethod: String? = null): Refactor =
        addImport(clazz.name, staticMethod)

    @JvmOverloads
    fun addImport(clazz: String, staticMethod: String? = null): Refactor {
        addOp(AddImport(clazz, staticMethod))
        return this
    }

    fun removeImport(clazz: Class<*>): Refactor = removeImport(clazz.name)

    fun removeImport(clazz: String): Refactor {
        addOp(RemoveImport(clazz))
        return this
    }

    fun changeType(from: Class<*>, to: Class<*>): Refactor = changeType(from.name, to.name)

    fun changeType(from: String, to: String): Refactor {
        addOp(ChangeType(from, to))
        addOp(AddImport(to, onlyIfReferenced = true))
        addOp(RemoveImport(from))
        return this
    }

    fun run(t: Tree, visitor: RefactorVisitor<*>): Refactor {
        addOp(t, visitor)
        return this
    }

    fun run(visitor: RefactorVisitor<*>): Refactor {
        addOp(visitor)
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
        addOp(target, AddField(listOf(Tr.Modifier.Private(format("", " "))), clazz, name, init))
        addOp(AddImport(clazz))
        return this
    }

    // -------------
    // Field Refactoring
    // -------------

    fun changeFieldType(targets: Iterable<Tr.VariableDecls>, toType: String): Refactor {
        targets.forEach { target ->
            addOp(target, ChangeFieldType(toType))
            target.typeExpr.type?.asClass()?.let { addOp(RemoveImport(it.fullyQualifiedName)) }
        }

        if(targets.any())
            addOp(AddImport(toType))

        return this
    }

    fun changeFieldType(target: Tr.VariableDecls, toType: Class<*>) =
            changeFieldType(target, toType.name)

    fun changeFieldType(target: Tr.VariableDecls, toType: String) =
            changeFieldType(listOf(target), toType)

    fun changeFieldName(targets: Iterable<Tr.VariableDecls>, toName: String): Refactor {
        targets.forEach { target -> addOp(target, ChangeFieldName(toName)) }
        return this
    }

    fun changeFieldName(target: Tr.VariableDecls, toName: String) =
            changeFieldName(listOf(target), toName)

    fun deleteField(targets: Iterable<Tr.VariableDecls>): Refactor {
        targets.groupBy { original.cursor(it.id)!!.enclosingClass() }
                .forEach { clazz, variables ->
                    addOp(clazz!!, DeleteField(variables))
                    variables.forEach { (_, _, typeExpr) ->
                        typeExpr.type?.asClass()?.let { addOp(RemoveImport(it.fullyQualifiedName)) }
                    }
                }

        return this
    }

    fun deleteField(target: Tr.VariableDecls) = deleteField(listOf(target))

    // -------------
    // Method Refactoring
    // -------------

    fun changeMethodName(targets: Iterable<Tr.MethodInvocation>, toName: String): Refactor {
        targets.forEach { target -> addOp(target, ChangeMethodName(toName)) }
        return this
    }

    fun changeMethodName(target: Tr.MethodInvocation, toName: String) =
            changeMethodName(listOf(target), toName)

    /**
     * Change to a static method invocation on <code>toClass</code>
     */
    fun changeMethodTargetToStatic(targets: Iterable<Tr.MethodInvocation>, toClass: String): Refactor {
        targets.forEach { target ->
            addOp(target, ChangeMethodTargetToStatic(toClass))
            target.type?.declaringType?.fullyQualifiedName?.let { addOp(RemoveImport(it)) }
        }

        if(targets.any())
            addOp(AddImport(toClass))

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

    fun changeMethodTarget(targets: Iterable<Tr.MethodInvocation>, namedVar: String, type: Type.Class): Refactor {
        targets.forEach { target ->
            addOp(target, ChangeMethodTargetToVariable(namedVar, type))

            // if the original is a static method invocation, the import on it's type may no longer be needed
            target.type?.declaringType?.fullyQualifiedName?.let { addOp(RemoveImport(it)) }
        }

        return this
    }

    fun changeMethodTarget(target: Tr.MethodInvocation, namedVar: Tr.VariableDecls.NamedVar) =
            changeMethodTarget(target, namedVar.simpleName, namedVar.type.asClass()!!)

    fun changeMethodTarget(target: Tr.MethodInvocation, namedVar: String, type: Type.Class) =
            changeMethodTarget(listOf(target), namedVar, type)

    fun insertArgument(targets: Iterable<Tr.MethodInvocation>, pos: Int, source: String): Refactor {
        targets.forEach { target -> addOp(target, InsertMethodArgument(pos, source)) }
        return this
    }

    fun insertArgument(target: Tr.MethodInvocation, pos: Int, source: String) =
            insertArgument(listOf(target), pos, source)

    fun deleteArgument(target: Tr.MethodInvocation, pos: Int) =
            deleteArgument(listOf(target), pos)

    fun deleteArgument(targets: Iterable<Tr.MethodInvocation>, pos: Int): Refactor {
        targets.forEach { target -> addOp(target, DeleteMethodArgument(pos)) }
        return this
    }

    fun reorderArguments(target: Tr.MethodInvocation, vararg byArgumentNames: String): ReorderMethodArguments {
        val reorderOp = ReorderMethodArguments(byArgumentNames.toList())
        addOp(target, reorderOp)
        return reorderOp
    }

    // -------------
    // Expression Refactoring
    // -------------

    fun changeLiteral(targets: Iterable<Expression>, transform: (Any?) -> Any?): Refactor {
        targets.forEach { target -> addOp(target, ChangeLiteral(transform)) }
        return this
    }

    fun changeLiteral(target: Expression, transform: (Any?) -> Any?): Refactor =
            changeLiteral(listOf(target), transform)

    fun stats(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()

        ops.fold(original) { acc, (targetId, visitor) ->
            // by transforming the AST for each op, we allow for the possibility of overlapping changes
            val target = RetrieveTreeVisitor(targetId).visit(acc)
            val transformations = visitor.visit(target)
            val transformed = TransformVisitor(transformations).visit(acc) as Tr.CompilationUnit
            transformations.groupBy { it.name }.forEach { name, transformations ->
                stats.merge(name, transformations.size, Int::plus)
            }
            transformed
        }

        return stats
    }

    /**
     * @return Transformed version of the AST after changes are applied
     */
    fun fix(): Tr.CompilationUnit = ops
            .fold(original) { acc, (targetId, visitor) ->
                val target = RetrieveTreeVisitor(targetId).visit(acc)

                // by transforming the AST for each op, we allow for the possibility of overlapping changes
                TransformVisitor(visitor.visit(target)).visit(acc) as Tr.CompilationUnit
            }
            .let { cu -> TransformVisitor(FormatVisitor().visit(cu)).visit(cu) as Tr.CompilationUnit }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit
     */
    @JvmOverloads
    fun diff(relativeTo: Path? = null) = InMemoryDiffEntry(Paths.get(original.sourcePath), relativeTo, original.print(), fix().print()).diff

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