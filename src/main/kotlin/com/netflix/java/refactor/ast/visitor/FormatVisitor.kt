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
package com.netflix.java.refactor.ast.visitor

import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.Tree
import java.util.*

/**
 * Only formatting for nodes that we can ADD so far
 * is supported.
 *
 * Emits a side-effect of mutating formatting on tree nodes as necessary
 */
class FormatVisitor: AstVisitor<Tree?>({ it }) {

    lateinit var cu: Tr.CompilationUnit

    override fun visitCompilationUnit(cu: Tr.CompilationUnit): Tree? {
        this.cu = cu

        cu.imports.forEach { import ->
            if(import.formatting is Formatting.Infer) {
                if(import === cu.imports.last()) {
                    cu.classes.firstOrNull()?.blankLinesBefore(2)
                    if(cu.imports.size > 1)
                        import.blankLinesBefore(1)
                }

                if(import === cu.imports.first()) {
                    if(cu.packageDecl != null)
                        import.blankLinesBefore(2)

                    // a previous first import will likely have a multiple line spacing prefix
                    if(cu.imports.size > 1 && cu.imports[1].formatting !is Formatting.Infer)
                        cu.imports[1].blankLinesBefore(1)
                }

                if(import !== cu.imports.last() && import !== cu.imports.first()) {
                    import.blankLinesBefore(1)
                    cu.imports[cu.imports.indexOf(import) + 1].blankLinesBefore(1)
                }
            }
        }

        return super.visitCompilationUnit(cu)
    }

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): Tree? {
        if(multiVariable.formatting is Formatting.Infer) {
            // we make a simplifying assumption here that inferred variable
            // declaration formatting comes only from added fields
            val classBody = cursor().parent().last() as Tr.Block<*>

            val indents = classBody.statements
                    .map(Tree::formatting)
                    .filterIsInstance<Formatting.Reified>()
                    .map { it.prefix.substringAfterLast("\n") }

            fun indentWidth(c: Char) = indents.fold(mutableMapOf<Int, Int>()) { acc, indent ->
                acc.merge(indent.toCharArray().count { it == c }, 0, Int::plus)
                acc
            }.maxWith<Int, Int>(Comparator { e1, e2 -> e1.value.compareTo(e2.value) })?.key ?: 0

            val spaceIndent = indentWidth(' ')
            val tabIndent = indentWidth('\t')

            if(spaceIndent > 0) {
                multiVariable.formatting = Formatting.Reified((1..spaceIndent).joinToString("", prefix = "\n") { " " })
            } else if(tabIndent > 0) {
                multiVariable.formatting = Formatting.Reified((1..spaceIndent).joinToString("", prefix = "\n") { "\t" })
            } else {
                // default formatting of 4 spaces
                multiVariable.formatting = Formatting.Reified("\n    ")
            }
        }

        return super.visitMultiVariable(multiVariable)
    }

    private fun Tree.blankLinesBefore(n: Int) {
        when(formatting) {
            is Formatting.Reified -> {
                val reified = formatting as Formatting.Reified

                // add blank lines if necessary
                val prefix = (1..Math.max(0, n - reified.prefix.takeWhile { it == '\n' }.length)).map { "\n" }.joinToString("")
                reified.prefix = prefix + reified.prefix

                // remove extra blank lines if necessary
                reified.prefix = reified.prefix.substring((reified.prefix.takeWhile { it == '\n' }.count() - n))
            }
            is Formatting.Infer ->
                formatting = Formatting.Reified((1..n).map { "\n" }.joinToString(""))
        }
    }
}