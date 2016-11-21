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

    override fun visitImport(import: Tr.Import): Tree? {
        if(import.formatting is Formatting.Infer) {
            // we are assuming throughout no common indentation of the whole file (including imports and class decls)
            if (cu.imports.size > 1) {
                var importPassed = false
                val firstSubsequentImport = cu.imports.find {
                    if(it == import) {
                        importPassed = true
                        false
                    }
                    else importPassed
                }

                if(firstSubsequentImport != null) {
                    import.formatting = firstSubsequentImport.formatting
                    firstSubsequentImport.formatting = Formatting.Reified("\n")
                }
                else {
                    // last import in the list
                    import.formatting = Formatting.Reified("\n")
                }
            } else if(cu.packageDecl != null) {
                import.blankLinesBefore(2)
            } else {
                import.formatting = Formatting.Reified.Empty
                cu.classes.firstOrNull()?.blankLinesBefore(2)
            }
        }

        return super.visitImport(import)
    }

    private fun Tree.blankLinesBefore(n: Int) {
        when(formatting) {
            is Formatting.Reified -> {
                val reified = formatting as Formatting.Reified
                val prefix = (1..Math.max(0, n - reified.prefix.takeWhile { it == '\n' }.length)).map { "\n" }.joinToString("")
                reified.prefix = prefix + reified.prefix
            }
            is Formatting.Infer ->
                formatting = Formatting.Reified((1..2).map { "\n" }.joinToString(""))
        }
    }
}