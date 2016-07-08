package com.netflix.java.refactor

import com.sun.tools.javac.comp.Enter
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.io.File
import java.net.URI
import java.util.regex.Pattern
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

class AstParser {
    companion object {
        fun fullyQualifiedName(sourceStr: String): String? {
            val pkgMatcher = Pattern.compile("\\s*package\\s+([\\w\\.]+)").matcher(sourceStr)
            val pkg = if (pkgMatcher.find()) pkgMatcher.group(1) + "." else ""

            val classMatcher = Pattern.compile("\\s*(class|interface|enum)\\s+(\\w+)").matcher(sourceStr)
            return if (classMatcher.find()) pkg + classMatcher.group(2) else null
        }
    }
    
    val context = Context()
    val compiler = JavaCompiler(context)
    
    init {
        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position 
        // for every tree element
        compiler.genEndPos = true
    }
    
    fun parseFiles(files: Iterable<File>): List<JCTree.JCCompilationUnit> =
        files.map { f ->
            compiler.parse(object: SimpleJavaFileObject(f.toURI(), JavaFileObject.Kind.SOURCE) {
                override fun getCharContent(ignoreEncodingErrors: Boolean) = f.readText()
            })
        }.enterAll()

    /**
     * For use in tests
     */
    fun parseSources(vararg fileSources: String): List<JCTree.JCCompilationUnit> =
        fileSources.map { source ->
            val sourceUri = URI.create("string:///" + fullyQualifiedName(source)?.replace("\\.".toRegex(), "/") + ".java") ?:
                    throw IllegalArgumentException("Source must contain a class definition")

            compiler.parse(object : SimpleJavaFileObject(sourceUri, JavaFileObject.Kind.SOURCE) {
                override fun getCharContent(ignoreEncodingErrors: Boolean) = source
            })
        }.enterAll()

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