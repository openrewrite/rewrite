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
package com.netflix.rewrite.search

import com.netflix.rewrite.ast.visitor.AstVisitor
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.ast.Type

class HasType(val clazz: String): AstVisitor<Boolean>(false) {

    override fun visitIdentifier(ident: Tr.Ident): Boolean =
        ident.type is Type.Class && (ident.type as Type.Class).fullyQualifiedName == clazz

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): Boolean {
        if(meth.firstMethodInChain().select == null) {
            // either a same-class instance method or a statically imported method
            return meth.type?.declaringType?.fullyQualifiedName == clazz
        }
        return super.visitMethodInvocation(meth)
    }
}