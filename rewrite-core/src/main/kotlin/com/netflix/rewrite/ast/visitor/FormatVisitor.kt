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
package com.netflix.rewrite.ast.visitor

import com.netflix.rewrite.ast.*
import com.netflix.rewrite.refactor.RefactorVisitor

/**
 * Only formatting for nodes that we can ADD so far
 * is supported.
 *
 * Emits a side-effect of mutating formatting on tree nodes as necessary
 */
class FormatVisitor: RefactorVisitor<Tree>() {
    override val ruleName = "format"

    override fun visitCompilationUnit(cu: Tr.CompilationUnit): List<AstTransform<Tree>> {
        val changes = mutableListOf<AstTransform<Tree>>()

        // format changes in imports
        cu.imports.forEach { import ->
            if(import.formatting is Formatting.Infer) {
                if(import === cu.imports.last()) {
                    cu.classes.firstOrNull()?.let { clazz ->
                        changes.addAll(clazz.blankLinesBefore(2, clazz))
                    }
                    if(cu.imports.size > 1)
                        changes.addAll(import.blankLinesBefore(1, import))
                }

                if(import === cu.imports.first()) {
                    if(cu.packageDecl != null)
                        changes.addAll(import.blankLinesBefore(2, import))

                    // a previous first import will likely have a multiple line spacing prefix
                    if(cu.imports.size > 1 && cu.imports[1].formatting !is Formatting.Infer)
                        changes.addAll(cu.imports[1].blankLinesBefore(1, cu.imports[1]))
                }

                if(import !== cu.imports.last() && import !== cu.imports.first()) {
                    changes.addAll(import.blankLinesBefore(1, import))
                    cu.imports[cu.imports.indexOf(import) + 1].let { nextImport ->
                        changes.addAll(nextImport.blankLinesBefore(1, nextImport))
                    }
                }
            }
        }

        return super.visitCompilationUnit(cu) + changes
    }

    /**
     * Format added fields
     */
    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<AstTransform<Tree>> {
        val changes = if(multiVariable.formatting is Formatting.Infer) {
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

            transform {
                if (spaceIndent > 0) {
                    changeFormatting(format((1..spaceIndent).joinToString("", prefix = "\n") { " " }))
                } else if (tabIndent > 0) {
                    changeFormatting(format((1..spaceIndent).joinToString("", prefix = "\n") { "\t" }))
                } else {
                    // default formatting of 4 spaces
                    changeFormatting(format("\n    "))
                }
            }
        } else emptyList()

        return super.visitMultiVariable(multiVariable) + changes
    }

    private fun Tree?.blankLinesBefore(n: Int, tree: Tree = cursor().last()): List<AstTransform<Tree>> {
        if(this == null)
            return emptyList()

        return when(formatting) {
            is Formatting.Reified -> {
                val (prefix, _) = formatting as Formatting.Reified

                // add blank lines if necessary
                val addLines = (1..Math.max(0, n - prefix.takeWhile { it == '\n' }.length)).map { "\n" }.joinToString("")
                var modifiedPrefix = (addLines + prefix)

                // remove extra blank lines if necessary
                modifiedPrefix = modifiedPrefix.substring((modifiedPrefix.takeWhile { it == '\n' }.count() - n))

                if(modifiedPrefix != prefix)
                    transform(tree) { changeFormatting(formatting.withPrefix(modifiedPrefix)) }
                else emptyList()
            }
            is Formatting.Infer, is Formatting.None ->
                transform(tree) { changeFormatting(format((1..n).map { "\n" }.joinToString(""))) }
        }
    }
}