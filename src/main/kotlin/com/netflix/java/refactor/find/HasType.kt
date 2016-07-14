package com.netflix.java.refactor.find

import com.netflix.java.refactor.BaseRefactoringScanner
import com.netflix.java.refactor.RefactorOperation
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context


class HasType(val clazz: String): RefactorOperation<Boolean> {
    override fun scanner() = HasTypeScanner(this)
}

class HasTypeScanner(val op: HasType): BaseRefactoringScanner<Boolean>() {
    override fun visitIdentifier(node: IdentifierTree, context: Context): Boolean {
        val ident = node as JCTree.JCIdent
        if(ident.sym is Symbol.ClassSymbol) {
            return ident.sym.toString() == op.clazz
        }
        return false
    }

    override fun visitMethodInvocation(node: MethodInvocationTree, context: Context): Boolean? {
        val invocation = node as JCTree.JCMethodInvocation
        if(invocation.meth is JCTree.JCIdent) {
            // statically imported type
            return (invocation.meth as JCTree.JCIdent).sym.owner.toString() == op.clazz
        }
        return super.visitMethodInvocation(node, context)
    }
    
    override fun reduce(r1: Boolean?, r2: Boolean?) =
        (r1 ?: false) || (r2 ?: false)
}