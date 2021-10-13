import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
    id("nebula.maven-shadow-publish")
}

repositories {
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases-local/")
    }
}

dependencies {
    api(project(":rewrite-groovy"))

    runtimeOnly("org.codehaus.groovy:groovy-ant:latest.release")

    compileOnly("org.gradle:gradle-base-services:latest.release")
    compileOnly("org.gradle:gradle-core-api:latest.release")
    compileOnly("org.gradle:gradle-language-groovy:latest.release")
    compileOnly("org.gradle:gradle-language-java:latest.release")
    compileOnly("org.gradle:gradle-logging:latest.release")
    compileOnly("org.gradle:gradle-messaging:latest.release")
    compileOnly("org.gradle:gradle-native:latest.release")
    compileOnly("org.gradle:gradle-process-services:latest.release")

    implementation("com.squareup.okhttp3:okhttp:latest.release")

    // FIXME: switch to `latest.release`
    // when https://github.com/resilience4j/resilience4j/issues/1472 is resolved
    implementation("io.github.resilience4j:resilience4j-retry:1.7.0")

    testImplementation(project(":rewrite-test")) {
        // because gradle-api fatjars this implementation already
        exclude("ch.qos.logback", "logback-classic")
    }

    testRuntimeOnly("org.gradle:gradle-base-services:latest.release")
    testRuntimeOnly("org.gradle:gradle-core-api:latest.release")
    testRuntimeOnly("org.gradle:gradle-language-groovy:latest.release")
    testRuntimeOnly("org.gradle:gradle-language-java:latest.release")
    testRuntimeOnly("org.gradle:gradle-logging:latest.release")
    testRuntimeOnly("org.gradle:gradle-messaging:latest.release")
    testRuntimeOnly("org.gradle:gradle-native:latest.release")
    testRuntimeOnly("org.gradle:gradle-process-services:latest.release")
}

tasks.withType<ShadowJar> {
    configurations = listOf(project.configurations.compileClasspath.get())
    archiveClassifier.set(null as String?)
    dependencies {
        include(dependency("org.gradle:"))
    }
}

tasks.named("jar") {
    enabled = false
    dependsOn(tasks.named("shadowJar"))
}
