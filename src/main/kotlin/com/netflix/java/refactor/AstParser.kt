package com.netflix.java.refactor

import com.sun.tools.javac.comp.Check
import com.sun.tools.javac.comp.Enter
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.io.Writer
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation

class AstParser(val classpath: Iterable<File>?) {
    val context = Context()
    val compiler = JavaCompiler(context)

    private val logger = LoggerFactory.getLogger(AstParser::class.java)
    
    init {
        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position 
        // for every tree element
        compiler.genEndPos = true
        val log = Log.instance(context)
        log.setWriters(PrintWriter(object: Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                logger.debug(String(cbuf.slice(off..(off + len)).toCharArray()))
            }
            override fun flush() {}
            override fun close() {}
        }))
    }

    fun parseFiles(files: Iterable<File>): List<JCTree.JCCompilationUnit> {
        val fm = context.get(JavaFileManager::class.java)
        if(classpath != null) // override classpath
            (fm as JavacFileManager).setLocation(StandardLocation.CLASS_PATH, classpath)
        
        val cus = files.map { f ->
            compiler.parse(object : SimpleJavaFileObject(f.toURI(), JavaFileObject.Kind.SOURCE) {
                override fun getCharContent(ignoreEncodingErrors: Boolean) = f.readText()
            })
        }.enterAll()
        
        compiler.attribute(compiler.todo)
        
        return cus
    }
    
    fun reparse(cu: JCTree.JCCompilationUnit): JCTree.JCCompilationUnit {
        // this will cause the new AST to be re-entered and re-attributed
        val chk = Check.instance(context)
        cu.defs.filterIsInstance<JCTree.JCClassDecl>().forEach {
            chk.compiled.remove(it.sym.flatname)
        }
        return parseFiles(listOf(File(cu.sourcefile.toUri().path))).first()
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