pluginManagement {
    repositories {
        maven {
            name = "codegenome"
            url = uri("https://artifacts.codegenomeproject.org/maven")
            credentials {
                username = providers.gradleProperty("codegenomeUsername").orNull ?: System.getenv("CODEGENOME_USERNAME")
                password = providers.gradleProperty("codegenomePassword").orNull ?: System.getenv("CODEGENOME_TOKEN")
            }
            content {
                includeGroupAndSubgroups("org.openrewrite")
                includeGroupAndSubgroups("io.moderne")
            }
        }
        gradlePluginPortal()
    }
}

rootProject.name = "rewrite"

// ---------------------------------------------------------------
// ------ Included Projects --------------------------------------
// ---------------------------------------------------------------

// Projects that can be selectively included/excluded via IDE.properties
val allProjects = listOf(
    "rewrite-android",
    "rewrite-benchmarks",
    "rewrite-bom",
    "rewrite-core",
    "rewrite-csharp",
    "rewrite-docker",
    "rewrite-gradle",
    "rewrite-go",
    "rewrite-groovy",
    "rewrite-hcl",
    "rewrite-java",
    "rewrite-java-tck",
    "rewrite-java-test",
    "rewrite-java-lombok",
    "rewrite-java-21",
    "rewrite-java-25",
    "rewrite-javascript",
    "rewrite-json",
    "rewrite-kotlin",
    "rewrite-maven",
    "rewrite-properties",
    "rewrite-protobuf",
    "rewrite-python",
    "rewrite-ruby",
    "rewrite-scala",
    "rewrite-test",
    "rewrite-toml",
    "rewrite-xml",
    "rewrite-yaml",
)

// Always included because their paths contain colons which can't be represented in IDE.properties
val alwaysIncluded = listOf(
    "rewrite-gradle-tooling-model:model",
    "rewrite-gradle-tooling-model:plugin",
)

val includedProjects = file("IDE.properties").let {
    if (it.exists() && (System.getProperty("idea.active") != null || System.getProperty("idea.sync.active") != null)) {
        val props = java.util.Properties()
        it.reader().use { reader ->
            props.load(reader)
        }
        allProjects.filter { it in props.keys }
    } else {
        allProjects
    }
}.toSet()

include(*(includedProjects + alwaysIncluded).toTypedArray())

if (System.getProperty("idea.active") == null &&
    System.getProperty("idea.sync.active") == null
) {
    include(
        "rewrite-java-8",
        "rewrite-java-11",
        "rewrite-java-17",
        "rewrite-java-21",
        "rewrite-java-25"
    )
}
