package com.netflix.java.refactor.parse

import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.TypeCache
import com.sun.tools.javac.comp.Check
import com.sun.tools.javac.comp.Enter
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.main.Option
import com.sun.tools.javac.nio.JavacPathFileManager
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import com.sun.tools.javac.util.Options
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

/**
 * This parser is NOT thread-safe, as the Oracle parser maintains in-memory caches in static state.
 */
class OracleJdkParser(classpath: List<Path>? = null) : AbstractParser(classpath) {
    val context = Context()
    val typeCache = TypeCache.new()

    // Both of these must be declared before compiler, so that compiler doesn't attempt to register alternate
    // instances with context
    private val compilerLog = object : Log(context) {
        fun reset() {
            sourceMap.clear()
        }
    }

    private val pfm = JavacPathFileManager(context, true, Charset.defaultCharset())

    private val compiler = JavaCompiler(context)

    companion object {
        private val logger = LoggerFactory.getLogger(OracleJdkParser::class.java)
    }

    init {
        // otherwise, consecutive string literals in binary expressions are concatenated by the parser, losing the original
        // structure of the expression!
        Options.instance(context).put("allowStringFolding", "false")

        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position
        // for every tree element
        compiler.genEndPos = true
        compiler.keepComments = true

        compilerLog.setWriters(PrintWriter(object : Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                val log = String(cbuf.slice(off..(off + len - 1)).toCharArray())
                if(log.isNotBlank())
                    logger.warn(log)
            }

            override fun flush() {
            }

            override fun close() {
            }
        }))
    }

    override fun reset() {
        compilerLog.reset()
        pfm.flush()
        Check.instance(context).compiled.clear()
        typeCache.reset()
    }

    override fun parse(sourceFiles: List<Path>): List<Tr.CompilationUnit> {
        if (classpath != null) { // override classpath
            assert(context.get(JavaFileManager::class.java) === pfm)
            pfm.setLocation(StandardLocation.CLASS_PATH, classpath)
        }

        val fileObjects = pfm.getJavaFileObjects(*filterSourceFiles(sourceFiles).toTypedArray())
        val cus = fileObjects.map { Paths.get(it.toUri()) to compiler.parse(it) }.toMap()

        try {
            cus.values.enterAll()
            compiler.attribute(compiler.todo)
        } catch(ignore: Throwable) {
            // when symbol entering fails on problems like missing types, attribution can often times proceed
            // unhindered, but it sometimes cannot (so attribution is always a BEST EFFORT in the presence of errors)
        }

        return cus.map {
            val (path, cu) = it
            logger.trace("Building AST for {}", path.toAbsolutePath().fileName)
            OracleJdkParserVisitor(typeCache, path, path.toFile().readText()).scan(cu, Formatting.Reified.Empty) as Tr.CompilationUnit
        }
    }

    /**
     * Enter symbol definitions into each compilation unit's scope
     */
    private fun Collection<JCTree.JCCompilationUnit>.enterAll() {
        val enter = Enter.instance(context)
        val compilationUnits = com.sun.tools.javac.util.List.from(this.toTypedArray())
        enter.main(compilationUnits)
    }
}
