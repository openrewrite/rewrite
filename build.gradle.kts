plugins {
    id("org.openrewrite.build.root") version("2.5.6")
    id("org.openrewrite.build.java-base") version("2.5.6")
    id("org.owasp.dependencycheck") version("latest.release")
}

configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    analyzers.assemblyEnabled = false
    analyzers.nodeAuditEnabled = false
    analyzers.nodeEnabled = false
    failBuildOnCVSS = System.getenv("FAIL_BUILD_ON_CVSS")?.toFloatOrNull() ?: 9.0F
    format = System.getenv("DEPENDENCY_CHECK_FORMAT") ?: "HTML"
    nvd.apiKey = System.getenv("NVD_API_KEY")
    suppressionFile = "suppressions.xml"
}

repositories {
    mavenCentral()
}

allprojects {
    group = "org.openrewrite"
    description = "Eliminate tech-debt. Automatically."
}
