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
import java.util.Properties
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
// Gradle 7 moved part of org.gradle.util to org.gradle.util.internal and declared the classes it left public.
// Gradle 6 has the same classes, and builds call them, as in GradleVersion.current(), but its declaration leaves
// out the package.
val gradle6UtilApi = listOf(
    "ClosureBackedAction", "CollectionUtils", "Configurable", "ConfigureUtil", "DistributionLocator", "GFileUtils",
    "GUtil", "GradleVersion", "NameMatcher", "NameValidator", "Path", "RelativePathUtil", "SingleMessageLogger",
    "TextUtil", "VersionNumber", "WrapUtil"
).map { "org/gradle/util/$it" }.toSet()
// The Groovy runtime the Groovy parser runs on, as rewrite-groovy declares it. Gradle 6 ships Groovy as groovy-all, which
// also holds modules such as groovy-ant, so a distribution's own Groovy jars cannot tell which classes the parser has.
val parserGroovy = configurations.detachedConfiguration(dependencies.create("org.apache.groovy:groovy:4.+"))
    .apply { isTransitive = false }
// The Kotlin compiler rewrite-kotlin parses with, so that it reads the metadata of the accessors compiled with this one
val kotlinCompiler = configurations.detachedConfiguration(dependencies.create("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.20"))

// Gradle has published no API modules since 6.1.1, so the API of each major is taken from its distribution
val addGradleApiToTypeTable = tasks.register("addGradleApiToTypeTable") {
    group = "OpenRewrite"
    description = "Adds the public API of each major Gradle version to the type table as org.gradle:gradle-public-api, " +
            "and the accessors Gradle generates for Kotlin scripts as org.openrewrite:gradle-kotlin-dsl-accessors."
    gradleDistributions.forEach { inputs.files(it) }
    inputs.files(parserGroovy, kotlinCompiler)
    val table = layout.projectDirectory.file("src/main/resources/META-INF/rewrite/classpath.tsv.gz").asFile
    doLast {
        // Rows are split on '\n' only, as bytes, since annotation values in them hold other control characters
        fun rows(gzipped: ByteArray) = GZIPInputStream(ByteArrayInputStream(gzipped)).readBytes()
            .toString(Charsets.ISO_8859_1).split('\n').filter { it.isNotEmpty() }
        val rows = rows(table.readBytes()).filter {
            !it.startsWith("org.gradle\tgradle-public-api\t") && !it.startsWith("org.openrewrite\tgradle-kotlin-dsl-accessors\t")
        }.toMutableList()
        val groovyCore = ZipInputStream(parserGroovy.singleFile.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.map { it.name }.filter { it.endsWith(".class") }
                .map { it.removeSuffix(".class") }.toSet()
        }
        for (distribution in gradleDistributions) {
            val version = distribution.resolvedConfiguration.resolvedArtifacts.single().moduleVersion.id.version
            val api = temporaryDir.resolve("gradle-api-$version.jar")
            val undeclaredApi = if (version.startsWith("6.")) gradle6UtilApi else emptySet()
            writePublicApi(distribution.singleFile, api, groovyCore, undeclaredApi)
            val written = ByteArrayOutputStream()
            TypeTable.newWriter(written).use { it.jar("org.gradle", "gradle-public-api", version).write(api.toPath()) }
            rows += rows(written.toByteArray()).drop(1)
        }
        // One compilation, against the newest API, serves every version
        val newestApi = temporaryDir.resolve("gradle-api-${gradleDistributions.last().resolvedConfiguration.resolvedArtifacts.single().moduleVersion.id.version}.jar")
        val accessors = temporaryDir.resolve("gradle-kotlin-dsl-accessors.jar")
        writeKotlinDslAccessors(newestApi, accessors, temporaryDir)
        val written = ByteArrayOutputStream()
        TypeTable.newWriter(written).use { it.jar("org.openrewrite", "gradle-kotlin-dsl-accessors", "1").write(accessors.toPath()) }
        rows += rows(written.toByteArray()).drop(1)
        GZIPOutputStream(table.outputStream()).use { out ->
            rows.forEach { out.write(it.toByteArray(Charsets.ISO_8859_1)); out.write('\n'.code) }
        }
    }
}
tasks.named("createTypeTable") {
    finalizedBy(addGradleApiToTypeTable)
}

