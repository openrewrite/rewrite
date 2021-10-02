dependencies {
    api(project(":rewrite-groovy"))
    implementation("dev.gradleplugins:gradle-api:7.2")
    implementation("org.gradle:gradle-tooling-api:7.2")

    testImplementation(project(":rewrite-test"))
}
