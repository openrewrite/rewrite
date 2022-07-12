plugins {
    id("org.openrewrite.java-library")
}

dependencies {
    implementation(project(":rewrite-java"))
    implementation("org.graphstream:gs-algo:2.0")
    implementation("org.graphstream:gs-core:2.0")
    implementation("org.graphstream:gs-ui:1.3")
    implementation("org.graphstream:gs-ui-swing:2.0")
    implementation("io.github.classgraph:classgraph:latest.release")
}

repositories {
    mavenCentral()
}
