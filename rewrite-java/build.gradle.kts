plugins {
    id("org.openrewrite.build.language-library")
}

val antlrGeneration by configurations.creating {
    extendsFrom(configurations.implementation.get())
}

tasks.register<JavaExec>("generateAntlrSources") {
    mainClass.set("org.antlr.v4.Tool")

    args = listOf(
            "-o", "src/main/java/org/openrewrite/java/internal/grammar",
            "-package", "org.openrewrite.java.internal.grammar",
            "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = antlrGeneration
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-yaml"))
    api(project(":rewrite-xml"))

    api("io.micrometer:micrometer-core:1.9.+")
    api("org.jetbrains:annotations:latest.release")

    antlrGeneration("org.antlr:antlr4:4.11.1")
    implementation("org.antlr:antlr4-runtime:4.11.1")
    compileOnly("com.puppycrawl.tools:checkstyle:9.+") { // Pinned to 9.+ because 10.x does not support Java 8: https://checkstyle.sourceforge.io/#JRE_and_JDK
        isTransitive = false
    }
    compileOnly(project(":rewrite-test"))
    compileOnly("org.junit.jupiter:junit-jupiter-api:latest.release")
    compileOnly("org.assertj:assertj-core:latest.release")
    implementation("org.apache.commons:commons-lang3:latest.release")
    implementation("org.apache.commons:commons-text:latest.release")
    implementation("io.github.classgraph:classgraph:latest.release")

    implementation("org.xerial.snappy:snappy-java:1.1.10.+")

    api("com.fasterxml.jackson.core:jackson-annotations")

    implementation("io.github.fastfilter:fastfilter:latest.release")

    implementation("org.ow2.asm:asm:latest.release")
    implementation("org.ow2.asm:asm-util:latest.release")

    testImplementation("org.yaml:snakeyaml:latest.release")
    testImplementation("com.puppycrawl.tools:checkstyle:9.+") { // Pinned to 9.+ because 10.x does not support Java 8: https://checkstyle.sourceforge.io/#JRE_and_JDK
        isTransitive = false
    }
    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-java-test"))
    testRuntimeOnly(project(":rewrite-java-17"))
    testImplementation("com.tngtech.archunit:archunit:1.0.1")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.0.1")

    // For use in ClassGraphTypeMappingTest
    testRuntimeOnly("org.eclipse.persistence:org.eclipse.persistence.core:3.0.2")

    testRuntimeOnly("org.slf4j:jul-to-slf4j:1.7.+")
}

tasks.withType<Javadoc> {
    // generated ANTLR sources violate doclint
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")

    // Items besides JavaParser due to lombok error which looks similar to this:
    //     openrewrite/rewrite/rewrite-java/src/main/java/org/openrewrite/java/OrderImports.java:42: error: cannot find symbol
    // @AllArgsConstructor(onConstructor_=@JsonCreator)
    //                     ^
    //   symbol:   method onConstructor_()
    //   location: @interface AllArgsConstructor
    // 1 error
    exclude("**/JavaParser**", "**/ChangeMethodTargetToStatic**", "**/J.java")
}
