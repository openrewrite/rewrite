package com.netflix.java.refactor

import com.netflix.java.refactor.ast.RefactoringAstScannerBuilder
import com.netflix.java.refactor.fix.*
import java.util.*

class RefactorTransaction(val refactorer: JavaSource) {
    private val ops = ArrayList<RefactoringAstScannerBuilder>()

    fun changeType(from: String, to: String): RefactorTransaction {
        ops.add(ChangeType(from, to))
        return this
    }

    fun changeType(from: Class<*>, to: Class<*>) = changeType(from.name, to.name)

    fun findMethodCalls(signature: String): ChangeMethodInvocation {
        val changeMethod = ChangeMethodInvocation(signature, this)
        ops.add(changeMethod)
        return changeMethod
    }

    fun findFieldsOfType(clazz: Class<*>): ChangeField = findFieldsOfType(clazz.name)

    fun findFieldsOfType(clazz: String): ChangeField {
        val changeField = ChangeField(clazz, this)
        ops.add(changeField)
        return changeField
    }

    fun removeImport(clazz: String): RefactorTransaction {
        ops.add(RemoveImport(clazz))
        return this
    }

    fun removeImport(clazz: Class<*>) = removeImport(clazz.name)

    fun addImport(clazz: String): RefactorTransaction {
        ops.add(AddImport(clazz))
        return this
    }

    fun addImport(clazz: Class<*>) = addImport(clazz.name)

    fun addStaticImport(clazz: String, method: String): RefactorTransaction {
        ops.add(AddImport(clazz, method))
        return this
    }

    fun addStaticImport(clazz: Class<*>, method: String) = addStaticImport(clazz.name, method)

    fun fix() {
        val fixes = ops.flatMap { it.scanner().scan(refactorer.cu) }

        if(fixes.isNotEmpty()) {
            try {
                val sourceText = refactorer.text()
                val sortedFixes = fixes.sortedBy { it.position.last }.sortedBy { it.position.start }
                var source = sortedFixes.foldIndexed("") { i, source, fix ->
                    val prefix = if (i == 0)
                        sourceText.substring(0, fix.position.first)
                    else sourceText.substring(sortedFixes[i - 1].position.last, fix.position.start)
                    source + prefix + (fix.changes ?: "")
                }
                if (sortedFixes.last().position.last < sourceText.length) {
                    source += sourceText.substring(sortedFixes.last().position.last)
                }
                refactorer.file().writeText(source)
            } catch(t: Throwable) {
                // TODO how can we throw a better exception?
                t.printStackTrace()
            }
        }

        if(fixes.isNotEmpty()) {
            refactorer.changedFile = true
            refactorer.lastCommitChangedFile = true
        }
    }
}