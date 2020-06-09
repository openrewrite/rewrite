import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream
import me.lucko.jarrelocator.JarRelocator
import me.lucko.jarrelocator.Relocation

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        "classpath"("me.lucko:jar-relocator:1.4")
    }
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    implementation(files(shadeTools(toolsJar()).absolutePath))

    implementation("org.slf4j:slf4j-api:1.7.+")
    implementation("me.lucko:jar-relocator:1.4")

    testImplementation(project(":rewrite-test"))
}

tasks.withType<Javadoc> {
    exclude("**/JavaParser**")
}

fun toolsJar(): File {
    val toolsJar = File(".tools/tools.jar")
    if(!toolsJar.exists()) {
        toolsJar.parentFile.mkdirs()

        URL("https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u252-b09.1/OpenJDK8U-jdk_x64_windows_hotspot_8u252b09.zip")
                .openConnection()
                .getInputStream()
                .use {
                    ZipInputStream(it).use { zis ->
                        while(true) {
                            val ze = zis.nextEntry ?: break
                            if(ze.name.endsWith("tools.jar")) {
                                toolsJar.writeBytes(zis.readBytes())
                                break
                            }
                        }
                    }
                }
    }

    return toolsJar
}

fun shadeTools(toolsJar: File): File {
    val shadedToolsJar = File(".tools/tools-shaded.jar")
    if(!shadedToolsJar.exists()) {
        JarRelocator(toolsJar, shadedToolsJar, listOf(
                Relocation("com.sun", "java8tools.com.sun")
        )).run()
    }
    return shadedToolsJar
}
