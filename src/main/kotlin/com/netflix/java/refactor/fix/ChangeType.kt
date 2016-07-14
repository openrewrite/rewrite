package com.netflix.java.refactor.fix

import com.netflix.java.refactor.FixingOperation
import com.netflix.java.refactor.FixingScanner
import com.netflix.java.refactor.IfThenScanner
import com.netflix.java.refactor.RefactorFix
import com.sun.source.tree.IdentifierTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context

data class ChangeType(val from: String, val toPkg: String, val toClass: String): FixingOperation {
    override fun scanner() = IfThenScanner(
            ifFixesResultFrom = ChangeTypeScanner(this),
            then = arrayOf(
                RemoveImport(from).scanner(),
                AddImport(toPkg, toClass).scanner()
            )
    )
}

class ChangeTypeScanner(val op: ChangeType) : FixingScanner() {
    override fun visitIdentifier(node: IdentifierTree, context: Context): List<RefactorFix>? {
        val ident = node as JCTree.JCIdent
        return if(ident.sym is Symbol.ClassSymbol &&
                (ident.sym as Symbol.ClassSymbol).fullname.toString() == op.from) {
            listOf(ident.replace(op.toClass))
        } else null
    }
}