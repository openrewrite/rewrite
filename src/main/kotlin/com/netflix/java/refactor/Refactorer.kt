package com.netflix.java.refactor

import com.netflix.java.refactor.op.*
import com.sun.tools.javac.tree.JCTree
import java.io.File
import java.util.*

class Refactorer() {

    private val ops = ArrayList<RefactorOperation>()
    private val bookmarks = BookmarkTable()
    
    fun bookmark(id: String): Bookmark {
        val bookmark = Bookmark(id, this)
        ops.add(bookmark)
        return bookmark
    }
    
    fun changeType(from: String, toPackage: String, toClass: String): Refactorer {
        ops.add(ChangeType(from, toPackage, toClass))
        return this
    }

    fun changeType(from: Class<*>, to: Class<*>) = changeType(from.name, to.`package`.name, to.simpleName)

    fun changeMethod(signature: String): ChangeMethodInvocation {
        val changeMethod = ChangeMethodInvocation(signature, this)
        ops.add(changeMethod)
        return changeMethod
    }
    
    fun removeImport(clazz: String): Refactorer {
        ops.add(RemoveImport(clazz))
        return this
    }
    
    fun removeImport(clazz: Class<*>) = removeImport(clazz.name)
    
    fun addImport(pkg: String, clazz: String): Refactorer {
        ops.add(AddImport(pkg, clazz))
        return this
    }
    
    fun addImport(clazz: Class<*>) = addImport(clazz.`package`.name, clazz.simpleName)

    /**
     * Perform refactoring on sources whose parsed representation matches
     */
    fun refactorWhen(matches: (JCTree.JCCompilationUnit) -> Boolean, sources: Iterable<File>, 
                     classPath: Iterable<File>? = null): List<RefactorFix> {
        val parser = AstParser()
        val cus = parser.parseFiles(sources.toList(), classPath)
        return ops.flatMap { op ->
            val fixes = cus.flatMap { cu ->
                if (matches.invoke(cu)) {
                    op.scanner().scan(cu, parser.context, bookmarks)
                } else emptyList()
            }
            fixes
        }
    }

    /**
     * Refactor all sources
     */
    fun refactor(sources: Iterable<File>, classPath: Iterable<File>? = null) = 
            refactorWhen({ cu -> true }, sources, classPath)

    /**
     * Perform refactoring and fix sources whose parsed representation matches
     */
    fun refactorAndFixWhen(matches: (JCTree.JCCompilationUnit) -> Boolean, sources: Iterable<File>,
                           classPath: Iterable<File>? = null): List<RefactorFix> {
        val fixes = refactorWhen(matches, sources, classPath)
        fixes.groupBy { it.source }
                .forEach {
                    try {
                        val fileText = it.key.readText()
                        val sortedFixes = it.value.sortedBy { it.position.last }.sortedBy { it.position.start }
                        var source = sortedFixes.foldIndexed("") { i, source, fix ->
                            val prefix = if (i == 0)
                                fileText.substring(0, fix.position.first)
                            else fileText.substring(sortedFixes[i - 1].position.last, fix.position.start)
                            source + prefix + (fix.changes ?: "")
                        }
                        if (sortedFixes.last().position.last < fileText.length) {
                            source += fileText.substring(sortedFixes.last().position.last)
                        }
                        it.key.writeText(source)
                    } catch(t: Throwable) {
                        // TODO how can we throw a better exception?
                        t.printStackTrace()
                    }
                }
        return fixes
    }

    /**
     * Refactor and fix all sources
     */
    fun refactorAndFix(sources: Iterable<File>, classPath: Iterable<File>? = null) =
            refactorAndFixWhen({ cu -> true }, sources, classPath)
}
