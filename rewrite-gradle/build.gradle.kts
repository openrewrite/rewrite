dependencies {
    api(project(":rewrite-groovy"))
    implementation("dev.gradleplugins:gradle-api:6.7")

    implementation("com.squareup.okhttp3:okhttp:latest.release")

    // FIXME: switch to `latest.release`
    // when https://github.com/resilience4j/resilience4j/issues/1472 is resolved
    implementation("io.github.resilience4j:resilience4j-retry:1.7.0")

    testImplementation(project(":rewrite-test")) {
        // because gradle-api fatjars this implementation already
        exclude("ch.qos.logback", "logback-classic")
    }
}
