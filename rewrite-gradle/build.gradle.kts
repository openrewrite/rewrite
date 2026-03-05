import nl.javadude.gradle.plugins.license.LicenseExtension

plugins {
    id("org.openrewrite.build.language-library")
    id("groovy")
}

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

recipeDependencies {
    parserClasspath("org.gradle:gradle-base-services:latest.release")
    parserClasspath("org.gradle:gradle-core-api:latest.release")
    parserClasspath("org.gradle:gradle-language-groovy:latest.release")
    parserClasspath("org.gradle:gradle-language-java:latest.release")
    parserClasspath("org.gradle:gradle-logging:latest.release")
    parserClasspath("org.gradle:gradle-messaging:latest.release")
    parserClasspath("org.gradle:gradle-native:latest.release")
    parserClasspath("org.gradle:gradle-process-services:latest.release")
    parserClasspath("org.gradle:gradle-resources:latest.release")
    parserClasspath("org.gradle:gradle-testing-base:latest.release")
    parserClasspath("org.gradle:gradle-testing-jvm:latest.release")
    // No particular reason to hold back upgrading this beyond 3.x, but it takes some effort: https://github.com/openrewrite/rewrite/issues/5270
    parserClasspath("com.gradle:develocity-gradle-plugin:3.+")
}

//val rewriteVersion = rewriteRecipe.rewriteVersion.get()
val latest = if (project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}
val pluginLocalTestClasspath = configurations.create("pluginLocalTestClasspath")
dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-groovy")) {
        exclude("org.codehaus.groovy", "groovy")
    }
    api(project(":rewrite-kotlin"))
    api(project(":rewrite-maven"))
    api("org.jetbrains:annotations:latest.release")
    compileOnly(project(":rewrite-test"))
    implementation(project(":rewrite-properties"))
    implementation(project(":rewrite-toml"))

    compileOnly("org.apache.groovy:groovy:4.+")
    compileOnly(gradleApi())
    // No particular reason to hold back upgrading this beyond 3.x, but it takes some effort: https://github.com/openrewrite/rewrite/issues/5270
    compileOnly("com.gradle:develocity-gradle-plugin:3.+")

    testImplementation(project(":rewrite-test")) {
        // because gradle-api fatjars this implementation already
        exclude("ch.qos.logback", "logback-classic")
        exclude("org.slf4j", "slf4j-nop")
    }
    testImplementation(project(":rewrite-toml"))
    testImplementation(project(":rewrite-gradle-tooling-model:model"))
    "pluginLocalTestClasspath"(project(mapOf("path" to ":rewrite-gradle-tooling-model:model", "configuration" to "pluginLocalTestClasspath")))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.+")
    testImplementation(localGroovy())
    testImplementation(gradleApi())

    testRuntimeOnly("org.gradle:gradle-base-services:latest.release")
    testRuntimeOnly("com.google.guava:guava:latest.release")
    testRuntimeOnly(project(":rewrite-java-21"))
    testRuntimeOnly("org.projectlombok:lombok:latest.release")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<Test>().configureEach {
    dependsOn(pluginLocalTestClasspath)
    systemProperty("org.openrewrite.gradle.local.use-embedded-classpath", pluginLocalTestClasspath.files.find { it.name == "test-manifest.txt" }!!.path)
    maxHeapSize = "2g"
}

// This seems to be the only way to get the groovy compiler to emit java-8 compatible bytecode
// No option to explicitly target java-8 in the groovy compiler
tasks.withType<GroovyCompile>().configureEach {
    this.javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}

//Javadoc compiler will complain about the use of the internal types.
tasks.withType<Javadoc>().configureEach {
    exclude(
        "**/GradleProject**",
        "**/GradleDependencyConfiguration**",
        "**/GradleSettings**"
    )
}

configure<LicenseExtension> {
    excludePatterns.add("**/gradle-wrapper/*")
}

tasks.register<JavaExec>("syncWrapperScripts") {
    classpath = sourceSets.test.get().runtimeClasspath + sourceSets.test.get().output
    mainClass = "org.openrewrite.gradle.internal.GradleWrapperScriptDownloader"
    workingDir = project.rootDir
}
