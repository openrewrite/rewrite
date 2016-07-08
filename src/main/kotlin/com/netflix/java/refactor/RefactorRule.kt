package com.netflix.java.refactor

import com.netflix.java.refactor.op.*
import com.sun.tools.javac.tree.JCTree
import java.io.File
import java.util.*

class RefactorRule() {

    private val ops = ArrayList<RefactorOperation>()
    
    fun changeType(from: String, toPackage: String, toClass: String): RefactorRule {
        ops.add(ChangeType(from, toPackage, toClass))
        return this
    }

    fun changeType(from: Class<*>, to: Class<*>) = changeType(from.name, to.`package`.toString(), to.simpleName)

    fun changeMethod(signature: String): ChangeMethodInvocation {
        val changeMethod = ChangeMethodInvocation(signature, this)
        ops.add(changeMethod)
        return changeMethod
    }
    
    fun removeImport(clazz: String): RefactorRule {
        ops.add(RemoveImport(clazz))
        return this
    }
    
    fun removeImport(clazz: Class<*>) = removeImport(clazz.name)
    
    fun addImport(pkg: String, clazz: String): RefactorRule {
        ops.add(AddImport(pkg, clazz))
        return this
    }
    
    fun addImport(clazz: Class<*>) = addImport(clazz.`package`.toString(), clazz.simpleName)
 
    fun refactorWhen(matches: (File, JCTree.JCCompilationUnit) -> Boolean, vararg sources: File) =
        ops.flatMap { op ->
            val parser = AstParser()
            val filesToCompilationUnit = sources.zip(parser.parseFiles(sources.toList()))
            filesToCompilationUnit.flatMap {
                val (file, cu) = it
                if(matches.invoke(file, cu)) {
                    op.scanner.scan(cu, parser.context, file)
                } else emptyList()
            }
        }        
    
    fun refactor(vararg sources: File): List<RefactorFix> = refactorWhen({ f, cu -> true }, *sources)

    fun refactorAndFixWhen(matches: (File, JCTree.JCCompilationUnit) -> Boolean, vararg sources: File): List<RefactorFix> {
        val fixes = refactorWhen(matches, *sources)
        fixes.groupBy { it.source }
                .forEach {
                    val fileText = it.key.readText()
                    val sortedFixes = it.value.sortedBy { it.position.first }
                    var source = sortedFixes.foldIndexed("") { i, source, fix ->
                        val prefix = if(i == 0)
                            fileText.substring(0, fix.position.first)
                        else fileText.substring(sortedFixes[i-1].position.last, fix.position.start)
                        source + prefix + (fix.changes ?: "")
                    }
                    if(sortedFixes.last().position.last < fileText.length) {
                        source += fileText.substring(sortedFixes.last().position.last)
                    }
                    it.key.writeText(source)
                }
        return fixes        
    }
    
    fun refactorAndFix(vararg sources: File): List<RefactorFix> = refactorAndFixWhen({ f, cu -> true }, *sources)
}
