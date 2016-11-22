/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.refactor.RefactorVisitor

class ReorderMethodArguments(val meth: Tr.MethodInvocation, vararg val byArgumentNames: String): RefactorVisitor() {
    private var originalParamNames: Array<out String>? = null

    fun setOriginalParamNames(vararg names: String) { originalParamNames = names }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        if(meth.id == this.meth.id && meth.type is Type.Method) {
            val paramNames = originalParamNames?.toList() ?: meth.type.paramNames?.toList() ?:
                    error("There is no source attachment for method ${meth.declaringType?.fullyQualifiedName}.${meth.name.simpleName}(..), " +
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
