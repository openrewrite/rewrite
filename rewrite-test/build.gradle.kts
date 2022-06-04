plugins {
    id("org.openrewrite.java-library")
    id("org.openrewrite.maven-publish")
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-gradle"))
    api(project(":rewrite-groovy"))
    api(project(":rewrite-hcl"))
    api(project(":rewrite-java"))
    api(project(":rewrite-json"))
    api(project(":rewrite-maven"))
    api(project(":rewrite-properties"))
    api(project(":rewrite-protobuf"))
    api(project(":rewrite-xml"))
    api(project(":rewrite-yaml"))

    api("org.eclipse.jgit:org.eclipse.jgit:5.13.+")

    compileOnly("io.micrometer:micrometer-registry-prometheus:1.+")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    api("org.junit.jupiter:junit-jupiter-api:latest.release")
    api("org.junit.jupiter:junit-jupiter-params:latest.release")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.assertj:assertj-core:latest.release")

    // later versions are compiled with Java 11, so are not usable in rewrite-java-8 tests
//    implementation("com.google.googlejavaformat:google-java-format:1.6")

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // configuration generator for service providers
    implementation("com.google.auto.service:auto-service:latest.release")

    implementation("org.apache.hbase:hbase-shaded-client:2.4.11")
    testRuntimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin:latest.release")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

tasks.withType<Javadoc> {
    isFailOnError = false
    exclude("org/openrewrite/java/**")
}
