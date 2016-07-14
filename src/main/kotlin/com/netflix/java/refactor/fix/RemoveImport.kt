package com.netflix.java.refactor.fix

import com.netflix.java.refactor.FixingOperation
import com.netflix.java.refactor.FixingScanner
import com.netflix.java.refactor.RefactorFix
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.ImportTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.util.*

class RemoveImport(val clazz: String) : FixingOperation {
    override fun scanner() = RemoveImportScanner(this)
}

class RemoveImportScanner(val op: RemoveImport) : FixingScanner() {
    var starImport: JCTree.JCImport? = null
    var otherTypes = ArrayList<Symbol.ClassSymbol>()
    
    override fun visitImport(node: ImportTree, context: Context): List<RefactorFix>? {
        val import = node as JCTree.JCImport
        val importType = import.qualid as JCTree.JCFieldAccess
        return if (importType.toString() == op.clazz) {
            listOf(import.delete())
        }
        else if(importType.name.toString() == "*" && importType.selected.toString() == context.packageContaining(op.clazz)) {
            starImport = import
            null
        }
        else null
    }

    override fun visitIdentifier(node: IdentifierTree, context: Context): List<RefactorFix>? {
        val ident = node as JCTree.JCIdent
        if(ident.sym is Symbol.ClassSymbol) {
            val sym = ident.sym as Symbol.ClassSymbol
            if(sym.owner.toString() == context.packageContaining(op.clazz) && !op.clazz.endsWith(sym.name.toString())) {
                otherTypes.add(sym)
            }
        }
        return null
    }

    override fun visitEnd(context: Context): List<RefactorFix> {
        return if(starImport is JCTree.JCImport && otherTypes.isEmpty()) {
            listOf(starImport!!.delete())
        } else if(starImport is JCTree.JCImport && otherTypes.size == 1) {
            listOf(starImport!!.replace("import ${otherTypes[0].className()};"))
        }
        else emptyList()
    }
}