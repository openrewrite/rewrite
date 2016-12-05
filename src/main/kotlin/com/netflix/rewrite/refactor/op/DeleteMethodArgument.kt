package com.netflix.rewrite.refactor.op

import com.netflix.rewrite.ast.AstTransform
import com.netflix.rewrite.ast.Formatting
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.refactor.RefactorVisitor

class DeleteMethodArgument(val meth: Tr.MethodInvocation,
                           val pos: Int,
                           override val ruleName: String = "delete-method-argument"): RefactorVisitor<Tr.MethodInvocation>() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<Tr.MethodInvocation>> {
        if(meth.id == this.meth.id && meth.args.args.filter { it !is Tr.Empty }.size > pos) {
            return transform {
                meth.copy(args = meth.args.let {
                    var modifiedArgs = it.args.slice(0..pos - 1) + it.args.drop(pos + 1)
                    if(modifiedArgs.isEmpty())
                        modifiedArgs = listOf(Tr.Empty(Formatting.Reified.Empty))
                    it.copy(modifiedArgs)
                })
            }
        }
        return super.visitMethodInvocation(meth)
    }
}