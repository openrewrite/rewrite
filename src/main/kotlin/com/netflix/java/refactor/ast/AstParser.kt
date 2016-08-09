package com.netflix.java.refactor.ast

import com.netflix.java.refactor.CompilationUnit
import com.sun.tools.javac.comp.Check
import com.sun.tools.javac.comp.Enter
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.nio.JavacPathFileManager
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Path
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

class AstParser(val classpath: Iterable<Path>?) {
    val context = Context()

    // Both of these must be declared before compiler, so that compiler doesn't attempt to register alternate
    // instances with contest
    private val mutableLog = MutableSourceMapLog()
    private val pfm = JavacPathFileManager(context, true, Charset.defaultCharset())
    
    val compiler = JavaCompiler(context)
    
    private inner class MutableSourceMapLog(): Log(context) {
        fun removeFile(file: JavaFileObject) {
            sourceMap.remove(file)
        }
    }
    
    private val logger = LoggerFactory.getLogger(AstParser::class.java)
    
    init {
        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position 
        // for every tree element
        compiler.genEndPos = true
        mutableLog.setWriters(PrintWriter(object: Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                logger.debug(String(cbuf.slice(off..(off + len)).toCharArray()))
            }
            override fun flush() {}
            override fun close() {}
        }))
    }

    fun parseFiles(files: Iterable<Path>): List<JCTree.JCCompilationUnit> {
        if(classpath != null) { // override classpath
            assert(context.get(JavaFileManager::class.java) === pfm)
            pfm.setLocation(StandardLocation.CLASS_PATH, classpath)
        }
        
        val cus = pfm.getJavaFileObjects(*files.toList().toTypedArray())
                .map { compiler.parse(it) }
                .enterAll()
        
        compiler.attribute(compiler.todo)
        
        return cus
    }
    
    fun reparse(cu: CompilationUnit): JCTree.JCCompilationUnit {
        // this will cause the new AST to be re-entered and re-attributed
        val chk = Check.instance(context)

        cu.jcCompilationUnit.defs.filterIsInstance<JCTree.JCClassDecl>().forEach {
            chk.compiled.remove(it.sym.flatname)
        }
        
        // otherwise, when the parser attempts to set endPosTable on the DiagnosticSource of this file it will blow up
        // because the previous parsing iteration has already set one
        mutableLog.removeFile(pfm.getJavaFileObjects(cu.source()).first())
        
        return parseFiles(listOf(cu.source())).first()
    }
    
    /**
     * Enter symbol definitions into each compilation unit's scope
     */
    private fun List<JCTree.JCCompilationUnit>.enterAll(): List<JCTree.JCCompilationUnit> {
        val enter = Enter.instance(context)
        val compilationUnits = com.sun.tools.javac.util.List.from(this.toTypedArray())
        enter.main(compilationUnits)
        return this
    }
}