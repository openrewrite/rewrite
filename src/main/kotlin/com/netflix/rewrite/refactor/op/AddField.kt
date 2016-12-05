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

class AddField(val modifiers: List<Tr.VariableDecls.Modifier>,
               val target: Tr.ClassDecl,
               val clazz: String,
               val name: String,
               val init: String?): RefactorVisitor<Tr.Block<*>>() {

    private val classType by lazy { Type.Class.build(cu.typeCache(), clazz) }

    override fun visitClassDecl(classDecl: Tr.ClassDecl): List<AstTransform<Tr.Block<*>>> =
        if(classDecl.id == target.id) {
            val newField = Tr.VariableDecls(
                    emptyList(),
                    modifiers,
                    Tr.Ident(classType.className(), classType, Formatting.Reified.Empty),
                    null,
                    emptyList(),
                    listOf(Tr.VariableDecls.NamedVar(
                            Tr.Ident(name, null, Formatting.Reified("", if(init != null) " " else "")),
                            emptyList(),
                            init?.let { Tr.UnparsedSource(it, Formatting.Reified(" ")) },
                            classType,
                            Formatting.Reified(" ")
                    )),
                    Formatting.Infer
            )

            transform(cursor().plus(classDecl.body)) {
                copy(statements = listOf(newField) + statements)
            }
        }
        else super.visitClassDecl(classDecl)
}