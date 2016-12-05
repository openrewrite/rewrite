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
import com.netflix.rewrite.ast.Type
import com.netflix.rewrite.refactor.RefactorVisitor

class ChangeMethodTargetToStatic(val meth: Tr.MethodInvocation, val clazz: String): RefactorVisitor<Tr.MethodInvocation>() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<Tr.MethodInvocation>> {
        if(meth.id == this.meth.id) {
            val classType = Type.Class.build(cu.typeCache(), clazz)
            return transform {
                meth.copy(select = Tr.Ident(classType.className(), classType, meth.select?.formatting ?:
                        Formatting.Reified.Empty), declaringType = classType)
            }
        }
        return super.visitMethodInvocation(meth)
    }
}