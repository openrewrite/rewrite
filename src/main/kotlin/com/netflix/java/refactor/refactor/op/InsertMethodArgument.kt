package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.RefactorVisitor

class InsertMethodArgument(val cu: Tr.CompilationUnit,
                           val meth: Tr.MethodInvocation,
                           val pos: Int,
                           val source: String): RefactorVisitor() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        if(meth.id == this.meth.id) {
            return listOf(AstTransform<Tr.MethodInvocation>(cursor()) {
                meth.copy(args = meth.args.let {
                    val modifiedArgs = it.args.toMutableList()
                    modifiedArgs.removeIf { it is Tr.Empty }

                    modifiedArgs.add(pos, Tr.UnparsedSource(source,
                            if (pos == 0) {
                                modifiedArgs.firstOrNull()?.formatting ?: Formatting.Reified.Empty
                            } else Formatting.Reified(" "))
                    )

                    if(pos == 0 && modifiedArgs.size > 1) {
                        // this argument previously did not occur after a comma, and now does, so let's introduce a bit of space
                        modifiedArgs[1].formatting = Formatting.Reified(" ")
                    }

                    it.copy(args = modifiedArgs)
                })
            })
        }
        return super.visitMethodInvocation(meth)
    }
}