package com.netflix.java.refactor.find

import com.netflix.java.refactor.BaseRefactoringScanner
import com.netflix.java.refactor.RefactorOperation
import com.sun.source.tree.VariableTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context

data class Field(val name: String?) {
    companion object {
        val NO_FIELD = Field(null)
    }
    val exists = name is String
}

class FindField(val clazz: String): RefactorOperation<Field> {
    override fun scanner() = FindFieldScanner(this)
}

class FindFieldScanner(val op: FindField): BaseRefactoringScanner<Field>() {
    override fun visitVariable(node: VariableTree, context: Context?): Field? {
        val decl = node as JCTree.JCVariableDecl
        if(decl.sym.owner is Symbol.ClassSymbol &&
                decl.sym.owner.toString() == containingClass() &&
                decl.type.toString() == op.clazz) {
            return Field(decl.name.toString())
        }
        return super.visitVariable(node, context)
    }
    
    private fun containingClass(): String? {
        var path = currentPath
        while(path != null && path.leaf !is JCTree.JCClassDecl) {
            path = currentPath.parentPath
        }
        return if(path.leaf is JCTree.JCClassDecl) {
            (path.leaf as JCTree.JCClassDecl).name.toString()
        }
        else null
    }
    
    override fun reduce(r1: Field?, r2: Field?): Field =
            r1 ?: r2 ?: Field.NO_FIELD
}