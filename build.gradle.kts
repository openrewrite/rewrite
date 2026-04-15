import java.util.Calendar

plugins {
    id("org.openrewrite.build.root") version "latest.release"
    id("org.openrewrite.build.java-base") version "latest.release"
}

buildscript {
    configurations.classpath {
        exclude(group = "org.openrewrite", module = "rewrite-yaml")
        exclude(group = "org.openrewrite", module = "rewrite-hcl")
    }
}

repositories {
    mavenCentral()
}

allprojects {
    group = "org.openrewrite"
    description = "Eliminate tech-debt. Automatically."
}

subprojects {
    tasks.withType<JavaExec>().configureEach {
        if (name == "generateAntlrSources") {
            doLast {
                val idx = args?.indexOf("-o") ?: return@doLast
                if (idx < 0 || idx + 1 >= args!!.size) return@doLast
                val rootPrefix = rootProject.projectDir.absolutePath.replace("\\", "/") + "/"
                val year = Calendar.getInstance().get(Calendar.YEAR)
                val licenseHeader = "/*\n" + rootProject.file("gradle/licenseHeader.txt")
                    .readText().trim()
                    .replace("\${year}", year.toString())
                    .lines()
                    .joinToString("\n") { " * $it".trimEnd() } + "\n */\n"
                project.file(args!![idx + 1]).walk().filter { it.extension == "java" }.forEach { file ->
                    file.writeText(licenseHeader + file.readLines().joinToString("\n") { line ->
                        line.trimEnd().replace("// Generated from $rootPrefix", "// Generated from ")
                    } + "\n")
                }
            }
        }
    }
}

// Use this task locally between different dependency check runs to have updated analysis:
// OSSINDEX_PASSWORD=... OSSINDEX_USERNAME=... gradle cleanReports dCAg --no-parallel
tasks.register<Delete>("cleanReports") {
    description = "Removes build/reports folder from all modules"
    group = "owasp dependency-check"
    delete(allprojects.map { it.layout.buildDirectory.dir("reports") })
}
