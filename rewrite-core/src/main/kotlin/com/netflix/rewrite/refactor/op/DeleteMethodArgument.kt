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
import com.netflix.rewrite.ast.Formatting
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.refactor.RefactorVisitor

class DeleteMethodArgument(val pos: Int, override val ruleName: String = "delete-method-argument"):
        RefactorVisitor<Tr.MethodInvocation>() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<Tr.MethodInvocation>> {
        if(meth.args.args.filter { it !is Tr.Empty }.size > pos) {
            return transform {
                meth.copy(args = meth.args.let {
                    var modifiedArgs = it.args.slice(0..pos - 1) + it.args.drop(pos + 1)
                    if(modifiedArgs.isEmpty())
                        modifiedArgs = listOf(Tr.Empty(Formatting.Empty))
                    it.copy(modifiedArgs)
                })
            }
        }
        return super.visitMethodInvocation(meth)
    }
}