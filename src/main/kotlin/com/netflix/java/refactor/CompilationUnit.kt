package com.netflix.java.refactor

import com.netflix.java.refactor.ast.AstParser
import com.sun.tools.javac.tree.JCTree
import java.io.File

/**
 * Keep track of the current state of the compilation unit for a given file. As rules
 * begin to modify the underlying source file, 
 */
data class CompilationUnit(var jcCompilationUnit: JCTree.JCCompilationUnit, 
                           val parser: AstParser) {
   
    fun source() = File(jcCompilationUnit.sourceFile.toUri().path)
    
    fun reparse() {
        jcCompilationUnit = parser.reparse(jcCompilationUnit)
    }
}