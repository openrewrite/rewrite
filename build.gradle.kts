import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension

plugins {
    `java-library`

    id("nebula.release") version "16.0.0"

    id("nebula.maven-manifest") version "18.4.0"
    id("nebula.maven-nebula-publish") version "18.4.0"
    id("nebula.maven-resolved-dependencies") version "18.4.0"

    id("nebula.contacts") version "6.0.0"
    id("nebula.info") version "11.3.3"

    id("nebula.javadoc-jar") version "18.4.0"
    id("nebula.source-jar") version "18.4.0"
}

apply(plugin = "nebula.publish-verification")

configure<nebula.plugin.release.git.base.ReleasePluginExtension> {
    defaultVersionStrategy = nebula.plugin.release.NetflixOssStrategies.SNAPSHOT(project)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Set as appropriate for your organization
group = "org.openrewrite"
description = "Rewrite Kotlin"

repositories {
    mavenLocal()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases-local/")
    }
    mavenCentral()
    gradlePluginPortal()
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, TimeUnit.SECONDS)
        cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
    }
}

val latest = if (project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}

val kotlinVersion = "1.8.0"

dependencies {
    annotationProcessor("org.projectlombok:lombok:latest.release")

    compileOnly("org.openrewrite:rewrite-test")
    compileOnly("org.projectlombok:lombok:latest.release")
    compileOnly("com.google.code.findbugs:jsr305:latest.release")

    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:${latest}"))
    implementation("org.openrewrite:rewrite-java")

    implementation(platform(kotlin("bom", kotlinVersion)))
    implementation(kotlin("compiler-embeddable"))
//    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:${kotlinVersion}")

    implementation(kotlin("stdlib"))

    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testImplementation("org.openrewrite:rewrite-test")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

//    testImplementation("org.gradle:gradle-kotlin-dsl:latest.release")
//    testImplementation("org.gradle:gradle-language-java:latest.release")
//    testImplementation("org.gradle:gradle-core-api:latest.release")
//    testImplementation("org.gradle:gradle-plugins:latest.release")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames")
    minHeapSize = "512m"
    maxHeapSize = "1024m"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.named<JavaCompile>("compileJava") {
    options.release.set(8)
}

configure<ContactsExtension> {
    val j = Contact("team@moderne.io")
    j.moniker("Team Moderne")
    people["team@moderne.io"] = j
}

configure<PublishingExtension> {
    publications {
        named("nebula", MavenPublication::class.java) {
            suppressPomMetadataWarningsFor("runtimeElements")
        }
    }
}

publishing {
  repositories {
      maven {
          name = "moderne"
          url = uri("https://us-west1-maven.pkg.dev/moderne-dev/moderne-recipe")
      }
  }
}