/**
 * The configurations, extensions and tasks of Gradle's core plugins and the most common Android and Kotlin ones, for
 * which Gradle generates Kotlin accessors in each build that applies the plugin.
 */
val kotlinDslConfigurations = listOf(
    "api", "implementation", "compileOnly", "compileOnlyApi", "runtimeOnly", "annotationProcessor",
    "testImplementation", "testCompileOnly", "testRuntimeOnly", "testAnnotationProcessor",
    "testFixturesApi", "testFixturesImplementation", "testFixturesCompileOnly", "testFixturesRuntimeOnly",
    "providedCompile", "providedRuntime", "deploy", "earlib",
    "classpath", "jacocoAggregation", "testReportAggregation", "checkstyle", "pmd", "antlr",
    "androidTestImplementation", "androidTestRuntimeOnly", "androidTestAnnotationProcessor",
    "debugImplementation", "releaseImplementation", "testDebugImplementation", "coreLibraryDesugaring",
    "kapt", "ksp"
)
val kotlinDslExtensions = mapOf(
    "java" to "org.gradle.api.plugins.JavaPluginExtension",
    "base" to "org.gradle.api.plugins.BasePluginExtension",
    "sourceSets" to "org.gradle.api.tasks.SourceSetContainer",
    "publishing" to "org.gradle.api.publish.PublishingExtension",
    "application" to "org.gradle.api.plugins.JavaApplication",
    "javaPlatform" to "org.gradle.api.plugins.JavaPlatformExtension",
    "distributions" to "org.gradle.api.distribution.DistributionContainer",
    "signing" to "org.gradle.plugins.signing.SigningExtension",
    "jacoco" to "org.gradle.testing.jacoco.plugins.JacocoPluginExtension",
    "checkstyle" to "org.gradle.api.plugins.quality.CheckstyleExtension",
    "pmd" to "org.gradle.api.plugins.quality.PmdExtension",
    "eclipse" to "org.gradle.plugins.ide.eclipse.model.EclipseModel",
    "idea" to "org.gradle.plugins.ide.idea.model.IdeaModel",
    "testing" to "org.gradle.testing.base.TestingExtension",
    "reporting" to "org.gradle.api.reporting.ReportingExtension",
    "gradlePlugin" to "org.gradle.plugin.devel.GradlePluginDevelopmentExtension"
)
val kotlinDslTasks = mapOf(
    "assemble" to "org.gradle.api.DefaultTask",
    "build" to "org.gradle.api.DefaultTask",
    "check" to "org.gradle.api.DefaultTask",
    "clean" to "org.gradle.api.tasks.Delete",
    "compileJava" to "org.gradle.api.tasks.compile.JavaCompile",
    "compileTestJava" to "org.gradle.api.tasks.compile.JavaCompile",
    "processResources" to "org.gradle.language.jvm.tasks.ProcessResources",
    "processTestResources" to "org.gradle.language.jvm.tasks.ProcessResources",
    "test" to "org.gradle.api.tasks.testing.Test",
    "jar" to "org.gradle.api.tasks.bundling.Jar",
    "javadoc" to "org.gradle.api.tasks.javadoc.Javadoc",
    "javadocJar" to "org.gradle.api.tasks.bundling.Jar",
    "sourcesJar" to "org.gradle.api.tasks.bundling.Jar",
    "run" to "org.gradle.api.tasks.JavaExec",
    "jacocoTestReport" to "org.gradle.testing.jacoco.tasks.JacocoReport",
    "jacocoTestCoverageVerification" to "org.gradle.testing.jacoco.tasks.JacocoCoverageVerification",
    "wrapper" to "org.gradle.api.tasks.wrapper.Wrapper"
)

/**
 * Compiles Kotlin declarations of the accessors Gradle generates for each build to a jar, so that the Kotlin parser
 * reads them from its classpath instead of compiling them with every script.
 *
 * They are in the scripts' own package, whose declarations Kotlin prefers over the ones default imports bring in.
 * Their file facades are named after the types the Groovy DSL declares the same methods on. So
 * `DependencyHandlerSpec *(..)` matches a dependency in either DSL.
 */
