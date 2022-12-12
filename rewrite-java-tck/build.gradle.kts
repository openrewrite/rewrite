plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    implementation(project(":rewrite-java"))
    implementation(project(":rewrite-test"))

    implementation("org.assertj:assertj-core:latest.release")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    runtimeOnly("junit:junit:4.13.2") {
        because("Used for RemoveUnneededAssertionTest")
    }
}

tasks.withType<Javadoc> {
    isFailOnError = false
    exclude("org/openrewrite/java/**")
}
