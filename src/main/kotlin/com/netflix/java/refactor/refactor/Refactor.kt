package com.netflix.java.refactor.refactor

import com.netflix.java.refactor.ast.Expression
import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.asClass
import com.netflix.java.refactor.ast.visitor.FormatVisitor
import com.netflix.java.refactor.ast.visitor.TransformVisitor
import com.netflix.java.refactor.refactor.op.*
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import java.io.ByteArrayOutputStream
import java.util.*

class Refactor(val cu: Tr.CompilationUnit) {
    private val ops = ArrayList<RefactorVisitor>()

    // -------------
    // Compilation Unit Refactoring
    // -------------

    fun addImport(clazz: Class<*>, staticMethod: String? = null): Refactor {
        addImport(clazz.name, staticMethod)
        return this
    }

    fun addImport(clazz: String, staticMethod: String? = null): Refactor {
        ops.add(AddImport(cu, clazz, staticMethod))
        return this
    }

    fun removeImport(clazz: Class<*>): Refactor {
        removeImport(clazz.name)
        return this
    }

    fun removeImport(clazz: String): Refactor {
        ops.add(RemoveImport(cu, clazz))
        return this
    }

    // -------------
    // Class Declaration Refactoring
    // -------------

    fun addField(target: Tr.ClassDecl, clazz: Class<*>, name: String, init: String?) {
        addField(target, clazz.name, name, init)
    }

    fun addField(target: Tr.ClassDecl, clazz: Class<*>, name: String) {
        addField(target, clazz.name, name, null)
    }

    fun addField(target: Tr.ClassDecl, clazz: String, name: String) {
        addField(target, clazz, name, null)
    }

    fun addField(target: Tr.ClassDecl, clazz: String, name: String, init: String?) {
        ops.add(AddField(cu, listOf(Tr.VariableDecls.Modifier.Private(Formatting.Reified("", " "))),
                target, clazz, name, init))
        ops.add(AddImport(cu, clazz))
    }

    // -------------
    // Field Refactoring
    // -------------

    fun changeType(target: Tr.VariableDecls, toType: Class<*>) {
        changeType(target, toType.name)
    }

    fun changeType(target: Tr.VariableDecls, toType: String) {
        ops.add(ChangeFieldType(cu, target, toType))
        ops.add(AddImport(cu, toType))
        target.typeExpr.type?.asClass()?.let { ops.add(RemoveImport(cu, it.fullyQualifiedName)) }
    }

    fun changeName(target: Tr.VariableDecls, toName: String) {
        ops.add(ChangeFieldName(target, toName))
    }

    fun delete(target: Tr.VariableDecls) {
        ops.add(DeleteField(target))
        target.typeExpr.type?.asClass()?.let { ops.add(RemoveImport(cu, it.fullyQualifiedName)) }
    }

    // -------------
    // Method Refactoring
    // -------------

    fun changeName(target: Tr.MethodInvocation, toName: String) {
        ops.add(ChangeMethodName(target, toName))
    }

    /**
     * Change to a static method invocation on <code>toClass</code>
     */
    fun changeTarget(target: Tr.MethodInvocation, toClass: String) {
        ops.add(ChangeMethodTargetToStatic(cu, target, toClass))
        ops.add(AddImport(cu, toClass))
        target.declaringType?.fullyQualifiedName?.let { ops.add(RemoveImport(cu, it)) }
    }

    /**
     * Change to a static method invocation on <code>toClass</code>
     */
    fun changeTarget(target: Tr.MethodInvocation, toClass: Class<*>) {
        changeTarget(target, toClass.name)
    }

    fun changeTarget(target: Tr.MethodInvocation, namedVar: Tr.VariableDecls.NamedVar) {
        ops.add(ChangeMethodTargetToVariable(target, namedVar))

        // if the original is a static method invocation, the import on it's type may no longer be needed
        target.declaringType?.fullyQualifiedName?.let { ops.add(RemoveImport(cu, it)) }
    }

    fun insertArgument(target: Tr.MethodInvocation, pos: Int, source: String) {
        ops.add(InsertMethodArgument(cu, target, pos, source))
    }

    fun reorderArguments(target: Tr.MethodInvocation, vararg byArgumentNames: String): ReorderMethodArguments {
        val reorderOp = ReorderMethodArguments(target, *byArgumentNames)
        ops.add(reorderOp)
        return reorderOp
    }

    // -------------
    // Expression Refactoring
    // -------------

    fun changeLiterals(target: Expression, transform: (Any?) -> Any?): Refactor {
        ops.add(ChangeLiteralArgument(target, transform))
        return this
    }

    /**
    * @return Transformed version of the AST after changes are applied
    */
    fun fix(): Tr.CompilationUnit {
        val fixed = ops.fold(cu) { acc, op ->
            // by transforming the AST for each op, we allow for the possibility of overlapping changes
            TransformVisitor(op.visit(acc)).visit(acc) as Tr.CompilationUnit
        }
        FormatVisitor().visit(fixed)
        return fixed
    }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit
     */
    fun diff() = InMemoryDiffEntry(cu.sourcePath, cu.print(), fix().print()).diff

    internal class InMemoryDiffEntry(filePath: String, old: String, new: String): DiffEntry() {
        private val repo = InMemoryRepository.Builder().build()

        init {
            changeType = ChangeType.MODIFY
            oldPath = filePath
            newPath = filePath

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