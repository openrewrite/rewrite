plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api(platform("org.junit:junit-bom:5.14.0"))
    api(project(":rewrite-core"))
    compileOnly("io.micrometer:micrometer-core:latest.release")
    api("org.junit.jupiter:junit-jupiter-api")
    api("org.junit.jupiter:junit-jupiter-params")

    implementation("org.assertj:assertj-core:3.+")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-nop:1.7.36")

    testImplementation(project(":rewrite-groovy"))
    testRuntimeOnly("org.antlr:antlr4-runtime:4.13.2")
}
