plugins {
    id("org.openrewrite.java-library")
    id("org.openrewrite.maven-publish")
}

// run manually with -x compileKotlin when you need to regenerate
tasks.register<JavaExec>("generateAntlrSources") {
    main = "org.antlr.v4.Tool"

    args = listOf(
            "-o", "src/main/java/org/openrewrite/java/internal/grammar",
            "-package", "org.openrewrite.java.internal.grammar",
            "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = sourceSets["main"].runtimeClasspath
}

dependencies {
    api(project(":rewrite-core"))

    api("io.micrometer:micrometer-core:1.+")
    api("org.jetbrains:annotations:latest.release")

    implementation("org.antlr:antlr4:4.9.+")
    compileOnly("com.puppycrawl.tools:checkstyle:10.+") {
        isTransitive = false
    }
    implementation("commons-lang:commons-lang:latest.release")
    implementation("io.github.classgraph:classgraph:latest.release")

    implementation("org.xerial.snappy:snappy-java:1.1.8.4")

    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")

    implementation("org.ow2.asm:asm:latest.release")
    implementation("org.ow2.asm:asm-util:latest.release")

    testImplementation("org.yaml:snakeyaml:latest.release")
    testImplementation("com.puppycrawl.tools:checkstyle:10.+") {
        isTransitive = false
    }
    testImplementation(project(":rewrite-test"))

    // For use in ClassGraphTypeMappingTest
    testRuntimeOnly("org.eclipse.persistence:org.eclipse.persistence.core:3.0.2")

    testRuntimeOnly("org.slf4j:jul-to-slf4j:1.7.+")
}

tasks.withType<Javadoc> {
    // generated ANTLR sources violate doclint
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")

    // ChangePackage and OrderImports due to lombok error which looks similar to this:
    //     openrewrite/rewrite/rewrite-java/src/main/java/org/openrewrite/java/OrderImports.java:42: error: cannot find symbol
    // @AllArgsConstructor(onConstructor_=@JsonCreator)
    //                     ^
    //   symbol:   method onConstructor_()
    //   location: @interface AllArgsConstructor
    // 1 error
    exclude("**/JavaParser**", "**/ChangePackage**", "**/OrderImports**")
}
