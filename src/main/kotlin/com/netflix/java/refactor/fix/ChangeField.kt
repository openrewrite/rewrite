package com.netflix.java.refactor.fix

import com.netflix.java.refactor.*
import com.sun.source.tree.VariableTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context

data class ChangeField(val clazz: String, val tx: RefactorTransaction) : FixingOperation {
    var refactorTargetType: String? = null

    fun refactorType(clazz: Class<*>): ChangeField {
        refactorTargetType = clazz.name
        return this
    }

    fun done(): RefactorTransaction {
        if (tx.autoCommit)
            tx.commit()
        return tx
    }

    override fun scanner() =
            if (refactorTargetType != null) {
                IfThenScanner(
                        ifFixesResultFrom = ChangeFieldScanner(this),
                        then = arrayOf(
                                AddImport(refactorTargetType!!).scanner(),
                                RemoveImport(clazz).scanner()
                        )
                )
            } else ChangeFieldScanner(this)
}

class ChangeFieldScanner(val op: ChangeField) : FixingScanner() {
    override fun visitVariable(node: VariableTree, context: Context): List<RefactorFix>? {
        val decl = node as JCTree.JCVariableDecl
        if(op.refactorTargetType is String && decl.type.toString() == op.clazz) {
            return listOf(decl.vartype.replace(className(op.refactorTargetType!!)))
        }
        return super.visitVariable(node, context)
    }
}