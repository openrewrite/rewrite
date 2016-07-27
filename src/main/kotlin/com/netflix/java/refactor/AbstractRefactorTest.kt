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
    
    fun parseJava(target: File, vararg otherFiles: File) =
        parseJava(target, otherFiles.toList(), null)
    
    fun parseJava(target: File, otherFiles: Iterable<File>, classpath: Iterable<File>? = null): JavaSource {
        val parser = AstParser(classpath)
        val allFiles = otherFiles.plus(target)
        val cu = parser.parseFiles(allFiles.toList()).last()
        return JavaSource(CompilationUnit(cu, parser))
    }
    
    fun java(sourceStr: String): File {
        val source = temp.newFile(fullyQualifiedName(sourceStr.trimMargin()) + ".java")
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

    val classMatcher = Pattern.compile("\\s*(class|interface|enum)\\s+(\\w+)").matcher(sourceStr)
    return if (classMatcher.find()) pkg + classMatcher.group(2) else null
}
