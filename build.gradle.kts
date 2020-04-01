import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    id("nebula.release") version "13.2.1"
    id("nebula.maven-publish") version "17.2.1" apply false
    id("nebula.maven-resolved-dependencies") version "17.2.1" apply false
    id("org.jetbrains.kotlin.jvm") version "1.3.71" apply false
}

project.gradle.taskGraph.whenReady(object : Action<TaskExecutionGraph> {
    override fun execute(graph: TaskExecutionGraph) {
        if (graph.hasTask(":snapshot") || graph.hasTask(":immutableSnapshot")) {
            throw GradleException("You cannot use the snapshot or immutableSnapshot task from the release plugin. Please use the devSnapshot task.")
        }
    }
})

fun shouldUseReleaseRepo(): Boolean {
    return project.gradle.startParameter.taskNames.contains("final") || project.gradle.startParameter.taskNames.contains(":final")
}

allprojects {
    group = "org.gradle.rewrite"
    description = "Pluggable and distributed refactoring tool"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "nebula.maven-publish")
    apply(plugin = "nebula.maven-resolved-dependencies")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral {
            content {
                excludeVersionByRegex("com\\.fasterxml\\.jackson\\..*", ".*", ".*rc.*")
            }
        }
        mavenCentral {
            content {
                includeVersionByRegex("com\\.fasterxml\\.jackson\\..*", ".*", "(\\d+\\.)*\\d+")
            }
        }
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok:latest.release")
        "annotationProcessor"("org.projectlombok:lombok:latest.release")

        "testImplementation"("org.junit.jupiter:junit-jupiter-api:latest.release")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:latest.release")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:latest.release")

        "testImplementation"("org.jetbrains.kotlin:kotlin-reflect")
        "testImplementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

        "testImplementation"("org.assertj:assertj-core:latest.release")

        "testRuntimeOnly"("ch.qos.logback:logback-classic:1.0.13")
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
        jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames")
    }

    tasks.withType(KotlinCompile::class.java).configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    configurations.all {
        resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        withJavadocJar()
        withSourcesJar()
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "GradleEnterprise"
                url = if (shouldUseReleaseRepo()) {
                    URI.create("https://repo.gradle.org/gradle/enterprise-libs-releases-local")
                } else {
                    URI.create("https://repo.gradle.org/gradle/enterprise-libs-snapshots-local")
                }
                credentials {
                    username = project.findProperty("artifactoryUsername") as String?
                    password = project.findProperty("artifactoryPassword") as String?
                }
            }
        }
    }

    project.rootProject.tasks.getByName("postRelease").dependsOn(project.tasks.getByName("publishNebulaPublicationToGradleEnterpriseRepository"))
}
