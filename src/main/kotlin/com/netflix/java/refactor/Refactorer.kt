package com.netflix.java.refactor

import com.netflix.java.refactor.op.*
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.io.File
import java.util.*

class Refactorer(val cu: JCTree.JCCompilationUnit, val context: Context, val dryRun: Boolean = false) {
    var changedFile = false
    
    val source = File(cu.sourceFile.toUri().path)
    val sourceText: String by lazy { source.readText() }

    fun tx() = RefactorTransaction(this)
    private fun autoTx() = RefactorTransaction(this, true)

    fun changeType(from: String, toPackage: String, toClass: String) {
        tx().changeType(from, toPackage, toClass).commit()
    }
    
    fun changeType(from: Class<*>, to: Class<*>) {
        tx().changeType(from, to).commit()
    }

    fun changeMethod(signature: String): ChangeMethodInvocation = autoTx().changeMethod(signature)

    fun removeImport(clazz: String) {
        tx().removeImport(clazz).commit()
    }
    
    fun removeImport(clazz: Class<*>) {
        tx().removeImport(clazz).commit()
    }

    fun addImport(pkg: String, clazz: String) {
        tx().addImport(pkg, clazz).commit()
    }

    fun addImport(clazz: Class<*>) {
        tx().addImport(clazz).commit()
    }
}


class RefactorTransaction(val refactorer: Refactorer, val autoCommit: Boolean = false) {
    private val ops = ArrayList<RefactorOperation>()
    private val bookmarks = BookmarkTable()
    
    fun changeType(from: String, toPackage: String, toClass: String): RefactorTransaction {
        ops.add(ChangeType(from, toPackage, toClass))
        return this
    }

    fun changeType(from: Class<*>, to: Class<*>) = changeType(from.name, to.`package`.name, to.simpleName)

    fun changeMethod(signature: String): ChangeMethodInvocation {
        val changeMethod = ChangeMethodInvocation(signature, this)
        ops.add(changeMethod)
        return changeMethod
    }

    fun removeImport(clazz: String): RefactorTransaction {
        ops.add(RemoveImport(clazz))
        return this
    }

    fun removeImport(clazz: Class<*>) = removeImport(clazz.name)

    fun addImport(pkg: String, clazz: String): RefactorTransaction {
        ops.add(AddImport(pkg, clazz))
        return this
    }

    fun addImport(clazz: Class<*>) = addImport(clazz.`package`.name, clazz.simpleName)
    
    fun commit() {
        val fixes = ops.flatMap { it.scanner().scan(refactorer.cu, refactorer.context, bookmarks) }
        
        if(!refactorer.dryRun) {
            try {
                val sourceText = refactorer.sourceText
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
                refactorer.source.writeText(source)
            } catch(t: Throwable) {
                // TODO how can we throw a better exception?
                t.printStackTrace()
            }
        }
        
        if(fixes.isNotEmpty())
            refactorer.changedFile = true
    }
}