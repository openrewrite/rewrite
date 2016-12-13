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

class AddField(val modifiers: List<Tr.VariableDecls.Modifier>,
               val clazz: String,
               val name: String,
               val init: String?,
               override val ruleName: String = "add-field"): RefactorVisitor<Tr.Block<*>>() {

    override fun visitClassDecl(classDecl: Tr.ClassDecl): List<AstTransform<Tr.Block<*>>> {
        val classType = Type.Class.build(clazz)
        val newField = Tr.VariableDecls(
                emptyList(),
                modifiers,
                Tr.Ident.build(classType.className(), classType, Formatting.Empty),
                null,
                emptyList(),
                listOf(Tr.VariableDecls.NamedVar(
                        Tr.Ident.build(name, null, format("", if (init != null) " " else "")),
                        emptyList(),
                        init?.let { Tr.UnparsedSource(it, format(" ")) },
                        classType,
                        format(" ")
                )),
                Formatting.Infer
        )

        return transform(classDecl.body, ruleName) {
            copy(statements = listOf(newField) + statements)
        }
    }
}