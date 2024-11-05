@file:Suppress("UnstableApiUsage")

import org.gradle.internal.impldep.org.junit.platform.launcher.TagFilter.excludeTags


plugins {
    id("org.openrewrite.build.language-library")
    id("jvm-test-suite")
}

val javaTck = configurations.create("javaTck") {
    isTransitive = false
}
dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))
    runtimeOnly(project(":rewrite-java-lombok"))

    compileOnly("org.slf4j:slf4j-api:1.7.+")

    implementation("io.micrometer:micrometer-core:1.9.+")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.ow2.asm:asm:latest.release")

    testImplementation(project(":rewrite-test"))
    "javaTck"(project(":rewrite-java-tck"))
}

tasks.withType<JavaCompile> {
    // allows --add-exports to in spite of the JDK's restrictions on this
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()

    options.release.set(null as Int?) // remove `--release 8` set in `org.openrewrite.java-base`
    options.compilerArgs.addAll(
        listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
        )
    )
}

//Javadoc compiler will complain about the use of the internal types.
tasks.withType<Javadoc> {
    exclude(
        "**/ReloadableJava17JavadocVisitor**",
        "**/ReloadableJava17Parser**",
        "**/ReloadableJava17ParserVisitor**",
        "**/ReloadableJava17TypeMapping**",
        "**/ReloadableJava17TypeSignatureBuilder**"
    )
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)

        register("compatibilityTest", JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation(project(":rewrite-test"))
                implementation(project(":rewrite-java-tck"))
                implementation(project(":rewrite-java-test"))
                implementation("org.assertj:assertj-core:latest.release")
            }

            targets {
                all {
                    testTask.configure {
                        useJUnitPlatform {
                            excludeTags("java21")
                        }
                        testClassesDirs += files(javaTck.files.map { zipTree(it) })
                        jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames")
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("compatibilityTest"))
}
