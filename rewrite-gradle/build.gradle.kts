import nl.javadude.gradle.plugins.license.LicenseExtension
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.TypePath
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.openrewrite.java.internal.parser.TypeTable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipInputStream

plugins {
    id("org.openrewrite.build.language-library")
    id("groovy")
}

repositories {
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases/")
        content {
            excludeVersionByRegex(".+", ".+", ".+-rc-?[0-9]*")
        }
    }
    google {
        content {
            includeGroup("com.android.tools.build")
        }
    }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
        content {
            excludeVersionByRegex(".+", ".+", ".+-rc-?[0-9]*")
        }
    }
    ivy {
        url = uri("https://services.gradle.org/distributions")
        patternLayout {
            artifact("[module]-[revision]-bin.zip")
        }
        metadataSources {
            artifact()
        }
        content {
            includeGroup("gradle-distribution")
            excludeVersionByRegex("gradle-distribution", "gradle", ".+-(rc|milestone)-[0-9]+")
        }
    }
}

recipeDependencies {
    parserClasspath("org.gradle:gradle-base-services:latest.release")
    parserClasspath("org.gradle:gradle-core-api:latest.release")
    parserClasspath("org.gradle:gradle-language-groovy:latest.release")
    parserClasspath("org.gradle:gradle-language-java:latest.release")
    parserClasspath("org.gradle:gradle-logging:latest.release")
    parserClasspath("org.gradle:gradle-messaging:latest.release")
    parserClasspath("org.gradle:gradle-native:latest.release")
    parserClasspath("org.gradle:gradle-process-services:latest.release")
    parserClasspath("org.gradle:gradle-resources:latest.release")
    parserClasspath("org.gradle:gradle-testing-base:latest.release")
    parserClasspath("org.gradle:gradle-testing-jvm:latest.release")
    // 3.19.2 is the last release with both the `develocity` and the legacy `gradleEnterprise` extension types;
    // 4.x removed the latter, which settings scripts still awaiting rewrite-migrate-gradle's
    // `migrate.plugins.MigrateGradleEnterpriseToDevelocity` rely on
    parserClasspath("com.gradle:develocity-gradle-plugin:3.19.2")

    testParserClasspath("com.android.tools.build:gradle-api:9.4.1")
}

// The newest release of each major Gradle version. Gradle removes API only in a major release, so each
// one covers the scripts written for any release of its major. GRADLE_API_MAJORS in GradleParser selects from these.
val gradleApiMajors = listOf(6, 7, 8, 9)
// One configuration per major, since a single configuration would resolve them all to the newest
val gradleDistributions = gradleApiMajors.map {
    configurations.detachedConfiguration(dependencies.create("gradle-distribution:gradle:$it.+@zip"))
}
// The Groovy runtime the Groovy parser runs on, as rewrite-groovy declares it. Gradle 6 ships Groovy as groovy-all, which
// also holds modules such as groovy-ant, so a distribution's own Groovy jars cannot tell which classes the parser has.
val parserGroovy = configurations.detachedConfiguration(dependencies.create("org.apache.groovy:groovy:4.+"))
    .apply { isTransitive = false }

// Gradle has published no API modules since 6.1.1, so the API of each major is taken from its distribution
val addGradleApiToTypeTable by tasks.registering {
    group = "OpenRewrite"
    description = "Adds the public API of each major Gradle version to the type table as org.gradle:gradle-public-api."
    gradleDistributions.forEach { inputs.files(it) }
    inputs.files(parserGroovy)
    val table = layout.projectDirectory.file("src/main/resources/META-INF/rewrite/classpath.tsv.gz").asFile
    doLast {
        // Rows are split on '\n' only, as bytes, since annotation values in them hold other control characters
        fun rows(gzipped: ByteArray) = GZIPInputStream(ByteArrayInputStream(gzipped)).readBytes()
            .toString(Charsets.ISO_8859_1).split('\n').filter { it.isNotEmpty() }
        val rows = rows(table.readBytes()).filter { !it.startsWith("org.gradle\tgradle-public-api\t") }.toMutableList()
        val groovyCore = ZipInputStream(parserGroovy.singleFile.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.map { it.name }.filter { it.endsWith(".class") }
                .map { it.removeSuffix(".class") }.toSet()
        }
        for (distribution in gradleDistributions) {
            val version = distribution.resolvedConfiguration.resolvedArtifacts.single().moduleVersion.id.version
            val api = temporaryDir.resolve("gradle-api-$version.jar")
            writePublicApi(distribution.singleFile, api, groovyCore)
            val written = ByteArrayOutputStream()
            TypeTable.newWriter(written).use { it.jar("org.gradle", "gradle-public-api", version).write(api.toPath()) }
            rows += rows(written.toByteArray()).drop(1)
        }
        GZIPOutputStream(table.outputStream()).use { out ->
            rows.forEach { out.write(it.toByteArray(Charsets.ISO_8859_1)); out.write('\n'.code) }
        }
    }
}
tasks.named("createTypeTable") {
    finalizedBy(addGradleApiToTypeTable)
}

