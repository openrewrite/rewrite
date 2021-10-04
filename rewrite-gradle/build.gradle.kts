dependencies {
    api(project(":rewrite-groovy"))
    implementation("dev.gradleplugins:gradle-api:7.2")
    implementation("org.gradle:gradle-tooling-api:7.2")

    testImplementation(project(":rewrite-test")) {
        // because gradle-api fatjars this implementation already
        exclude("ch.qos.logback", "logback-classic")
    }
}
