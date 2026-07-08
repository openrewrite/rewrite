import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    id("org.openrewrite.build.language-library")
    // The convention wraps com.gradleup.shadow (see phase1-results-spine.md "Shading decisions").
    id("org.openrewrite.build.shadow")
}

// The shaded jar is Java-8 bytecode (enforced by Java8BytecodeFloorTest), but the consumable shadow variant otherwise
// inherits the build toolchain's JVM version (21), which fails attribute matching when a Java-8 module (rewrite-maven)
// consumes the relocated stack. Advertise the variant's true level so those modules resolve it, exactly as apiElements
// already advertises 8.
configurations.named("shadowRuntimeElements").configure {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
    }
}

description = "Shaded Maven Resolver 2.x / Maven 3.9 model-building engine behind an OpenRewrite HttpSender transport."

// Everything in this configuration is relocated into the jar under
// org.openrewrite.maven.engine.shaded.* and never surfaces on the published POM.
// Mirrors the rewrite-java checkstyle-shading pattern.
val mavenStack: Configuration by configurations.creating
configurations.named("compileOnly").configure { extendsFrom(mavenStack) }
configurations.named("testImplementation").configure { extendsFrom(mavenStack) }

dependencies {
    // The engine's only non-shaded dependency (carries HttpSender + HttpUrlConnectionSender).
    api(project(":rewrite-core"))

    // Match rewrite-maven's retry semantics exactly (dev.failsafe is already a rewrite-maven dep).
    implementation("dev.failsafe:failsafe:latest.release")

    // The bundled resolver classes log through org.slf4j, which must bind to the HOST's slf4j — so it is
    // excluded from the shade and declared as a normal POM dependency instead. 2.0.x is what the resolver
    // stack targets (latest.release would pull a 2.1 alpha).
    implementation("org.slf4j:slf4j-api:2.0.+")

    // The entire no-DI resolver 2.x stack + Maven 3.9 model-builder/provider, bootstrapped by plain `new`.
    // Excluded from the shade: transport-apache + httpclient/httpcore/commons-codec (dead weight once
    // HttpSender is the sole transport), slf4j-api (host-bound, declared above), and error_prone_annotations
    // (annotation-only CLASS-retention metadata gson drags in, not needed at runtime).
    mavenStack("org.apache.maven.resolver:maven-resolver-supplier-mvn3:2.0.20") {
        exclude(group = "org.apache.maven.resolver", module = "maven-resolver-transport-apache")
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.apache.httpcomponents", module = "httpcore")
        exclude(group = "commons-codec", module = "commons-codec")
        exclude(group = "org.slf4j", module = "slf4j-api")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }

    testImplementation(project(":rewrite-test"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.+")
    testImplementation("com.squareup.okhttp3:okhttp:4.+")
    testRuntimeOnly("org.slf4j:slf4j-nop:latest.release")
}

// The engine and everything it bundles honor the Java 8 bytecode floor.
tasks.named<JavaCompile>("compileJava").configure {
    options.release.set(8)
}

val relocatePrefix = "org.openrewrite.maven.engine.shaded"

tasks.named<ShadowJar>("shadowJar").configure {
    // Override the convention default (compileClasspath): bundle only the Maven stack, never rewrite-core.
    configurations = listOf(mavenStack)

    // DESIGN §2 relocation set. The jar-scan test (RelocationJarScanTest) asserts an empty allowlist —
    // every class in the jar must live under org/openrewrite/** — so ANY bundled package must be relocated
    // here or excluded above. The whole org.apache.maven tree is relocated (a documented superset of DESIGN's
    // enumerated sub-packages): the actual 3.9.16 resolution also drags
    // org.apache.maven.{repository, repository.legacy, utils} out of maven-artifact/maven-model-builder.
    // gson (via resolver-spi) and asm (via maven-model-builder) relocate too — Gradle hosts ship their own asm
    // and gson is ubiquitous on plugin classpaths.
    relocate("org.eclipse.aether", "$relocatePrefix.org.eclipse.aether")
    relocate("org.apache.maven", "$relocatePrefix.org.apache.maven")
    relocate("org.codehaus.plexus.util", "$relocatePrefix.org.codehaus.plexus.util")
    relocate("org.codehaus.plexus.interpolation", "$relocatePrefix.org.codehaus.plexus.interpolation")
    relocate("com.google.gson", "$relocatePrefix.com.google.gson")
    relocate("org.objectweb.asm", "$relocatePrefix.org.objectweb.asm")

    // No DI container: sisu component indexes are never read (plain-`new` bootstrap). Strip them,
    // plus signature files and per-jar pom metadata that would misidentify the shaded artifact.
    // module-info descriptors (root or multi-release) name unrelocated modules and are inert here.
    exclude("META-INF/sisu/**")
    exclude("META-INF/maven/**")
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    exclude("**/module-info.class")

    // Aggregate service files (contents rewritten to the relocated names) and NOTICE/license text.
    mergeServiceFiles()
    append("META-INF/NOTICE")
    append("META-INF/NOTICE.txt")
    append("META-INF/LICENSE")
    append("META-INF/LICENSE.txt")
}

tasks.withType<Test>().configureEach {
    // RelocationJarScanTest and Java8BytecodeFloorTest scan the real published artifact.
    val shadowJarFile = tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile }
    // Track the jar's content so the scan tests re-run when the jar changes (a bare sysprop string would not).
    inputs.file(shadowJarFile).withPropertyName("engineShadowJar").withPathSensitivity(PathSensitivity.NONE)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dengine.shadowJar=${shadowJarFile.get().asFile.absolutePath}")
    })
}
