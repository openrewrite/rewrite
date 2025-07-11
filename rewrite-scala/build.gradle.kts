plugins {
    id("org.openrewrite.build.language-library")
    scala
}

dependencies {
    api(project(":rewrite-java"))

    // Scala 3 compiler (dotty) and library
    implementation("org.scala-lang:scala3-compiler_3:latest.release")
    implementation("org.scala-lang:scala3-library_3:latest.release")

    compileOnly(project(":rewrite-test"))
    compileOnly("org.slf4j:slf4j-api:1.7.+")

    api("io.micrometer:micrometer-core:1.9.+")

    api("org.jetbrains:annotations:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-java-test"))
    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")
}

// Configure Scala source sets and compilation order
sourceSets {
    main {
        scala {
            srcDirs("src/main/scala")
        }
    }
}

// Ensure Scala compiles before Java
tasks.named<JavaCompile>("compileJava") {
    dependsOn("compileScala")
    // Add Scala classes to Java classpath
    classpath = classpath.plus(files(tasks.named("compileScala").get().outputs))
}

// Configure the Scala compiler classpath to include Java classes
tasks.named<ScalaCompile>("compileScala") {
    classpath = sourceSets.main.get().compileClasspath
}