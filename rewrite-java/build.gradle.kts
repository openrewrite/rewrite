import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.shadow")
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

    finalizedBy("licenseFormat")
}

// Only need checkstyle for the classes that we use to load its configuration files
val checkstyle = configurations.create("checkstyle")
configurations.named("compileOnly").configure {
    extendsFrom(checkstyle)
}
configurations.named("testImplementation").configure {
    extendsFrom(checkstyle)
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-yaml"))

    api("io.micrometer:micrometer-core:1.9.+")
    api("org.jetbrains:annotations:latest.release")

    antlrGeneration("org.antlr:antlr4:4.13.2") {
        exclude(group = "com.ibm.icu", module = "icu4j")
    }
    implementation("org.antlr:antlr4-runtime:4.13.2")
    // Pinned to 9.+ because 10.x does not support Java 8: https://checkstyle.sourceforge.io/#JRE_and_JDK
    checkstyle("com.puppycrawl.tools:checkstyle:9.+") {
        isTransitive = false
    }
    compileOnly(project(":rewrite-test"))
    compileOnly("org.junit.jupiter:junit-jupiter-api")
    compileOnly("org.assertj:assertj-core:latest.release")
    implementation("org.apache.commons:commons-text:latest.release")
    implementation("io.github.classgraph:classgraph:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations")

    // these are required for now so that `ChangeType` and `ChangePackage` can use the `Reference` trait
    runtimeOnly(project(":rewrite-properties"))
    runtimeOnly(project(":rewrite-xml"))

    implementation("org.ow2.asm:asm:latest.release")
    implementation("org.ow2.asm:asm-util:latest.release")

    testImplementation("org.yaml:snakeyaml:latest.release")
    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-java-test"))
    testRuntimeOnly(project(":rewrite-java-21"))
    testImplementation("com.tngtech.archunit:archunit:1.0.1")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.0.1")
    testImplementation("org.junit-pioneer:junit-pioneer:2.0.0")
    testImplementation("io.moderne:jsonrpc:latest.integration")

    // For use in ClassGraphTypeMappingTest
    testRuntimeOnly("org.eclipse.persistence:org.eclipse.persistence.core:3.0.2")
    testRuntimeOnly("org.slf4j:jul-to-slf4j:1.7.+")
    testRuntimeOnly("jakarta.validation:jakarta.validation-api:3.1.1")
}

tasks.withType<Javadoc>().configureEach {
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

tasks.named<ShadowJar>("shadowJar").configure {
    dependsOn(checkstyle)
    configurations = listOf(checkstyle)
    relocate("com.puppycrawl.tools.checkstyle", "org.openrewrite.tools.checkstyle")
}
