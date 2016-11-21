package com.netflix.java.refactor.parse

import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.TypeCache
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

interface Parser {
    /**
     * Clear any in-memory parser caches that may prevent reparsing of classes with the same fully qualified name in
     * different rounds
     */
    fun reset(): Unit

    fun parse(sourceFiles: List<Path>): List<Tr.CompilationUnit>

    fun parse(source: String, whichDependsOn: String) =
            parse(source, listOf(whichDependsOn))

    fun parse(source: String, whichDependOn: List<String>) =
            parse(source, *whichDependOn.toTypedArray())

    fun parse(source: String, vararg whichDependOn: String): Tr.CompilationUnit {
        fun simpleName(sourceStr: String): String? {
            val classMatcher = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)").matcher(sourceStr)
            return if (classMatcher.find()) classMatcher.group(3) else null
        }

        val temp = Files.createTempDirectory("sources").toFile()

        fun sourceFile(source: String): Path {
            val file = File(temp, "${simpleName(source)}.java")
            file.writeText(source.trimMargin())
            return file.toPath()
        }

        val dependencies = whichDependOn.map { it.trimMargin() }.map(::sourceFile)
        val sources = dependencies + listOf(sourceFile(source.trimMargin()))

        try {
            return parse(sources).last()
        } finally {
            temp.deleteRecursively()
        }
    }
}

abstract class AbstractParser(val classpath: List<Path>?): Parser {
    protected fun filterSourceFiles(sourceFiles: List<Path>) =
            sourceFiles.filter { it.fileName.toString().endsWith(".java") }.toList()
}