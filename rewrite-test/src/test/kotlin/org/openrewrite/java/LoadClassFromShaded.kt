package org.openrewrite.java

import java.io.File
import java.net.URLClassLoader

object LoadClassFromShaded {
    @JvmStatic
    fun main(args: Array<String>) {
        println(
                URLClassLoader(arrayOf(File(".tools/tools-shaded.jar").toURI().toURL()))
                        .loadClass("java8tools.com.sun.tools.javac.util.Context")
                        .getDeclaredConstructor()
                        .newInstance()::class.qualifiedName
        )
    }
}