fun writeKotlinDslAccessors(gradleApi: File, jar: File, workDir: File) {
    val sources = workDir.resolve("kotlin-dsl-accessors").apply { deleteRecursively(); mkdirs() }
    val project = StringBuilder("""
        @file:JvmName("RewriteGradleProject")
        import org.gradle.api.Action
        import org.gradle.api.NamedDomainObjectProvider
        import org.gradle.api.Project
        import org.gradle.api.artifacts.Configuration
        import org.gradle.api.artifacts.ConfigurationContainer
        import org.gradle.api.artifacts.DependencyConstraint
        import org.gradle.api.artifacts.dsl.DependencyConstraintHandler
        import org.gradle.api.tasks.SourceSet
        import org.gradle.api.tasks.SourceSetContainer
        import org.gradle.api.tasks.TaskContainer
        import org.gradle.api.tasks.TaskProvider
        import org.gradle.kotlin.dsl.ScriptHandlerScope
        import org.gradle.plugin.use.PluginDependenciesSpec
        // Gradle declares these on the script, not the project
        fun Project.plugins(block: PluginDependenciesSpec.() -> Unit): Unit = TODO()
        fun Project.buildscript(block: ScriptHandlerScope.() -> Unit): Unit = TODO()
        val SourceSetContainer.main: NamedDomainObjectProvider<SourceSet> get() = TODO()
        val SourceSetContainer.test: NamedDomainObjectProvider<SourceSet> get() = TODO()

        """.trimIndent())
    kotlinDslExtensions.forEach { (name, type) ->
        project.append("val Project.`$name`: $type get() = TODO()\n")
            .append("fun Project.`$name`(configure: Action<$type>): Unit = TODO()\n")
    }
    kotlinDslTasks.forEach { (name, type) ->
        project.append("val TaskContainer.`$name`: TaskProvider<$type> get() = TODO()\n")
    }
    val dependencies = StringBuilder("""
        @file:JvmName("DependencyHandlerSpec")
        import org.gradle.api.Action
        import org.gradle.api.artifacts.Dependency
        import org.gradle.api.artifacts.ExternalModuleDependency
        import org.gradle.api.artifacts.ModuleDependency
        import org.gradle.api.artifacts.dsl.DependencyHandler
        import org.gradle.api.provider.Provider

        """.trimIndent())
    for (name in kotlinDslConfigurations) {
        project.append("val ConfigurationContainer.`$name`: NamedDomainObjectProvider<Configuration> get() = TODO()\n")
            .append("fun DependencyConstraintHandler.`$name`(constraintNotation: Any): DependencyConstraint? = TODO()\n")
            .append("fun DependencyConstraintHandler.`$name`(constraintNotation: Any, block: Action<DependencyConstraint>): DependencyConstraint = TODO()\n")
        val fn = "fun DependencyHandler.`$name`"
        dependencies.append("$fn(dependencyNotation: Any): Dependency? = TODO()\n")
            .append("$fn(dependencyNotation: String, dependencyConfiguration: Action<ExternalModuleDependency>): ExternalModuleDependency = TODO()\n")
            .append("$fn(dependencyNotation: Provider<*>, dependencyConfiguration: Action<ExternalModuleDependency>): Unit = TODO()\n")
            .append("$fn(group: String, name: String, version: String? = null, configuration: String? = null, classifier: String? = null, ext: String? = null, dependencyConfiguration: Action<ExternalModuleDependency>? = null): ExternalModuleDependency = TODO()\n")
            .append("fun <T : ModuleDependency> DependencyHandler.`$name`(dependency: T, dependencyConfiguration: T.() -> Unit): T = TODO()\n")
    }
    val settings = """
        @file:JvmName("RewriteSettings")
        import org.gradle.api.initialization.Settings
        import org.gradle.plugin.management.PluginManagementSpec
        import org.gradle.plugin.use.PluginDependenciesSpec
        fun Settings.plugins(block: PluginDependenciesSpec.() -> Unit): Unit = TODO()
        fun Settings.pluginManagement(block: PluginManagementSpec.() -> Unit): Unit = TODO()
        """.trimIndent()
    sources.resolve("RewriteGradleProject.kt").writeText(project.toString())
    sources.resolve("DependencyHandlerSpec.kt").writeText(dependencies.toString())
    sources.resolve("RewriteSettings.kt").writeText(settings)

    val classes = workDir.resolve("kotlin-dsl-accessors-classes").apply { deleteRecursively(); mkdirs() }
    val stdlib = kotlinCompiler.files.single { it.name.startsWith("kotlin-stdlib-") && !it.name.contains("-common") }
    providers.javaexec {
        classpath(kotlinCompiler)
        mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
        args("-d", classes.path, "-classpath", listOf(gradleApi, stdlib).joinToString(File.pathSeparator),
            "-no-stdlib", "-no-reflect", "-nowarn", "-module-name", "gradle-kotlin-dsl-accessors")
        args(sources.listFiles()!!.map { it.path })
    }.result.get().assertNormalExitValue()
    JarOutputStream(jar.outputStream()).use { out ->
        classes.walkTopDown().filter { it.isFile }.forEach {
            out.putNextEntry(JarEntry(it.relativeTo(classes).invariantSeparatorsPath))
            out.write(it.readBytes())
            out.closeEntry()
        }
    }
}

