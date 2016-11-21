package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.refactor.RefactorVisitor

class ReorderMethodArguments(val meth: Tr.MethodInvocation, vararg val byArgumentNames: String): RefactorVisitor() {
    private var originalParamNames: Array<out String>? = null

    fun setOriginalParamNames(vararg names: String) { originalParamNames = names }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        if(meth.id == this.meth.id && meth.type is Type.Method) {
            val paramNames = originalParamNames?.toList() ?: meth.type.paramNames?.toList() ?:
                    error("There is no source attachment for method ${meth.declaringType?.fullyQualifiedName}.${meth.name.name}(..), " +
                            "provide a reference for original parameter names by calling setOriginalParamNames(..)")

            val paramTypes = meth.type.resolvedSignature.paramTypes

            var i = 0
            val (reordered, formattings) = byArgumentNames.fold(emptyList<Expression>() to emptyList<Formatting>()) { acc, name ->
                val fromPos = paramNames.indexOf(name)
                if(meth.args.args.size > paramTypes.size && fromPos == paramTypes.size - 1) {
                    // this is a varargs argument
                    val varargs = meth.args.args.drop(fromPos)
                    val formatting = meth.args.args.subList(i, (i++) + varargs.size).map(Expression::formatting)
                    acc.first + varargs to
                            acc.second + formatting
                } else if(fromPos >= 0 && meth.args.args.size > fromPos) {
                    acc.first + meth.args.args[fromPos] to
                            acc.second + meth.args.args[i++].formatting
                } else acc
            }

            reordered.forEachIndexed { i, arg -> arg.formatting = formattings[i] }

            return listOf(AstTransform<Tr.MethodInvocation>(cursor()) {
                copy(args = args.copy(args = reordered))
            })
        }

        return super.visitMethodInvocation(meth)
    }
}
