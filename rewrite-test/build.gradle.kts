plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api(project(":rewrite-core"))
    compileOnly("io.micrometer:micrometer-core:latest.release")
    api("org.junit.jupiter:junit-jupiter-api:latest.release")
    api("org.junit.jupiter:junit-jupiter-params:latest.release")

    implementation("org.assertj:assertj-core:latest.release")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
    implementation("org.slf4j:slf4j-api:1.7.36")

    if (System.getProperty("idea.active") != null &&
        System.getProperty("idea.sync.active") != null
    ) {
        // because the shaded jgit will not be available on the classpath
        // for the IntelliJ runner
        runtimeOnly("org.eclipse.jgit:org.eclipse.jgit:5.13.+")
    }
}
