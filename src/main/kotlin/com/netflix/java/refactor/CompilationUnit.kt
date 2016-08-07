package com.netflix.java.refactor

import com.netflix.java.refactor.ast.AstParser
import com.sun.tools.javac.tree.JCTree
import java.nio.file.Path

/**
 * Keep track of the current state of the compilation unit for a given file. As rules
 * begin to modify the underlying source file, 
 */
data class CompilationUnit(var jcCompilationUnit: JCTree.JCCompilationUnit, 
                           val parser: AstParser) {
   
    fun source(): Path {
        val path = jcCompilationUnit.sourcefile.javaClass.declaredFields.find { it.type == Path::class.java }!!
        path.isAccessible = true
        return path.get(jcCompilationUnit.sourcefile) as Path
    }
    
    fun reparse() {
        jcCompilationUnit = parser.reparse(this)
    }
}