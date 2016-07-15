package com.netflix.java.refactor.fix

import com.netflix.java.refactor.*
import com.sun.source.tree.VariableTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.util.*

data class ChangeField(val clazz: String, val tx: RefactorTransaction) : FixingOperation {
    var refactorTargetType: String? = null
    var refactorName: String? = null
    var refactorDelete: Boolean = false
    
    fun refactorType(clazz: Class<*>): ChangeField {
        refactorTargetType = clazz.name
        return this
    }

    fun refactorName(name: String): ChangeField {
        refactorName = name
        return this
    }
    
    fun delete(): RefactorTransaction {
        refactorDelete = true
        return done()
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
        
        if(decl.type.toString() == op.clazz) {
            return refactorField(decl)
        }
        
        return super.visitVariable(node, context)
    }

    private fun refactorField(decl: JCTree.JCVariableDecl): ArrayList<RefactorFix> {
        val fixes = ArrayList<RefactorFix>()

        if (op.refactorDelete) {
            fixes.add(decl.delete())
            return fixes
        }
        
        if (op.refactorTargetType is String && !decl.vartype.matches(op.refactorTargetType!!)) {
            fixes.add(decl.vartype.replace(className(op.refactorTargetType!!)))
        }

        if (op.refactorName is String && decl.name.toString() != op.refactorName) {
            // unfortunately name is not represented with a JCTree, so we have to resort to extraordinary measures...
            val original = decl.name.toString()
            val start = decl.startPosition + sourceText.substring(decl.startPosition..decl.getEndPosition(cu.endPositions))
                .substringBefore(original).length
            
            fixes.add(replace(start..start+original.length, op.refactorName!!))
        }

        return fixes
    }
}