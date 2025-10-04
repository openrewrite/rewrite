plugins {
    id("org.openrewrite.build.language-library")
}

// comment out to run TestClassPathIsolationTest#Classpath
recipeDependencies {
    // note both on the classpath to show bug in TestClassPathIsolationTest
    parserClasspath("javax.servlet:javax.servlet-api:4.+")
    parserClasspath("jakarta.servlet:jakarta.servlet-api:6.+")
}

dependencies {

//    uncomment to run TestClassPathIsolationTest#Classpath
//    testRuntimeOnly("javax.servlet:javax.servlet-api:4.+")
//    testRuntimeOnly("jakarta.servlet:jakarta.servlet-api:6.+")

    api(platform("org.junit:junit-bom:5.13.3"))
    api(project(":rewrite-core"))
    compileOnly("io.micrometer:micrometer-core:latest.release")
    api("org.junit.jupiter:junit-jupiter-api")
    api("org.junit.jupiter:junit-jupiter-params")
    api("org.junit.platform:junit-platform-launcher")

    implementation("org.assertj:assertj-core:3.+")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-nop:1.7.36")

    testImplementation(project(":rewrite-groovy"))
    testImplementation(project(":rewrite-java"))
    testRuntimeOnly(project(":rewrite-java-21"))
    testRuntimeOnly("org.antlr:antlr4-runtime:4.13.2")
    testRuntimeOnly(project(":rewrite-java-21"))
}