/**
 * Writes the public classes of a Gradle distribution to a jar, with the classes their declarations extend, which type
 * attribution needs to find inherited members. A member or annotation that refers to a class outside the jar is left
 * out, since Groovy cannot resolve the members of a class when one of them refers to a missing class. The Kotlin DSL
 * is left out.
 */
fun writePublicApi(distribution: File, jar: File, groovyCore: Set<String>) {
    val classes = mutableMapOf<String, ByteArray>()
    ZipInputStream(distribution.inputStream()).use { zip ->
        generateSequence { zip.nextEntry }
            .filter { Regex("gradle-[^/]+/lib/(plugins/)?[^/]+\\.jar").matches(it.name) }
            .forEach {
                val nested = ZipInputStream(ByteArrayInputStream(zip.readBytes()))
                generateSequence { nested.nextEntry }
                    .filter { e -> e.name.endsWith(".class") && !e.name.startsWith("java/") }
                    .forEach { e ->
                        val name = e.name.removeSuffix(".class")
                        if (name !in groovyCore) classes.putIfAbsent(name, nested.readBytes())
                    }
            }
    }
    val pending = ArrayDeque(classes.keys.filter {
        it.startsWith("org/gradle/") && !it.contains("/internal/") && !it.startsWith("org/gradle/kotlin/")
    })
    val kept = sortedSetOf<String>()
    while (pending.isNotEmpty()) {
        val name = pending.removeFirst()
        val bytes = classes[name] ?: continue
        if (kept.add(name)) {
            val reader = ClassReader(bytes)
            reader.superName?.let(pending::add)
            pending.addAll(reader.interfaces)
            reader.accept(object : ClassVisitor(Opcodes.ASM9) {
                override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?,
                                   interfaces: Array<String>?) {
                    pending.addAll(referencedClasses(signature))
                }
            }, ClassReader.SKIP_CODE)
        }
    }
    val resolvable = { name: String -> name in kept || name in groovyCore || name.startsWith("java/") }
    JarOutputStream(jar.outputStream()).use { out ->
        kept.forEach {
            out.putNextEntry(JarEntry("$it.class"))
            out.write(withoutUnresolvableMembers(classes.getValue(it), resolvable))
            out.closeEntry()
        }
    }
}

fun referencedClasses(signature: String?, isType: Boolean = false): Set<String> {
    val names = mutableSetOf<String>()
    if (signature != null) {
        val visitor = object : SignatureVisitor(Opcodes.ASM9) {
            override fun visitClassType(name: String) {
                names += name
            }
        }
        if (isType) SignatureReader(signature).acceptType(visitor) else SignatureReader(signature).accept(visitor)
    }
    return names
}

fun withoutUnresolvableMembers(bytes: ByteArray, resolvable: (String) -> Boolean): ByteArray {
    val writer = ClassWriter(0)
    val annotationResolvable = { descriptor: String -> resolvable(Type.getType(descriptor).internalName) }
    ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM9, writer) {
        override fun visitAnnotation(descriptor: String, visible: Boolean) =
            if (annotationResolvable(descriptor)) super.visitAnnotation(descriptor, visible) else null

        override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean) = null

        override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
            if (resolvable(name)) super.visitInnerClass(name, outerName, innerName, access)
        }

        override fun visitNestMember(nestMember: String) {
            if (resolvable(nestMember)) super.visitNestMember(nestMember)
        }

        override fun visitPermittedSubclass(permittedSubclass: String) {
            if (resolvable(permittedSubclass)) super.visitPermittedSubclass(permittedSubclass)
        }

        override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
            if (access and Opcodes.ACC_SYNTHETIC != 0 ||
                !(referencedClasses(descriptor, true) + referencedClasses(signature, true)).all(resolvable)) {
                return null
            }
            return object : FieldVisitor(Opcodes.ASM9, super.visitField(access, name, descriptor, signature, value)) {
                override fun visitAnnotation(descriptor: String, visible: Boolean) =
                    if (annotationResolvable(descriptor)) super.visitAnnotation(descriptor, visible) else null

                override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean) = null
            }
        }

        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?,
                                 exceptions: Array<String>?): MethodVisitor? {
            if (access and Opcodes.ACC_SYNTHETIC != 0 ||
                !(referencedClasses(descriptor) + referencedClasses(signature) + exceptions.orEmpty()).all(resolvable)) {
                return null
            }
            return object : MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                override fun visitAnnotation(descriptor: String, visible: Boolean) =
                    if (annotationResolvable(descriptor)) super.visitAnnotation(descriptor, visible) else null

                override fun visitParameterAnnotation(parameter: Int, descriptor: String, visible: Boolean) =
                    if (annotationResolvable(descriptor)) super.visitParameterAnnotation(parameter, descriptor, visible) else null

                override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean) = null
            }
        }
    }, ClassReader.SKIP_CODE)
    return writer.toByteArray()
}

