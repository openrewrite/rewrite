dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    api("org.junit.jupiter:junit-jupiter-api:latest.release")
    api("org.junit.jupiter:junit-jupiter-params:latest.release")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.assertj:assertj-core:latest.release")

    testImplementation("me.lucko:jar-relocator:1.4")
}
