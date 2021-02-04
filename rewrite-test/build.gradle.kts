dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    compileOnly("io.micrometer:micrometer-registry-prometheus:latest.release")

    api("org.junit.jupiter:junit-jupiter-api:latest.release")
    api("org.junit.jupiter:junit-jupiter-params:latest.release")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.assertj:assertj-core:latest.release")

    implementation("org.slf4j:slf4j-api:1.7.+")
    implementation("ch.qos.logback:logback-classic:1.0.13")
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()

    options.isFork = true
    options.forkOptions.executable = "javac"
    options.compilerArgs.addAll(listOf("--release", "8"))
}
