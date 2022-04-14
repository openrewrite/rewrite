plugins {
    id("org.owasp.dependencycheck")
}

configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    skipConfigurations = listOf("integTestImplementationDependenciesMetadata")
    analyzers.assemblyEnabled = false
    failBuildOnCVSS = 9.0F
}
