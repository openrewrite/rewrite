pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

include(
    "rewrite-core",
    "rewrite-groovy",
    "rewrite-hcl",
    "rewrite-java",
    "rewrite-java-8",
    "rewrite-java-11",
    "rewrite-json",
    "rewrite-maven",
    "rewrite-properties",
    "rewrite-xml",
    "rewrite-yaml",
    "rewrite-test",
    "rewrite-benchmarks"
)
