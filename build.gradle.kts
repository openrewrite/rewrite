plugins {
    id("org.openrewrite.build.root") version "latest.release"
    id("org.openrewrite.build.java-base") version "latest.release"
    id("org.owasp.dependencycheck") version "latest.release"
}

configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    analyzers.assemblyEnabled = false
    analyzers.nodeAudit { enabled = false }
    analyzers.nodePackage { enabled = false }
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
