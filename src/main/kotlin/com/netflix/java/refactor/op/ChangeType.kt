package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.sun.source.tree.IdentifierTree
import com.sun.tools.javac.tree.JCTree

data class ChangeType(val from: String, val toPkg: String, val toClass: String): RefactorOperation {
    override fun scanner() = IfThenScanner(
            ifFixesResultFrom = ChangeTypeScanner(this),
            then = arrayOf(
                RemoveImport(from).scanner(),
                AddImport(toPkg, toClass).scanner()
            )
    )
}

class ChangeTypeScanner(val op: ChangeType) : BaseRefactoringScanner() {
    override fun visitIdentifier(node: IdentifierTree, session: Session): List<RefactorFix>? {
        val ident = node as JCTree.JCIdent
        return if(session.classSymbol(ident.name)?.fullname?.toString() == op.from) {
            listOf(ident.replace(op.toClass, session))
        } else null
    }
}