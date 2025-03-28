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


dependencies {
    api(project(":rewrite-java"))

    implementation("org.codehaus.groovy:groovy:latest.release")

    compileOnly(project(":rewrite-test"))
    compileOnly("org.slf4j:slf4j-api:1.7.+")

    api("io.micrometer:micrometer-core:1.9.+")

    api("org.jetbrains:annotations:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-java-test"))
    testImplementation("org.junit-pioneer:junit-pioneer:latest.release")
    testRuntimeOnly("org.antlr:antlr4-runtime:4.13.2")
    testRuntimeOnly("org.codehaus.groovy:groovy-all:latest.release")
    testRuntimeOnly(project(":rewrite-java-17"))
}

tasks.named("check") {
    dependsOn(testing.suites.named("groovy2Test"))
}
