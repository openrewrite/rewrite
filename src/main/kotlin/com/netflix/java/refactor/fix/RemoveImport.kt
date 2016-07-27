package com.netflix.java.refactor.fix

import com.netflix.java.refactor.ast.RefactoringAstScannerBuilder
import com.netflix.java.refactor.ast.FixingScanner
import com.netflix.java.refactor.RefactorFix
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.ImportTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.util.*

class RemoveImport(val clazz: String) : RefactoringAstScannerBuilder {
    override fun scanner() = RemoveImportScanner(this)
}

class RemoveImportScanner(val op: RemoveImport) : FixingScanner() {
    var namedImport: JCTree.JCImport? = null
    var starImport: JCTree.JCImport? = null
    var referencedTypes = ArrayList<Symbol.ClassSymbol>()
    
    override fun visitImport(node: ImportTree, context: Context): List<RefactorFix>? {
        val import = node as JCTree.JCImport
        val importType = import.qualid as JCTree.JCFieldAccess
        if (importType.toString() == op.clazz) {
            namedImport = import
        }
        else if(importType.name.toString() == "*" && importType.selected.toString() == context.packageContaining(op.clazz)) {
            starImport = import
        }
        return null
    }

    override fun visitIdentifier(node: IdentifierTree, context: Context): List<RefactorFix>? {
        val ident = node as JCTree.JCIdent
        if(ident.sym is Symbol.ClassSymbol) {
            val sym = ident.sym as Symbol.ClassSymbol
            if(sym.owner.toString() == context.packageContaining(op.clazz)) {
                referencedTypes.add(sym)
            }
        }
        return null
    }

    override fun visitEnd(context: Context): List<RefactorFix> {
        return if(namedImport is JCTree.JCImport && referencedTypes.none { it.toString() == op.clazz }) {
            listOf(namedImport!!.delete())
        }
        else if(starImport is JCTree.JCImport && referencedTypes.isEmpty()) {
            listOf(starImport!!.delete())
        } else if(starImport is JCTree.JCImport && referencedTypes.size == 1) {
            listOf(starImport!!.replace("import ${referencedTypes[0].className()};"))
        }
        else emptyList()
    }
}