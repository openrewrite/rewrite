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
package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.ast.visitor.AstVisitor

class FindInheritedFields(val fullyQualifiedClassName: String): AstVisitor<List<Type.Var>>(emptyList()) {
    override fun visitClassDecl(classDecl: Tr.ClassDecl): List<Type.Var> {
        return superFields(classDecl.type.asClass()?.supertype)
    }

    private fun superFields(type: Type.Class?): List<Type.Var> {
        if (type == null)
            return emptyList()

        if (type.supertype == null)
            return emptyList()

        return type.members.filter { !it.hasFlags(Type.Var.Flags.Private) && it.type.hasElementType(fullyQualifiedClassName) } +
                superFields(type.supertype)
    }
}