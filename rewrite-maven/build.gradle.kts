import nl.javadude.gradle.plugins.license.LicenseExtension

plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api(project(":rewrite-xml"))
    api("org.jetbrains:annotations:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations")

    implementation(project(":rewrite-core"))
    compileOnly(project(":rewrite-test"))

    // Caffeine 2.x works with Java 8, Caffeine 3.x is Java 11 only.
    implementation("com.github.ben-manes.caffeine:caffeine:2.+")

    implementation("org.antlr:antlr4-runtime:4.13.2")
    implementation("dev.failsafe:failsafe:latest.release")
    //implementation(platform("com.fasterxml.jackson:jackson-bom:2.20.1"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.17.3"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")

    // needed by AddDependency
    implementation(project(":rewrite-java"))

    compileOnly("org.rocksdb:rocksdbjni:10.2.1")
    compileOnly(project(":rewrite-yaml"))
    implementation(project(":rewrite-properties"))

    implementation("io.micrometer:micrometer-core:1.9.+")

    implementation("org.apache.commons:commons-text:latest.release")

    testImplementation(project(":rewrite-test"))

    testImplementation("com.squareup.okhttp3:okhttp:4.+")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.+")
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.+")
    testImplementation("com.squareup.okio:okio-jvm:3.9.1")
    testImplementation("org.mapdb:mapdb:latest.release")

    testRuntimeOnly("org.mapdb:mapdb:latest.release")
    testRuntimeOnly(project(":rewrite-java-21"))
    testRuntimeOnly("org.rocksdb:rocksdbjni:10.2.1")
}

tasks.register<JavaExec>("generateAntlrSources") {
    mainClass.set("org.antlr.v4.Tool")

    args = listOf(
            "-o", "src/main/java/org/openrewrite/maven/internal/grammar",
            "-package", "org.openrewrite.maven.internal.grammar",
            "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = sourceSets["main"].runtimeClasspath

    finalizedBy("licenseFormat")
}

tasks.withType<Javadoc>().configureEach {
    // generated ANTLR sources violate doclint
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")

    // TODO
    // openrewrite/rewrite/rewrite-maven/src/main/java/org/openrewrite/maven/AddDependency.java:29: error: cannot find symbol
    // @AllArgsConstructor(onConstructor_=@JsonCreator)
    //                     ^
    //   symbol:   method onConstructor_()
    //   location: @interface AllArgsConstructor
    // 1 error
    exclude("**/VersionRangeParser**", "**/AddDependency**", "**/MavenResolutionResult**")
}

configure<LicenseExtension> {
    excludePatterns.add("**/unresolvable.txt")
}

tasks.register<JavaExec>("generateRecipeMarketplace") {
    group = "build"
    description = "Generate recipe marketplace CSV from the rewrite-maven JAR"

    dependsOn("jar")

    mainClass.set("org.openrewrite.maven.marketplace.MavenRecipeMarketplaceGenerator")

    val jarFile = tasks.jar.get().archiveFile.get().asFile
    val outputCsv = project.file("build/recipes.csv")
    val groupId = project.group.toString()
    val artifactId = project.name

    args = listOf("$groupId:$artifactId", outputCsv.absolutePath, jarFile.absolutePath) +
           configurations.runtimeClasspath.get().files.map { it.absolutePath }

    classpath = configurations.runtimeClasspath.get() + files(jarFile)
}
