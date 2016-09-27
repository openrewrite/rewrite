package com.netflix.java.refactor

import com.netflix.java.refactor.ast.AstParser
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.regex.Pattern

abstract class AbstractRefactorTest {
    @JvmField @Rule
    val temp = TemporaryFolder()
    
    fun parseJava(target: File, vararg otherFiles: File): JavaSource<*> =
        parseJava(target, otherFiles.toList(), null)
    
    fun parseJava(target: File, otherFiles: Iterable<File>, classpath: Iterable<File>? = null): JavaSource<*> {
        val parser = AstParser(classpath?.map { it.toPath() })
        val allFiles = otherFiles.plus(target)
        val cu = parser.parseFiles(allFiles.map { it.toPath() }).last()
        return JavaSource(CompilationUnit(cu, parser), Unit)
    }
    
    fun java(sourceStr: String): File {
        val source = temp.newFile(fullyQualifiedName(sourceStr.trimMargin())!!.substringAfterLast(".") + ".java")
        source.writeText(sourceStr.trimMargin())
        return source
    }
    
    fun assertRefactored(file: File, refactored: String) {
        assertEquals(refactored.trimMargin(), file.readText())
    }
}

fun fullyQualifiedName(sourceStr: String): String? {
    val pkgMatcher = Pattern.compile("\\s*package\\s+([\\w\\.]+)").matcher(sourceStr)
    val pkg = if (pkgMatcher.find()) pkgMatcher.group(1) + "." else ""

    val classMatcher = Pattern.compile("\\s*(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)").matcher(sourceStr)
    return if (classMatcher.find()) pkg + classMatcher.group(3) else null
}
