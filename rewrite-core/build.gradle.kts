import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.shadow")
}

dependencies {
    // Pin to 5.x for Java 8, as 6.x requires Java 11
    compileOnly("org.eclipse.jgit:org.eclipse.jgit:5.13.+")

    implementation("org.openrewrite.tools:java-object-diff:latest.release")

    implementation("io.quarkus.gizmo:gizmo:1.0.+")

    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("net.java.dev.jna:jna-platform:latest.release")

    // Pinning okhttp while waiting on 5.0.0
    // https://github.com/openrewrite/rewrite/issues/1479
    compileOnly("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("org.apache.commons:commons-compress:latest.release")

    implementation("io.micrometer:micrometer-core:1.9.+")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.yaml:snakeyaml:latest.release")

    testImplementation(project(":rewrite-test"))
    testImplementation("org.eclipse.jgit:org.eclipse.jgit:5.13.+")
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include(dependency("org.eclipse.jgit:"))
    }
    relocate("org.eclipse.jgit", "org.openrewrite.shaded.jgit")
    metaInf {
        from("$rootDir/LICENSE")
        from("$rootDir/NOTICE")
    }
}

tasks.named<Test>("test").configure {
    dependsOn(shadowJar)
    classpath = files(shadowJar, sourceSets.test.get().output, configurations.testRuntimeClasspath)
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
    exclude("**/GitProvenance.java")
}
