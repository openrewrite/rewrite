import com.github.jk1.license.LicenseReportExtension
import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension
import nl.javadude.gradle.plugins.license.LicenseExtension
import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing

    id("nebula.release") version "15.3.1"
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"

    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("com.github.hierynomus.license") version "0.16.1" apply false
    id("org.jetbrains.kotlin.jvm") version "1.5.21" apply false
    id("org.gradle.test-retry") version "1.2.1" apply false
    id("com.github.jk1.dependency-license-report") version "2.0" apply false
    id("org.owasp.dependencycheck") version "6.5.3" apply false

    id("nebula.maven-publish") version "17.3.2" apply false
    id("nebula.contacts") version "5.1.0" apply false
    id("nebula.info") version "11.1.0" apply false

    id("nebula.javadoc-jar") version "17.3.2" apply false
    id("nebula.source-jar") version "17.3.2" apply false
    id("nebula.maven-apache-license") version "17.3.2" apply false

    id("org.openrewrite.rewrite") version "latest.release"
}

repositories {
    mavenCentral()
}

configure<org.openrewrite.gradle.RewriteExtension> {
    activeRecipes = listOf("org.openrewrite.java.format.AutoFormat")
}

configure<nebula.plugin.release.git.base.ReleasePluginExtension> {
    defaultVersionStrategy = nebula.plugin.release.NetflixOssStrategies.SNAPSHOT(project)
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

allprojects {
    apply(plugin = "license")
    apply(plugin = "org.owasp.dependencycheck")
    group = "org.openrewrite"
    description = "Eliminate tech-debt. Automatically."
    configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
        skipConfigurations = listOf("integTestImplementationDependenciesMetadata")
        analyzers.assemblyEnabled = false
        failBuildOnCVSS = 9.0F
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.jk1.dependency-license-report")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    }

    if(!name.contains("benchmark")) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        apply(plugin = "nebula.maven-resolved-dependencies")
        apply(plugin = "org.gradle.test-retry")

        apply(plugin = "nebula.maven-publish")
        apply(plugin = "nebula.contacts")
        apply(plugin = "nebula.info")

        apply(plugin = "nebula.javadoc-jar")
        apply(plugin = "nebula.source-jar")
        apply(plugin = "nebula.maven-apache-license")

        signing {
            setRequired({
                !project.version.toString().endsWith("SNAPSHOT") || project.hasProperty("forceSigning")
            })
            val signingKey: String? by project
            val signingPassword: String? by project
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["nebula"])
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }

    dependencies {
        "implementation"(platform("org.jetbrains.kotlin:kotlin-bom"))

        "compileOnly"("com.google.code.findbugs:jsr305:latest.release")

        "compileOnly"("org.projectlombok:lombok:latest.release")
        "annotationProcessor"("org.projectlombok:lombok:latest.release")

        "testImplementation"("org.junit.jupiter:junit-jupiter-api:latest.release")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:latest.release")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:latest.release")

        "testImplementation"("org.jetbrains.kotlin:kotlin-reflect")
        "testImplementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        "testImplementation"("org.jetbrains.kotlin:kotlin-stdlib-common")

        "testImplementation"("org.assertj:assertj-core:latest.release")

        "testRuntimeOnly"("ch.qos.logback:logback-classic:1.2.10")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
        executable = javaToolchains.javadocToolFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        }.get().executablePath.toString()
    }

    configure<LicenseExtension> {
        ext.set("year", Calendar.getInstance().get(Calendar.YEAR))
        skipExistingHeaders = true
        excludePatterns.addAll(listOf("**/*.tokens", "**/*.config", "**/*.interp", "**/*.txt"))
        header = project.rootProject.file("gradle/licenseHeader.txt")
        mapping(mapOf("kt" to "SLASHSTAR_STYLE", "java" to "SLASHSTAR_STYLE"))
        strictCheck = true
    }

    tasks.named<Test>("test").configure {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        useJUnitPlatform {
            excludeTags("debug")
        }
        jvmArgs = listOf(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+ShowHiddenFrames"
        )
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        })
        testLogging {
            showExceptions = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showCauses = true
            showStackTraces = true
        }
    }

    configurations.all {
        exclude("com.google.errorprone", "*")
        resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
    }

    tasks.named<JavaCompile>("compileJava").configure {
        options.isFork = true
        options.release.set(8)
    }

    configure<LicenseReportExtension> {
        renderers = arrayOf(com.github.jk1.license.render.CsvReportRenderer())
    }

    if(!name.contains("benchmark")) {
        configure<ContactsExtension> {
            val j = Contact("team@moderne.io")
            j.moniker("Moderne")

            people["team@moderne.io"] = j
        }

        configure<PublishingExtension> {
            publications {
                named("nebula", MavenPublication::class.java) {
                    suppressPomMetadataWarningsFor("runtimeElements")
                    suppressPomMetadataWarningsFor("checkstyleApiElements")
                    suppressPomMetadataWarningsFor("checkstyleRuntimeElements")
                    pom.withXml {
                        (asElement().getElementsByTagName("dependencies")
                            .item(0) as org.w3c.dom.Element).let { dependencies ->
                            dependencies.getElementsByTagName("dependency").let { dependencyList ->
                                var i = 0
                                var length = dependencyList.length
                                while (i < length) {
                                    (dependencyList.item(i) as org.w3c.dom.Element).let { dependency ->
                                        if ((dependency.getElementsByTagName("scope")
                                                .item(0) as org.w3c.dom.Element).textContent == "provided"
                                        ) {
                                            dependencies.removeChild(dependency)
                                            i--
                                            length--
                                        }
                                    }
                                    i++
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

defaultTasks("build")
