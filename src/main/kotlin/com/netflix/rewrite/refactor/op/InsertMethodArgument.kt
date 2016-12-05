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
package com.netflix.rewrite.refactor.op

import com.netflix.rewrite.ast.AstTransform
import com.netflix.rewrite.ast.Expression
import com.netflix.rewrite.ast.Formatting
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.refactor.RefactorVisitor

class InsertMethodArgument(val meth: Tr.MethodInvocation,
                           val pos: Int,
                           val source: String,
                           override val ruleName: String = "insert-method-argument"): RefactorVisitor<Tr.MethodInvocation>() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<Tr.MethodInvocation>> {
        if(meth.id == this.meth.id) {
            return transform {
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
                        modifiedArgs[1] = modifiedArgs[1].format(Formatting.Reified(" "))
                    }

                    it.copy(args = modifiedArgs)
                })
            }
        }
        return super.visitMethodInvocation(meth)
    }
}