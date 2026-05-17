plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-gradle"))
    api(project(":rewrite-java"))
    api(project(":rewrite-kotlin"))
    api(project(":rewrite-xml"))
    api("org.jetbrains:annotations:latest.release")

    implementation(project(":rewrite-properties"))
    implementation(project(":rewrite-toml"))

    compileOnly(project(":rewrite-test"))

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-java-tck"))

    testRuntimeOnly(project(":rewrite-java-21"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Javadoc compiler doesn't understand Lombok's `onConstructor_`, the same way it doesn't on
// the equivalent markers in rewrite-gradle.
tasks.withType<Javadoc>().configureEach {
    exclude(
        "**/AndroidProject**",
        "**/AndroidSdkVersions**",
        "**/AndroidBuildFeatures**",
        "**/AndroidBuildType**",
        "**/AndroidProductFlavor**",
        "**/AndroidVariant**",
        "**/AndroidSourceSet**"
    )
    // Phase 1 ships only marker types, all excluded above; javadoc would otherwise fail
    // with "No public or protected classes found to document". Phases C/D add real classes.
    isFailOnError = false
}
