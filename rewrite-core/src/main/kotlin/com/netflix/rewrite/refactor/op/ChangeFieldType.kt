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

import com.netflix.rewrite.ast.*
import com.netflix.rewrite.refactor.RefactorVisitor

data class ChangeFieldType(val targetType: String, override val ruleName: String = "change-field-type") :
        RefactorVisitor<Tr.VariableDecls>() {

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<AstTransform<Tr.VariableDecls>> {
        return if(multiVariable.typeExpr?.type?.asClass()?.fullyQualifiedName != targetType) {
            val classType = Type.Class.build(targetType)
            transform {
                copy(typeExpr = Tr.Ident.build(classType.className(), classType, typeExpr?.formatting ?: Formatting.Empty),
                        vars = vars.map { it.copy(type = classType, name = it.name.copy(type = classType)) })
            }
        } else emptyList()
    }
}