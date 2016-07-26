package com.netflix.java.refactor.find

import com.netflix.java.refactor.ast.SingleCompilationUnitAstScanner
import com.netflix.java.refactor.ast.AstScannerBuilder
import com.sun.source.tree.VariableTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context

data class Field(val name: String)

class FindFields(val clazz: String): AstScannerBuilder<List<Field>> {
    override fun scanner() = FindFieldScanner(this)
}

class FindFieldScanner(val op: FindFields): SingleCompilationUnitAstScanner<List<Field>>() {
    override fun visitVariable(node: VariableTree, context: Context?): List<Field>? {
        val decl = node as JCTree.JCVariableDecl
        if(decl.sym.owner is Symbol.ClassSymbol &&
                decl.sym.owner.toString() == containingClass() &&
                decl.type.toString() == op.clazz) {
            return listOf(Field(decl.name.toString()))
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

    override fun reduce(r1: List<Field>?, r2: List<Field>?) = (r1 ?: emptyList()).plus(r2 ?: emptyList())
}