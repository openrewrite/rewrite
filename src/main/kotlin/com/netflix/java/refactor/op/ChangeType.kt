package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.netflix.java.refactor.op.RefactorOperation
import com.netflix.java.refactor.op.RefactoringScannerInternal
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.ImportTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.tree.JCTree

data class ChangeType(val from: String, val to: String): RefactorOperation {
    override val scanner = ChangeTypeScanner(this)
}

class ChangeTypeScanner(val op: ChangeType) : RefactoringScannerInternal() {
    override fun visitImport(node: ImportTree, session: Session): List<RefactorFix>? {
        val importType = (node as JCTree.JCImport).qualid as JCTree.JCFieldAccess
        return if(importType.toString() == op.from) {
            listOf(importType.replace(op.to, session))
        } else null
    }

    override fun visitIdentifier(node: IdentifierTree, session: Session): List<RefactorFix>? {
        val ident = node as JCTree.JCIdent
        val import = cu().namedImportScope.getElementsByName(ident.name).firstOrNull() ?:
                cu().starImportScope.getElementsByName(ident.name).firstOrNull()

        return if(import is Symbol.ClassSymbol && import.fullname.toString() == op.from) {
            val toElem = JavacElements.instance(session.context).getTypeElement(op.to)
            listOf(ident.replace(toElem.name.toString(), session))
        } else null
    }
}