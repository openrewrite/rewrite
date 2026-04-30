plugins {
    id("org.openrewrite.build.language-library")
}

recipeDependencies {
    parserClasspath("jakarta.persistence:jakarta.persistence-api:3.1.0")
    testParserClasspath("jakarta.validation:jakarta.validation-api:3.0.2")
    testParserClasspath("javax.validation:validation-api:1.1.0.Final")
    testParserClasspath("org.hibernate:hibernate-validator:5.4.3.Final")
}

dependencies {
    implementation("org.assertj:assertj-core:3.+") // CVE-2026-24400 in 4.0.0-M1 and no higher versions available
    implementation(project(":rewrite-java"))
    implementation(project(":rewrite-test"))

    testImplementation(project(":rewrite-groovy"))
    testImplementation(project(":rewrite-kotlin"))
    testImplementation("io.github.classgraph:classgraph:latest.release")
    testImplementation("org.junit-pioneer:junit-pioneer:2.0.0")
    testRuntimeOnly(project(":rewrite-java-21"))
    testRuntimeOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
    testRuntimeOnly("org.apache.hbase:hbase-shaded-client:2.4.11")
    testRuntimeOnly("com.google.guava:guava:latest.release")
    testRuntimeOnly("org.mapstruct:mapstruct:latest.release")
    testRuntimeOnly(project(":rewrite-yaml"))
    testImplementation(project(":rewrite-properties"))
    testImplementation(project(":rewrite-xml"))
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
    exclude("org/openrewrite/java/**")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("junit.jupiter.extensions.autodetection.enabled", true)
}

configurations.all {
    if (isCanBeConsumed) {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
    }
}
