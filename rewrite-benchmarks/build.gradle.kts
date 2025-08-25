plugins {
    id("org.openrewrite.build.java-base")
    id("org.openrewrite.build.recipe-repositories")
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    jmh("com.google.code.findbugs:jsr305:latest.release")
    jmh("org.projectlombok:lombok:latest.release")

    jmh(project(":rewrite-core"))
    jmh(project(":rewrite-java-21"))
    jmh(project(":rewrite-maven"))
    jmh("org.rocksdb:rocksdbjni:10.2.1")
    jmh("org.openjdk.jmh:jmh-core:latest.release")
    jmh("org.openjdk.jol:jol-core:latest.release")
    jmh("io.github.fastfilter:fastfilter:latest.release")
    jmh("org.xerial.snappy:snappy-java:1.1.10.7")

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
