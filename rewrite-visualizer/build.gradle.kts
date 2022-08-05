plugins {
    id("org.openrewrite.java-library")
    id("maven-publish")
}

dependencies {
    implementation(project(":rewrite-java"))
    implementation("org.graphstream:gs-algo:2.0")
    implementation("org.graphstream:gs-core:2.0")
    implementation("org.graphstream:gs-ui:1.3")
    implementation("com.github.smehta23:gs-ui-swing:dd03181a99b1ef27b819429275724e7c9d743331")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("guru.nidi:graphviz-java:latest.release")
    implementation("guru.nidi:graphviz-java-all-j2v8:latest.release")
    implementation("org.slf4j:slf4j-api:1.7.+")

}

repositories {
    mavenCentral()
}
