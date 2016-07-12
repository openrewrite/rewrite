package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.ImportTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import java.util.*

class RemoveImport(val clazz: String) : RefactorOperation {
    override fun scanner() = RemoveImportScanner(this)
}

class RemoveImportScanner(val op: RemoveImport) : BaseRefactoringScanner() {
    var starImport: JCTree.JCImport? = null
    var otherTypes = ArrayList<Symbol.ClassSymbol>()
    
    override fun visitImport(node: ImportTree, session: Session): List<RefactorFix>? {
        val import = node as JCTree.JCImport
        val importType = import.qualid as JCTree.JCFieldAccess
        return if (importType.toString() == op.clazz) {
            listOf(import.delete(session))
        }
        else if(importType.name.toString() == "*" && importType.selected.toString() == session.packageContaining(op.clazz)) {
            starImport = import
            null
        }
        else null
    }

    override fun visitIdentifier(node: IdentifierTree, session: Session): List<RefactorFix>? {
        val ident = node as JCTree.JCIdent
        val type = session.cu.namedImportScope.getElementsByName(ident.name).firstOrNull() ?:
                session.cu.starImportScope.getElementsByName(ident.name).firstOrNull()
        type?.let {
            val sym = it as Symbol.ClassSymbol
            if(sym.owner.toString() == session.packageContaining(op.clazz) && !op.clazz.endsWith(sym.name.toString())) {
                otherTypes.add(sym)
            }
        }
        return null
    }

    override fun visitEnd(session: Session): List<RefactorFix>? {
        return if(starImport is JCTree.JCImport && otherTypes.isEmpty()) {
            listOf(starImport!!.delete(session))
        } else if(starImport is JCTree.JCImport && otherTypes.size == 1) {
            listOf(starImport!!.replace("import ${otherTypes[0].className()};", session))
        }
        else null
    }
}