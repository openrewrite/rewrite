plugins {
    id("org.owasp.dependencycheck")
}

configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    skipConfigurations = listOf("integTestImplementationDependenciesMetadata")
    analyzers.assemblyEnabled = false
    failBuildOnCVSS = 9.0F
    scanProjects = listOf("rewrite-core",
                              "rewrite-gradle",
                              "rewrite-groovy",
                              "rewrite-hcl",
                              "rewrite-java",
                              "rewrite-java-8",
                              "rewrite-java-11",
                              "rewrite-json",
                              "rewrite-maven",
                              "rewrite-properties",
                              "rewrite-protobuf",
                              "rewrite-xml",
                              "rewrite-yaml",
                              "rewrite-test",
                              "rewrite-bom",
                              "rewrite-benchmarks")
}
