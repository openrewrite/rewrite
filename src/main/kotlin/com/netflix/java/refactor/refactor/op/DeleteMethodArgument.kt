package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.RefactorVisitor

class DeleteMethodArgument(val meth: Tr.MethodInvocation, val pos: Int): RefactorVisitor() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        if(meth.id == this.meth.id && meth.args.args.filter { it !is Tr.Empty }.size > pos) {
            return listOf(AstTransform<Tr.MethodInvocation>(cursor()) {
                meth.copy(args = meth.args.let {
                    var modifiedArgs = it.args.slice(0..pos - 1) + it.args.drop(pos + 1)
                    if(modifiedArgs.isEmpty())
                        modifiedArgs = listOf(Tr.Empty(Formatting.Reified.Empty))
                    it.copy(modifiedArgs)
                })
            })
        }
        return super.visitMethodInvocation(meth)
    }
}