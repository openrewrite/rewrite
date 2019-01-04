/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.parse

import com.netflix.rewrite.ast.Formatting
import com.netflix.rewrite.ast.Tr
import com.sun.tools.javac.comp.Check
import com.sun.tools.javac.comp.Enter
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.main.JavaCompiler
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
import javax.tools.StandardLocation

/**
 * This parser is NOT thread-safe, as the Oracle parser maintains in-memory caches in static state.
 */
class OracleJdkParser(classpath: List<Path>? = null,val charset: Charset= Charset.defaultCharset()) : AbstractParser(classpath) {
    val context = Context()

    // Both of these must be declared before compiler, so that compiler doesn't attempt to register alternate
    // instances with context
    private val compilerLog = object : Log(context) {
        fun reset() {
            sourceMap.clear()
        }
    }

    private val pfm = JavacFileManager(context, true, charset)

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
    }

    override fun parse(sourceFiles: List<Path>, relativeTo: Path?): List<Tr.CompilationUnit> {

        if (classpath != null) { // override classpath
            assert(context.get(JavaFileManager::class.java) === pfm)
            pfm.setLocation(StandardLocation.CLASS_PATH, classpath.map { it.toFile() })
        }

        val fileObjects = pfm.getJavaFileObjects(*filterSourceFiles(sourceFiles).map { it.toFile() }.toTypedArray())
        val cus = fileObjects.map { Paths.get(it.toUri()) to compiler.parse(it) }.toMap()

        try {
            cus.values.enterAll()
            compiler.attribute(compiler.todo)
        } catch(t: Throwable) {
            // when symbol entering fails on problems like missing types, attribution can often times proceed
            // unhindered, but it sometimes cannot (so attribution is always a BEST EFFORT in the presence of errors)
            logger.warn("Failed symbol entering or attribution", t)
        }

        return cus.map {
            val (path, cu) = it
            logger.trace("Building AST for {}", path.toAbsolutePath().fileName)
            OracleJdkParserVisitor(relativeTo?.relativize(path) ?: path, path.toFile().readText(charset)).scan(cu, Formatting.Empty) as Tr.CompilationUnit
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
