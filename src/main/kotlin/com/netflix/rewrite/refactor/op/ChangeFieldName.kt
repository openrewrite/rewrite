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
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.refactor.RefactorVisitor

data class ChangeFieldName(val decls: Tr.VariableDecls,
                           val name: String,
                           override val ruleName: String = "change-field-name") : RefactorVisitor<Tr.VariableDecls>() {

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<AstTransform<Tr.VariableDecls>> {
        if(multiVariable.id == decls.id) {
            assert(multiVariable.vars.size == 1) { "Refactor name is not supported on multi-variable declarations" }

            val v = multiVariable.vars.first()
            if(v.simpleName != name) {
                return transform(ruleName) { copy(vars = listOf(v.copy(name = Tr.Ident(name, v.name.type, v.name.formatting)))) }
            }
        }
        return emptyList()
    }
}