//val rewriteVersion = rewriteRecipe.rewriteVersion.get()
val latest = if (project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}
val pluginLocalTestClasspath = configurations.create("pluginLocalTestClasspath")
dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-groovy")) {
        exclude("org.codehaus.groovy", "groovy")
    }
    api(project(":rewrite-kotlin"))
    api(project(":rewrite-maven"))
    api("org.jetbrains:annotations:latest.release")
    api(project(":rewrite-toml"))

    implementation(project(":rewrite-properties"))

    compileOnly(project(":rewrite-test"))
    compileOnly("org.apache.groovy:groovy:4.+")
    compileOnly(gradleApi())
    compileOnly("com.gradle:develocity-gradle-plugin:3.19.2")

    // Align kotlin-stdlib-jdk7/-jdk8/-common with kotlin-stdlib
    // (mockwebserver 4.x would otherwise pin them to 1.9.10 transitively).
    testImplementation(platform("org.jetbrains.kotlin:kotlin-bom:2.4.20"))

    testImplementation(project(":rewrite-test")) {
        // because gradle-api fatjars this implementation already
        exclude("ch.qos.logback", "logback-classic")
        exclude("org.slf4j", "slf4j-nop")
    }
    testImplementation(project(":rewrite-gradle-tooling-model:model"))
    "pluginLocalTestClasspath"(project(mapOf("path" to ":rewrite-gradle-tooling-model:model", "configuration" to "pluginLocalTestClasspath")))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.+")
    testImplementation(localGroovy())
    testImplementation(gradleApi())

    testRuntimeOnly("org.gradle:gradle-base-services:latest.release")
    testRuntimeOnly("com.google.guava:guava:latest.release")
    testRuntimeOnly(project(":rewrite-java-21"))
    testRuntimeOnly("org.projectlombok:lombok:latest.release")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<Test>().configureEach {
    dependsOn(pluginLocalTestClasspath)
    val manifestFile = pluginLocalTestClasspath.files.find { it.name == "test-manifest.txt" }!!
    val userHomesDir = temporaryDir.resolve("gradle-user-homes")
    jvmArgumentProviders.add(object : CommandLineArgumentProvider {
        // Track the actual classpath JARs for cache key (content-aware, path-insensitive)
        // Filter out test-manifest.txt which contains absolute paths and is tracked separately as @Internal
        @get:Classpath
        val pluginClasspath: FileCollection = pluginLocalTestClasspath.filter { it.name != "test-manifest.txt" }

        // The manifest file contains absolute paths, so exclude it from cache key;
        // the classpath property above already tracks the actual content
        @get:Internal
        val manifest: File = manifestFile

        // Each test JVM builds in a Gradle user home of its own in here (Assertions.withToolingApi)
        @get:Internal
        val userHomes: File = userHomesDir

        override fun asArguments() = listOf(
            "-Dorg.openrewrite.gradle.local.use-embedded-classpath=${manifest.absolutePath}",
            "-Dorg.openrewrite.gradle.local.gradle-user-homes=${userHomes.absolutePath}"
        )
    })
    doFirst { userHomesDir.deleteRecursively() }
    maxHeapSize = "2g"
}

// This seems to be the only way to get the groovy compiler to emit java-8 compatible bytecode
// No option to explicitly target java-8 in the groovy compiler
tasks.withType<GroovyCompile>().configureEach {
    this.javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}

//Javadoc compiler will complain about the use of the internal types.
tasks.withType<Javadoc>().configureEach {
    exclude(
        "**/GradleProject**",
        "**/GradleDependencyConfiguration**",
        "**/GradleSettings**"
    )
}

configure<LicenseExtension> {
    excludePatterns.add("**/gradle-wrapper/*")
}

tasks.register<JavaExec>("syncWrapperScripts") {
    classpath = sourceSets.test.get().runtimeClasspath + sourceSets.test.get().output
    mainClass = "org.openrewrite.gradle.internal.GradleWrapperScriptDownloader"
    workingDir = project.rootDir
}
