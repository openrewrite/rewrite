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
import com.netflix.java.refactor.ast.Type
import com.netflix.java.refactor.refactor.RefactorVisitor

data class ChangeFieldType(val decls: Tr.VariableDecls, val targetType: String) : RefactorVisitor() {

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<AstTransform<*>> {
        if(multiVariable.id == decls.id) {
            val classType = Type.Class.build(cu.typeCache(), targetType)
            if(decls.typeExpr.type != classType) {
                return listOf(AstTransform<Tr.VariableDecls>(cursor()) {
                    decls.copy(typeExpr = Tr.Ident(classType.className(), classType, decls.typeExpr.formatting),
                            vars = decls.vars.map { it.copy(type = classType, name = it.name.copy(type = classType)) })
                })
            }
        }
        return emptyList()
    }
}