import com.github.jk1.license.LicenseReportExtension
import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension
import nl.javadude.gradle.plugins.license.LicenseExtension
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing

    id("nebula.release") version "15.3.1"
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"

    id("com.github.johnrengelman.shadow") version "6.1.0" apply false
    id("com.github.hierynomus.license") version "0.16.1" apply false
    id("org.jetbrains.kotlin.jvm") version "1.5.10" apply false
    id("org.gradle.test-retry") version "1.2.1" apply false
    id("com.github.jk1.dependency-license-report") version "1.16" apply false

    id("nebula.maven-publish") version "17.3.2" apply false
    id("nebula.contacts") version "5.1.0" apply false
    id("nebula.info") version "9.3.0" apply false

    id("nebula.javadoc-jar") version "17.3.2" apply false
    id("nebula.source-jar") version "17.3.2" apply false
    id("nebula.maven-apache-license") version "17.3.2" apply false

    id("org.openrewrite.rewrite") version "5.0.0" apply false
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
    group = "org.openrewrite"
    description = "Eliminate tech-debt. Automatically."
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.jk1.dependency-license-report")

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    val compiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    val maybeExe = if(getCurrentOperatingSystem().isWindows) {
        ".exe"
    } else {
        ""
    }
    val javac = compiler.get().metadata.installationPath.file("bin/javac${maybeExe}")

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

        "testRuntimeOnly"("ch.qos.logback:logback-classic:1.0.13")
    }

    // This eagerly realizes KotlinCompile tasks, which is undesirable from the perspective of minimizing
    // time spent during Gradle's configuration phase.
    // But if we don't proactively make sure the destination dir exists, sometimes JavaCompile can fail with:
    // '..rewrite-core\build\classes\java\main' specified for property 'compileKotlinOutputClasses' does not exist.
    tasks.withType<KotlinCompile>() {
        kotlinOptions {
            jdkHome = compiler.get().metadata.installationPath.asFile.absolutePath
            jvmTarget = "1.8"
        }
        destinationDir.mkdirs()
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

    val testReportDir = File(buildDir, "reports/recipe-examples")
    tasks.named<Test>("test").configure {
        useJUnitPlatform {
            excludeTags("debug")
        }
        jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames")
        // Redundant to produce examples for both rewrite-java-11 and rewrite-java-8
        if(project.name != "rewrite-java-8") {
            jvmArgs("-Dorg.openrewrite.TestExampleOutputDir=$testReportDir")
        }
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        })
        outputs.dir(testReportDir)
        testLogging {
            showExceptions = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showCauses = true
            showStackTraces = true
        }
    }
    // Tests produce examples which can then be used to generate documentation
    val releasing = project.hasProperty("releasing")
    tasks.register<Jar>("jarWithExamples") {
        archiveClassifier.set("examples")
        // Only produce these when releasing to avoid slowing down local development
        // Without this every "publishToMavenLocal" forces tests to run, which would be tedious in most local dev scenarios
        inputs.property("releasing", releasing)
        if(releasing) {
            dependsOn(tasks.named("test"))
        }
        enabled = releasing
        from(testReportDir) {
            into("META-INF/rewrite")
        }
    }

    configurations.all {
        resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
    }

    tasks.named<JavaCompile>("compileJava").configure {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()

        options.isFork = true
        options.forkOptions.executable = javac.toString()
        options.compilerArgs.addAll(listOf("--release", "8"))
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    configure<LicenseReportExtension> {
        renderers = arrayOf(com.github.jk1.license.render.CsvReportRenderer())
    }

    if(!name.contains("benchmark")) {
        configure<ContactsExtension> {
            val j = Contact("jkschneider@gmail.com")
            j.moniker("Jonathan Schneider")

            people["jkschneider@gmail.com"] = j
        }

        configure<PublishingExtension> {
            publications {
                named("nebula", MavenPublication::class.java) {
                    val jarWithExamples = tasks.findByName("jarWithExamples")
                    if(jarWithExamples != null && releasing) {
                        artifact(jarWithExamples)
                    }

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
