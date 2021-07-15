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

// So as to ensure that com.puppycrawl.tools:checkstyle is truly an optional dependency, keep things that depend on it in their own sourceSet
sourceSets {
    create("checkstyle") {
        compileClasspath += sourceSets.getByName("main").output
        runtimeClasspath += sourceSets.getByName("main").output
    }
    named("test") {
        compileClasspath += sourceSets.getByName("checkstyle").output
        runtimeClasspath += sourceSets.getByName("checkstyle").output
    }
}

java {
    registerFeature("checkstyle") {
        usingSourceSet(sourceSets.getByName("checkstyle"))
    }
}

dependencies {
    api(project(":rewrite-core"))

    api("io.micrometer:micrometer-core:latest.release")
    api("org.jetbrains:annotations:latest.release")

    implementation("org.antlr:antlr4:4.8-1")
    "checkstyleImplementation"("com.puppycrawl.tools:checkstyle:latest.release")
    "checkstyleImplementation"(project(":rewrite-core"))
    implementation("commons-lang:commons-lang:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations:2.12.+")

    implementation("org.ow2.asm:asm:latest.release")
    implementation("org.ow2.asm:asm-util:latest.release")

    testImplementation("org.yaml:snakeyaml:latest.release")
    testImplementation("com.puppycrawl.tools:checkstyle:latest.release")
}

tasks.named<Jar>("jar") {
    from(sourceSets.getByName("checkstyle").output)
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
