plugins {
    id("org.openrewrite.build.language-library")
    id("groovy")
}

val parserClasspath = configurations.create("parserClasspath")

repositories {
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases/")
        content {
            excludeVersionByRegex(".+", ".+", ".+-rc-?[0-9]*")
        }
    }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
        content {
            excludeVersionByRegex(".+", ".+", ".+-rc-?[0-9]*")
        }
    }
}

//val rewriteVersion = rewriteRecipe.rewriteVersion.get()
val rewriteVersion = if(project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}
dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-groovy")) {
        exclude("org.codehaus.groovy", "groovy")
    }
    api(project(":rewrite-maven"))
    api("org.jetbrains:annotations:latest.release")
    compileOnly(project(":rewrite-test"))
    implementation(project(":rewrite-properties"))

    compileOnly("org.codehaus.groovy:groovy:latest.release")
    compileOnly(gradleApi())

    compileOnly("com.gradle:gradle-enterprise-gradle-plugin:latest.release")

    "parserClasspath"("org.gradle:gradle-base-services:latest.release")
    "parserClasspath"("org.gradle:gradle-core-api:latest.release")
    "parserClasspath"("org.gradle:gradle-language-groovy:latest.release")
    "parserClasspath"("org.gradle:gradle-language-java:latest.release")
    "parserClasspath"("org.gradle:gradle-logging:latest.release")
    "parserClasspath"("org.gradle:gradle-messaging:latest.release")
    "parserClasspath"("org.gradle:gradle-native:latest.release")
    "parserClasspath"("org.gradle:gradle-process-services:latest.release")
    "parserClasspath"("org.gradle:gradle-resources:latest.release")
    "parserClasspath"("org.gradle:gradle-testing-base:latest.release")
    "parserClasspath"("org.gradle:gradle-testing-jvm:latest.release")
    "parserClasspath"("com.gradle:gradle-enterprise-gradle-plugin:latest.release")

    testImplementation(project(":rewrite-test")) {
        // because gradle-api fatjars this implementation already
        exclude("ch.qos.logback", "logback-classic")
    }

    testImplementation("org.openrewrite.gradle.tooling:model:latest.release")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.+")

    testRuntimeOnly("org.codehaus.groovy:groovy:latest.release")
    testRuntimeOnly("org.gradle:gradle-base-services:latest.release")
    testRuntimeOnly(gradleApi())
    testRuntimeOnly("com.gradle:gradle-enterprise-gradle-plugin:latest.release")
    testRuntimeOnly("com.google.guava:guava:latest.release")
    testRuntimeOnly(project(":rewrite-java-17"))
    testRuntimeOnly("org.projectlombok:lombok:latest.release")
}

// This seems to be the only way to get the groovy compiler to emit java-8 compatible bytecode
// No option to explicitly target java-8 in the groovy compiler
tasks.withType<GroovyCompile> {
    this.javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}

tasks.named<Copy>("processResources") {
    from(parserClasspath) {
        into("META-INF/rewrite/classpath")
    }
}
