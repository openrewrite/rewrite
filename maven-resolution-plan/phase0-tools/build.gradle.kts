plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")

    implementation("org.openrewrite:rewrite-maven:latest.release")
    implementation(platform("com.fasterxml.jackson:jackson-bom:latest.release"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

fun entryArgs(): List<String> =
    (project.findProperty("entries") as String?)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

tasks.register<JavaExec>("corpusFetch") {
    group = "corpus"
    description = "Materialize corpus.yaml entries into .corpus/ (poms recorded through RecordingHttpSender, reactors shallow-cloned)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.openrewrite.maven.parity.corpus.CorpusFetch"
    workingDir = projectDir
    args(entryArgs())
}

tasks.register<JavaExec>("groundTruthCapture") {
    group = "corpus"
    description = "Capture real Maven 3.9.16 dependency:tree -Dverbose and help:effective-pom per entry into .corpus/ground-truth/"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.openrewrite.maven.parity.corpus.GroundTruthCapture"
    workingDir = projectDir
    args(entryArgs())
}

tasks.register<JavaExec>("corpusRun") {
    group = "corpus"
    description = "Resolve corpus entries with released rewrite-maven; -Pmode=record populates the store, -Pmode=replay (default) is hermetic + twice-run determinism check"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.openrewrite.maven.parity.corpus.CorpusResolutionRunner"
    workingDir = projectDir
    maxHeapSize = "6g"
    val mode = (project.findProperty("mode") as String?) ?: "replay"
    systemProperty("corpus.mode", mode)
    if (mode == "replay") {
        // Belt and suspenders on top of RecordingHttpSender's no-delegate REPLAY mode: any
        // HttpURLConnection that slips past the sender dies against a dead proxy.
        systemProperty("http.proxyHost", "127.0.0.1")
        systemProperty("http.proxyPort", "1")
        systemProperty("https.proxyHost", "127.0.0.1")
        systemProperty("https.proxyPort", "1")
        systemProperty("http.nonProxyHosts", "")
    }
    args(entryArgs())
}
