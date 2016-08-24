package com.netflix.java.refactor.find

import com.netflix.java.refactor.ast.AstScannerBuilder
import com.netflix.java.refactor.ast.SingleCompilationUnitAstScanner
import com.sun.source.tree.ImportTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context

class HasImport(val clazz: String): AstScannerBuilder<Boolean> {
    override fun scanner() = HasImportScanner(this)
}

class HasImportScanner(val op: HasImport): SingleCompilationUnitAstScanner<Boolean>() {
    override fun visitImport(node: ImportTree, p: Context): Boolean {
        val import = node as JCTree.JCImport
        val qualid = import.qualid as JCTree.JCFieldAccess
        return when(qualid.name.toString()) {
            "*" -> qualid.selected.toString() == op.clazz.split('.').takeWhile { it[0].isLowerCase() }.joinToString(".")
            else -> qualid.toString() == op.clazz
        }
    }
    
    override fun reduce(r1: Boolean?, r2: Boolean?) =
            (r1 ?: false) || (r2 ?: false)
}