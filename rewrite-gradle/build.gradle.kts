import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.shadow")
    id("groovy")
}

repositories {
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases/")
    }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-groovy"))
    api(project(":rewrite-maven"))
    api("org.jetbrains:annotations:latest.release")
    compileOnly(project(":rewrite-test"))
    implementation(project(":rewrite-properties"))
    implementation("org.codehaus.groovy:groovy:latest.release")
    implementation("org.openrewrite.gradle.tooling:model:latest.release")

    compileOnly("org.gradle:gradle-base-services:latest.release")
    compileOnly("org.gradle:gradle-core-api:latest.release")
    compileOnly("org.gradle:gradle-language-groovy:latest.release")
    compileOnly("org.gradle:gradle-language-java:latest.release")
    compileOnly("org.gradle:gradle-logging:latest.release")
    compileOnly("org.gradle:gradle-messaging:latest.release")
    compileOnly("org.gradle:gradle-native:latest.release")
    compileOnly("org.gradle:gradle-process-services:latest.release")
    compileOnly("org.gradle:gradle-resources:latest.release")
    compileOnly("org.gradle:gradle-testing-base:latest.release")
    compileOnly("org.gradle:gradle-testing-jvm:latest.release")

    compileOnly("com.gradle:gradle-enterprise-gradle-plugin:latest.release")

    compileOnly("org.gradle:gradle-tooling-api:latest.release")

    testImplementation("org.gradle:gradle-tooling-api:latest.release")

    testImplementation(project(":rewrite-test")) {
        // because gradle-api fatjars this implementation already
        exclude("ch.qos.logback", "logback-classic")
    }
    testImplementation(project(":rewrite-maven"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.+")

    testRuntimeOnly("org.gradle:gradle-base-services:latest.release")
    testRuntimeOnly("org.gradle:gradle-core-api:latest.release")
    testRuntimeOnly("org.gradle:gradle-language-groovy:latest.release")
    testRuntimeOnly("org.gradle:gradle-language-java:latest.release")
    testRuntimeOnly("org.gradle:gradle-logging:latest.release")
    testRuntimeOnly("org.gradle:gradle-messaging:latest.release")
    testRuntimeOnly("org.gradle:gradle-native:latest.release")
    testRuntimeOnly("org.gradle:gradle-process-services:latest.release")
    testRuntimeOnly("org.gradle:gradle-resources:latest.release")
    testRuntimeOnly("org.gradle:gradle-testing-base:latest.release")
    testRuntimeOnly("org.gradle:gradle-testing-jvm:latest.release")
    testRuntimeOnly("com.gradle:gradle-enterprise-gradle-plugin:latest.release")
    testRuntimeOnly("com.google.guava:guava:latest.release")
    testRuntimeOnly(project(":rewrite-java-17"))
    testRuntimeOnly("org.projectlombok:lombok:latest.release")
}

tasks.withType<ShadowJar> {
    exclude("org/slf4j/impl/**.class")
    exclude("org/gradle/internal/logging/slf4j/**.class")
    dependencies {
        include(dependency("org.gradle:"))
        include(dependency("com.gradle:"))
    }
}

// This seems to be the only way to get the groovy compiler to emit java-8 compatible bytecode
// No option to explicitly target java-8 in the groovy compiler
tasks.withType<GroovyCompile> {
    this.javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}