/**
 * Writes the classes of the API a Gradle distribution declares in its gradle-api-metadata jar to a jar, with the classes
 * their declarations extend, which type attribution needs to find inherited members. A member or annotation that
 * refers to a class outside the jar is left out, since Groovy cannot resolve the members of a class when one of them
 * refers to a missing class.
 *
 * The declaration leaves out the `org.gradle.kotlin.dsl` package, which Kotlin build scripts import by default, so it is
 * added with the module files of its jars. The Kotlin compiler reads the members of these classes from their
 * `kotlin.Metadata` annotation, which is kept.
 *
 * The classes in [undeclaredApi] are added as if declared, for API an older declaration leaves out.
 */
fun writePublicApi(distribution: File, jar: File, groovyCore: Set<String>, undeclaredApi: Set<String>) {
    val classes = mutableMapOf<String, ByteArray>()
    val kotlinModules = mutableMapOf<String, ByteArray>()
    val apiDeclaration = Properties()
    ZipInputStream(distribution.inputStream()).use { zip ->
        generateSequence { zip.nextEntry }
            .filter { Regex("gradle-[^/]+/lib/(plugins/)?[^/]+\\.jar").matches(it.name) }
            .forEach { lib ->
                val kotlinDslJar = lib.name.substringAfterLast('/').startsWith("gradle-kotlin-dsl")
                val nested = ZipInputStream(ByteArrayInputStream(zip.readBytes()))
                generateSequence { nested.nextEntry }
                    .onEach { e -> if (e.name == "gradle-api-declaration.properties") apiDeclaration.load(nested) }
                    .onEach { e -> if (kotlinDslJar && e.name.endsWith(".kotlin_module")) kotlinModules[e.name] = nested.readBytes() }
                    // The Kotlin parser brings its own Kotlin standard library
                    .filter { e -> e.name.endsWith(".class") && !e.name.startsWith("java/") && !e.name.startsWith("kotlin/") }
                    .forEach { e ->
                        val name = e.name.removeSuffix(".class")
                        if (name !in groovyCore) classes.putIfAbsent(name, nested.readBytes())
                    }
            }
    }
    fun antPatterns(key: String) = apiDeclaration.getProperty(key).split(':').map { pattern ->
        Regex(pattern.split("**").joinToString(".*") { part ->
            part.split('*').joinToString("[^/]*", transform = Regex::escape)
        })
    }
    val includes = antPatterns("includes")
    val excludes = antPatterns("excludes")
    check(classes.keys.containsAll(undeclaredApi)) { "Not in the distribution: ${undeclaredApi - classes.keys}" }
    val kotlinDsl = Regex("org/gradle/kotlin/dsl/[^/]+")
    val pending = ArrayDeque(classes.keys.filter { name ->
        "$name.class".let { path -> includes.any { it.matches(path) } && excludes.none { it.matches(path) } } ||
                kotlinDsl.matches(name) || name.substringBefore('$') in undeclaredApi
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
    val jdk = ClassLoader.getPlatformClassLoader()
    val resolvable = { name: String -> name in kept || name in groovyCore || jdk.getResource("$name.class") != null }
    JarOutputStream(jar.outputStream()).use { out ->
        kept.forEach {
            out.putNextEntry(JarEntry("$it.class"))
            out.write(withoutUnresolvableMembers(classes.getValue(it), resolvable))
            out.closeEntry()
        }
        kotlinModules.forEach { (name, bytes) ->
            out.putNextEntry(JarEntry(name))
            out.write(bytes)
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
    val annotationResolvable = { descriptor: String ->
        descriptor == "Lkotlin/Metadata;" || resolvable(Type.getType(descriptor).internalName)
    }
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
