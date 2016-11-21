package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.RefactorVisitor

class ChangeMethodName(val meth: Tr.MethodInvocation, val name: String) : RefactorVisitor() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        if (meth.id == this.meth.id) {
            if(meth.name.name != name) {
                return listOf(AstTransform<Tr.MethodInvocation>(cursor()) {
                    copy(name = name.copy(name = this@ChangeMethodName.name))
                })
            }
        }
        return super.visitMethodInvocation(meth)
    }
}