package org.openrewrite.java

import me.lucko.jarrelocator.JarRelocator
import me.lucko.jarrelocator.Relocation
import java.io.File
import java.net.URL
import java.util.*
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


object DownloadToolsJar {
    @JvmStatic
    fun main(args: Array<String>) {
        shade(minify(toolsJar()))
    }

    fun toolsJar(): File {
        val toolsJar = File(".tools/tools.jar")
        if (!toolsJar.exists()) {
            toolsJar.parentFile.mkdirs()

            URL("https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u252-b09.1/OpenJDK8U-jdk_x64_windows_hotspot_8u252b09.zip")
                    .openConnection()
                    .getInputStream()
                    .use {
                        ZipInputStream(it).use { zis ->
                            while (true) {
                                val ze = zis.nextEntry ?: break
                                if (ze.name.endsWith("tools.jar")) {
                                    println("Writing tools.jar...")
                                    toolsJar.writeBytes(zis.readAllBytes())
                                    println("Done!")
                                    break
                                }
                            }
                        }
                    }
        }

        return toolsJar
    }

    fun minify(toolsJar: File): File {
        val minified = File(".tools/tools-minified.jar")
        if(!minified.exists()) {
            minified.outputStream().use { minifiedFileOut ->
                ZipOutputStream(minifiedFileOut).use { minifiedOut ->
                    toolsJar.inputStream().use { toolsFileIn ->
                        ZipInputStream(toolsFileIn).use { toolsIn ->
                            while (true) {
                                val ze = toolsIn.nextEntry ?: break
                                if (ze.name.startsWith("com/sun/tools/javac") ||
                                        ze.name.startsWith("com/sun/tools/javadoc") ||
                                        ze.name.startsWith("com/sun/source")) {
                                    minifiedOut.putNextEntry(ze)
                                    minifiedOut.write(toolsIn.readAllBytes())
                                }
                            }
                        }
                    }
                }
            }
        }
        return minified
    }

    fun shade(toolsJar: File) {
        val rules: MutableList<Relocation> = ArrayList<Relocation>()
        rules.add(Relocation("com.google.common", "me.lucko.test.lib.guava"))

        val relocator = JarRelocator(toolsJar, File(".tools/tools-shaded.jar"), listOf(
                Relocation("com.sun", "java8tools.com.sun")
        ))

        relocator.run()
    }
}
