package org.openrewrite.java;

import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import kotlin.streams.toList

object RewriteJavaProjectOnDisk {
    @JvmStatic
    fun main(args: Array<String>) {
        val srcDir = Paths.get(args[0])
        val recipe: Recipe = Class.forName(args[1]).getDeclaredConstructor().newInstance() as Recipe

        val predicate = BiPredicate<Path, BasicFileAttributes> { p, bfa ->
            bfa.isRegularFile && p.fileName.toString().endsWith(".java")
        }

        val paths = Files.find(srcDir, 999, predicate)
            .limit(if (args.size > 2) args[2].toLong() else Long.MAX_VALUE)
            .toList()

        val parser: JavaParser = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(false) // optional, for quiet parsing
            .build()

        val sourceFiles: List<SourceFile> = parser.parse(paths, srcDir)
        recipe.run(sourceFiles).map {
            println(it.diff())
        }
    }
}
