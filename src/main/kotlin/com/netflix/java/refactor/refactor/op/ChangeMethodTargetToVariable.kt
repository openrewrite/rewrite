package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.asClass
import com.netflix.java.refactor.refactor.RefactorVisitor

class ChangeMethodTargetToVariable(val meth: Tr.MethodInvocation, val namedVar: Tr.VariableDecls.NamedVar): RefactorVisitor() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        if(meth.id == this.meth.id) {
            return listOf(AstTransform<Tr.MethodInvocation>(cursor()) {
                copy(select = Tr.Ident(namedVar.name.name, namedVar.type, select?.formatting ?: Formatting.Reified.Empty),
                        declaringType = namedVar.type.asClass())
            })
        }
        return super.visitMethodInvocation(meth)
    }
}