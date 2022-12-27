plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api(project(":rewrite-core"))

    compileOnly("io.micrometer:micrometer-registry-prometheus:1.9+")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    api("org.junit.jupiter:junit-jupiter-api:latest.release")
    api("org.junit.jupiter:junit-jupiter-params:latest.release")

    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.assertj:assertj-core:latest.release")

    // later versions are compiled with Java 11, so are not usable in rewrite-java-8 tests
//    implementation("com.google.googlejavaformat:google-java-format:1.6")

    implementation("org.slf4j:slf4j-api:1.7.36")

    // configuration generator for service providers
    implementation("com.google.auto.service:auto-service:latest.release")

    implementation("org.apache.hbase:hbase-shaded-client:2.4.11")

    // FindRepeatableAnnotationsTest
    implementation("org.mapstruct:mapstruct:latest.release")
}
