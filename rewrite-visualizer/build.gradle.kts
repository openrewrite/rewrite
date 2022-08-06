plugins {
    id("org.openrewrite.java-library")
    id("org.openrewrite.maven-publish")
}

dependencies {
    implementation(project(":rewrite-java"))
    implementation("guru.nidi:graphviz-java-all-j2v8:latest.release") {
        because("used to visualize control flow")
    }
}
