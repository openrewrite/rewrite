plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    implementation("org.assertj:assertj-core:latest.release")
    implementation(project(":rewrite-java"))
    implementation(project(":rewrite-groovy"))
    implementation(project(":rewrite-test"))

    testImplementation("io.github.classgraph:classgraph:latest.release")
    testImplementation("org.junit-pioneer:junit-pioneer:2.0.0")
    testRuntimeOnly(project(":rewrite-java-17"))
    testRuntimeOnly("junit:junit:4.13.2") {
        because("Used for RemoveUnneededAssertionTest")
    }
    testRuntimeOnly("org.apache.hbase:hbase-shaded-client:2.4.11")
    testRuntimeOnly("com.google.guava:guava:latest.release")
    testRuntimeOnly("org.mapstruct:mapstruct:latest.release")
    testRuntimeOnly("org.projectlombok:lombok:latest.release")
    testRuntimeOnly("org.apache.commons:commons-lang3:latest.release")
    testRuntimeOnly(project(":rewrite-yaml"))
    testImplementation(project(":rewrite-maven"))
}

tasks.withType<Javadoc> {
    isFailOnError = false
    exclude("org/openrewrite/java/**")
}

tasks.named<JavaCompile>("compileTestJava") {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()

    options.release.set(null as Int?) // remove `--release 8` set in `org.openrewrite.java-base`
}

tasks.withType<Test> {
    systemProperty("junit.jupiter.extensions.autodetection.enabled", true)
}

configurations.all {
    if (isCanBeConsumed) {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
    }
}
