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
package com.netflix.rewrite.refactor

import com.netflix.rewrite.ast.AstTransform
import com.netflix.rewrite.ast.Cursor
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.ast.Tree
import com.netflix.rewrite.ast.visitor.AstVisitor

abstract class RefactorVisitor<T: Tree>: AstVisitor<List<AstTransform<T>>>(emptyList()) {
    abstract val ruleName: String

    @Suppress("UNCHECKED_CAST")
    fun transform(target: Tree, name: String, mutation: T.() -> T): List<AstTransform<T>> =
            listOf(AstTransform(target.id, mutation as Tree.() -> T, name))

    fun transform(name: String, mutation: T.() -> T): List<AstTransform<T>> =
            transform(cursor().last(), name, mutation)

    fun transform(target: Tree, mutation: T.() -> T) = transform(target, ruleName, mutation)
    fun transform(mutation: T.() -> T) = transform(cursor().last(), ruleName, mutation)
}