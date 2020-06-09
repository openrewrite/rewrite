dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    implementation("org.slf4j:slf4j-api:1.7.+")

    testImplementation(project(":rewrite-test"))
}

tasks.withType<Javadoc> {
    exclude("**/JavaParser**")
}
