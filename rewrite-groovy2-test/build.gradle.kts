plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    testImplementation(project(":rewrite-groovy"))
    testImplementation(project(":rewrite-test"))
    testImplementation("org.junit.jupiter:junit-jupiter:latest.release")
    testImplementation("org.codehaus.groovy:groovy:[2.5,2.6)")
    testRuntimeOnly("org.codehaus.groovy:groovy-all:[2.5,2.6)")
    testRuntimeOnly(project(":rewrite-java-8"))
}

tasks.named<JavaCompile>("compileTestJava") {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()

    options.release.set(null as Int?) // remove `--release 8` set in `org.openrewrite.java-base`
}
