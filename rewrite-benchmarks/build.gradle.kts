plugins {
    id("me.champeau.gradle.jmh") version "0.5.2"
}

dependencies {
    jmh(project(":rewrite-core"))
    jmh(project(":rewrite-java-11"))
    jmh("org.openjdk.jmh:jmh-core:latest.release")

    // Nebula doesn't like having jmhAnnotationProcessor without jmh so we just add it twice.
    jmh("org.openjdk.jmh:jmh-generator-annprocess:latest.release")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:latest.release")
}

jmh {
    fork = 1
    warmupIterations = 1
    iterations = 1
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
}
