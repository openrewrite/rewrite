dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-hcl"))
    api(project(":rewrite-java"))
    api(project(":rewrite-json"))
    api(project(":rewrite-maven"))
    api(project(":rewrite-xml"))
    api(project(":rewrite-yaml"))
    api(project(":rewrite-properties"))

    api("org.eclipse.jgit:org.eclipse.jgit:latest.release")

    compileOnly("io.micrometer:micrometer-registry-prometheus:latest.release")

    implementation("com.squareup.okhttp3:okhttp:latest.release")

    api("org.junit.jupiter:junit-jupiter-api:latest.release")
    api("org.junit.jupiter:junit-jupiter-params:latest.release")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.assertj:assertj-core:latest.release")

    // later versions are compiled with Java 11, so are not usable in rewrite-java-8 tests
//    implementation("com.google.googlejavaformat:google-java-format:1.6")

    implementation("org.slf4j:slf4j-api:1.7.+")
    implementation("ch.qos.logback:logback-classic:1.0.13")

    // configuration generator for service providers
    implementation("com.google.auto.service:auto-service:latest.release")
}

tasks.withType<Javadoc> {
    isFailOnError = false
    exclude("org/openrewrite/java/**")
}
