plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Two engines that must NOT share a classpath (rewrite-maven vs maven-resolver-supplier-mvn3):
// isolated into their own source sets + configurations, each launched in its own JVM (JavaExec).
// A pure-JDK `common` source set holds shared harness code (corpus discovery, stats).
val common = sourceSets.create("common")
val old = sourceSets.create("old")
val neu = sourceSets.create("new")

listOf(old, neu).forEach { ss ->
    ss.compileClasspath += common.output
    ss.runtimeClasspath += common.output
}

dependencies {
    // OLD engine: current rewrite-maven resolution.
    "oldImplementation"("org.openrewrite:rewrite-maven:latest.release")
    "oldRuntimeOnly"("org.slf4j:slf4j-nop:2.0.16")

    // NEW engine: raw Maven pipeline (resolver 2.0.20 + maven 3.9.16 model-builder), HttpSender from rewrite-core.
    "newImplementation"("org.apache.maven.resolver:maven-resolver-supplier-mvn3:2.0.20")
    "newImplementation"("org.openrewrite:rewrite-core:latest.release")
    "newRuntimeOnly"("org.slf4j:slf4j-nop:2.0.16")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

// Corpus is parameterizable so the same harness runs the default apache/maven reactor and large reactors
// (apache/camel) at graduated module caps. Defaults preserve the original apache/maven behavior exactly.
//   -Pcorpus=<abs path to a reactor root>   (default: build/corpus, the apache/maven checkout)
//   -PmaxModules=N                          (default: 0 = full reactor; see Corpus.selectSubtree for the rule)
//   -PleafModule=<dir name>                 (default: maven-core; the re-resolution hot-loop module)
//   -PskipCold=true                         (skip the single-shot live-network cold phase; warm-up still primes caches)
//   -PlocalRepo=<abs dir>                   (persistent local repo reused across runs; else a fresh temp repo per run)
//   -Pheap=<size>                           (default: 2g; identical for both engines)
val corpusDir = (findProperty("corpus") as String?)?.let { file(it) } ?: layout.projectDirectory.dir("build/corpus").asFile
val outDir = layout.buildDirectory.get().asFile
val maxModules = (findProperty("maxModules") as String?)?.toIntOrNull() ?: 0
val leafModule = (findProperty("leafModule") as String?) ?: "maven-core"
val skipCold = (findProperty("skipCold") as String?)?.toBoolean() ?: false
val localRepo = (findProperty("localRepo") as String?) ?: ""
val heap = (findProperty("heap") as String?) ?: "2g"

fun JavaExec.harness() {
    group = "benchmark"
    // Identical heap for both engines (fairness).
    jvmArgs("-Xms$heap", "-Xmx$heap")
    systemProperty("bench.corpus", corpusDir.absolutePath)
    systemProperty("bench.out", outDir.absolutePath)
    systemProperty("bench.maxModules", maxModules.toString())
    systemProperty("bench.leaf", leafModule)
    systemProperty("bench.skipCold", skipCold.toString())
    systemProperty("bench.localRepo", localRepo)
}

tasks.register<JavaExec>("runOld") {
    harness()
    classpath = old.runtimeClasspath
    mainClass.set("bench.OldEngineBenchmark")
}

tasks.register<JavaExec>("runNew") {
    harness()
    classpath = neu.runtimeClasspath
    mainClass.set("bench.NewEngineBenchmark")
}

tasks.register("printVersions") {
    doLast {
        configurations["oldRuntimeClasspath"].resolvedConfiguration.resolvedArtifacts
            .filter { it.moduleVersion.id.group.startsWith("org.openrewrite") }
            .forEach { println("OLD  " + it.moduleVersion.id) }
        configurations["newRuntimeClasspath"].resolvedConfiguration.resolvedArtifacts
            .filter { it.moduleVersion.id.group.startsWith("org.apache.maven") || it.moduleVersion.id.group.startsWith("org.openrewrite") }
            .sortedBy { it.moduleVersion.id.toString() }
            .forEach { println("NEW  " + it.moduleVersion.id) }
    }
}
