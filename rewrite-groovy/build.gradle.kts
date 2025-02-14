plugins {
    id("org.openrewrite.build.language-library")
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

//val testJava8 = tasks.register<Test>("testJava8") {
//    javaLauncher.set(javaToolchains.launcherFor {
//        languageVersion.set(JavaLanguageVersion.of(8))
//    })
//}
//tasks.named("check").configure {
//    // Enable once the java8 tests are passing
//    // dependsOn(testJava8)
//}
