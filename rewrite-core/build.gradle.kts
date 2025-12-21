plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api("org.openrewrite.tools:jgit:latest.release")
    implementation("org.openrewrite.tools:java-object-diff:latest.release")
    implementation("io.quarkus.gizmo:gizmo:1.0.+")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("net.java.dev.jna:jna-platform:latest.release")

    api("org.jspecify:jspecify:latest.release")

    // Recipe marketplace
    implementation("com.univocity:univocity-parsers:latest.release")

    // Caffeine 2.x works with Java 8, Caffeine 3.x is Java 11 only.
    implementation("com.github.ben-manes.caffeine:caffeine:2.+")

    // For Levenshtein distance of mismatched recipes
    implementation("org.apache.commons:commons-text:latest.release")

    implementation("io.micrometer:micrometer-core:1.9.+")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.yaml:snakeyaml:latest.release")

    implementation("io.moderne:jsonrpc:latest.integration")
    implementation("org.objenesis:objenesis:latest.release")

    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation(project(":rewrite-test"))
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
    exclude("**/RpcObjectData.java")
}
