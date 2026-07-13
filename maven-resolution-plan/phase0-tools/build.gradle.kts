plugins {
    java
}

repositories {
    // Maven Local first so the worktree's freshly-published rewrite-maven SNAPSHOT wins over any
    // released artifact; mavenCentral supplies the third-party deps (jackson, junit, lombok).
    mavenLocal()
    mavenCentral()
}

// The worktree's rewrite-maven version. Publish it (and its local transitives) with:
//   ./gradlew :rewrite-core:publishToMavenLocal :rewrite-xml:publishToMavenLocal \
//             :rewrite-java:publishToMavenLocal :rewrite-properties:publishToMavenLocal \
//             :rewrite-yaml:publishToMavenLocal :rewrite-maven-engine:publishToMavenLocal \
//             :rewrite-maven:publishToMavenLocal
// Override with -PrewriteVersion=... if the worktree's version has moved.
val rewriteVersion = (findProperty("rewriteVersion") as String?) ?: "8.87.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")

    implementation("org.openrewrite:rewrite-maven:$rewriteVersion")
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

tasks.register<JavaExec>("integratedBenchmark") {
    group = "corpus"
    description = "Integrated perf gate: MavenParser end-to-end, one JVM, engine=legacy vs engine=maven, per tier"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.openrewrite.maven.parity.corpus.IntegratedBenchmark"
    workingDir = projectDir
    val heap = (project.findProperty("bench.heap") as String?) ?: "2g"
    maxHeapSize = heap
    jvmArgs("-Xms$heap")
    (project.findProperty("bench.warmups") as String?)?.let { systemProperty("bench.warmups", it) }
    (project.findProperty("bench.iters") as String?)?.let { systemProperty("bench.iters", it) }
    (project.findProperty("bench.loop") as String?)?.let { systemProperty("bench.loop", it) }
    (project.findProperty("engine.profile") as String?)?.let { systemProperty("engine.profile", it) }
    (project.findProperty("bench.fdwatch") as String?)?.let { systemProperty("bench.fdwatch", it) }
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
    // Dual-engine selector (dev/CI-only): legacy (default) | maven | shadow. Passed as the real
    // ResolutionEngineSelector system property so the runner can thread it onto the ExecutionContext.
    (project.findProperty("engine") as String?)?.let {
        systemProperty("org.openrewrite.maven.resolution.engine", it)
    }
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
