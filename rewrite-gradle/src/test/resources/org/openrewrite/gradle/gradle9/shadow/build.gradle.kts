import java.text.SimpleDateFormat
import java.util.*

plugins {
    java
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.graphql-java"

if (JavaVersion.current() != JavaVersion.VERSION_11) {
    val msg = String.format("This build must be run with java 11 - you are running %s - gradle finds the JDK via JAVA_HOME=%s",
            JavaVersion.current(), System.getenv("JAVA_HOME"))
    throw GradleException(msg)
}

fun version(): String {
    val RELEASE_VERSION = "RELEASE_VERSION"
    if (System.getenv().containsKey(RELEASE_VERSION)) {
        val releaseVersion =  System.getenv(RELEASE_VERSION)
        println("using release version $releaseVersion")
        return releaseVersion
    }
    val version = SimpleDateFormat("yyyy-MM-dd\'T\'HH-mm-ss-mmm").format(Date())
    println("created development version: $version")
    return version
}
version = version()


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.jar {
    manifest {
        attributes(
                "Main-Class" to "Main"
        )
    }
}
repositories {
    mavenCentral()
}
tasks.withType<JavaCompile> {
    val compilerArgs = options.compilerArgs
    compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

dependencies {
    implementation("info.picocli:picocli:4.6.1")
    implementation("com.graphql-java:graphql-java:0.0.0-2021-08-01T03-54-24-35768a0e")
    annotationProcessor("info.picocli:picocli-codegen:4.6.1")
    testCompile("junit", "junit", "4.12")
}
