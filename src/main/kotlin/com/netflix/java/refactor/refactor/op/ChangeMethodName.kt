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

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.RefactorVisitor

class ChangeMethodName(val meth: Tr.MethodInvocation, val name: String) : RefactorVisitor() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        if (meth.id == this.meth.id) {
            if(meth.simpleName != name) {
                return listOf(AstTransform<Tr.MethodInvocation>(cursor()) {
                    copy(name = name.copy(simpleName = this@ChangeMethodName.name))
                })
            }
        }
        return super.visitMethodInvocation(meth)
    }
}