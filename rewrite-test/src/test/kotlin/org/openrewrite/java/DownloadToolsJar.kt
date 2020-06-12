/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java

import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DownloadToolsJar {
    @JvmStatic
    fun main(args: Array<String>) {
        minify(toolsJar())
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
}
