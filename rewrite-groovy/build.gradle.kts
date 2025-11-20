@file:Suppress("UnstableApiUsage")

plugins {
    id("org.openrewrite.build.language-library")
    id("jvm-test-suite")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("groovy2Test") {
            configurations.getByName("groovy2TestRuntimeClasspath") {
                resolutionStrategy {
                    force("org.codehaus.groovy:groovy:2.5.22")
                }
            }

            dependencies {
                implementation(project())
                implementation(project(":rewrite-test"))
                implementation("org.assertj:assertj-core:latest.release")
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    api(project(":rewrite-java"))

    implementation("org.apache.groovy:groovy:4.+")

    compileOnly(project(":rewrite-test"))
    compileOnly("org.slf4j:slf4j-api:1.7.+")

    api("io.micrometer:micrometer-core:1.9.+")

    api("org.jetbrains:annotations:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-java-test"))
    testImplementation("org.junit-pioneer:junit-pioneer:latest.release")
    testRuntimeOnly("org.antlr:antlr4-runtime:4.13.2")
    testRuntimeOnly("org.apache.groovy:groovy-all:4.+")
    testRuntimeOnly(project(":rewrite-java-21"))
}

tasks.named("check") {
    dependsOn(testing.suites.named("groovy2Test"))
}
