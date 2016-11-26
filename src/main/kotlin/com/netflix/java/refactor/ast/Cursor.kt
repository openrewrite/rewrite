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
package com.netflix.java.refactor.ast

data class Cursor(val path: List<Tree>) {
    fun plus(t: Tree) = copy(path + t)
    operator fun plus(cursor: Cursor) = copy(path + cursor.path)

    fun parent() = copy(path.dropLast(1))
    fun last() = path.last()
    
    companion object {
        val Empty = Cursor(emptyList())
    }

    override fun equals(other: Any?): Boolean = if(other is Cursor && path.size == other.path.size) {
        path.forEachIndexed { i, tree ->
            if(other.path[i] != path[i])
                return@equals false
        }
        true
    } else false

    override fun hashCode(): Int = path.hashCode()

    fun enclosingMethod(): Tr.MethodDecl? = path.filterIsInstance<Tr.MethodDecl>().lastOrNull()
    fun enclosingClass(): Tr.ClassDecl? = path.filterIsInstance<Tr.ClassDecl>().lastOrNull()
    fun enclosingCompilationUnit(): Tr.CompilationUnit = path.filterIsInstance<Tr.CompilationUnit>().first()
}
