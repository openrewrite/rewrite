plugins {
    id("org.openrewrite.build.java-base")
    id("org.openrewrite.build.recipe-repositories")
    id("me.champeau.jmh") version "0.7.3"
}

val recipeModule by configurations.creating {
    isTransitive = true
}

dependencies {
    jmh("com.google.code.findbugs:jsr305:latest.release")
    jmh("org.projectlombok:lombok:latest.release")

    jmh(project(":rewrite-core"))
    jmh(project(":rewrite-java"))
    jmh(project(":rewrite-java-21"))
    jmh(project(":rewrite-javascript"))
    jmh(project(":rewrite-maven"))
    jmh("org.antlr:antlr4-runtime:4.13.2")
    jmh("org.rocksdb:rocksdbjni:10.2.1")
    jmh("org.openjdk.jmh:jmh-core:latest.release")
    jmh("org.openjdk.jol:jol-core:latest.release")
    jmh("io.github.fastfilter:fastfilter:latest.release")
    jmh("org.xerial.snappy:snappy-java:1.1.10.7")

    // Nebula doesn't like having jmhAnnotationProcessor without jmh so we just add it twice.
    jmh("org.openjdk.jmh:jmh-generator-annprocess:latest.release")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:latest.release")

    // Large recipe module for benchmarking recipe loading at scale
    recipeModule("org.openrewrite.recipe:rewrite-spring:latest.release")
}

// Write resolved recipeModule classpath to a file so JMH benchmarks can read it at runtime
val writeRecipeModuleClasspath by tasks.registering {
    val outputFile = layout.buildDirectory.file("recipeModuleClasspath.txt")
    outputs.file(outputFile)
    doLast {
        val paths = recipeModule.resolve().joinToString("\n") { it.absolutePath }
        outputFile.get().asFile.writeText(paths)
    }
}

tasks.named("jmh") {
    dependsOn(writeRecipeModuleClasspath)
}

jmh {
    fork = 1
    warmupIterations = 1
    iterations = 1
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
    val inc = project.findProperty("jmh.includes")?.toString()
    if (!inc.isNullOrBlank()) {
        includes = listOf(inc)
    }
    failOnError = true
    profilers = listOf("gc")
}
