package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.sun.source.tree.IdentifierTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree

data class ChangeType(val from: String, val toPkg: String, val toClass: String): RefactorOperation {
    override val scanner = IfThenScanner(
            ChangeTypeScanner(this),
            RemoveImport(from).scanner,
            AddImport(toPkg, toClass).scanner
    )
}

class ChangeTypeScanner(val op: ChangeType) : BaseRefactoringScanner() {
    override fun visitIdentifier(node: IdentifierTree, session: Session): List<RefactorFix>? {
        val ident = node as JCTree.JCIdent
        val import = session.cu.namedImportScope.getElementsByName(ident.name).firstOrNull() ?:
                session.cu.starImportScope.getElementsByName(ident.name).firstOrNull()

        return if(import is Symbol.ClassSymbol && import.fullname.toString() == op.from) {
            listOf(ident.replace(op.toClass, session))
        } else null
    }
}