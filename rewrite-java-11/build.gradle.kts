plugins {
    id("nebula.integtest") version "7.0.9" apply false
}

apply(plugin = "nebula.integtest-standalone")

val integTestImplementation = configurations.getByName("integTestImplementation")

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    testImplementation(project(":rewrite-test"))

    integTestImplementation("io.micrometer:micrometer-registry-prometheus:latest.release")
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()

    options.compilerArgs.clear() // remove `--release 8` set in root gradle build
    options.compilerArgs.addAll(listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"
    ))
}

tasks.withType<Javadoc> {
    exclude("**/Java11Parser**")
}
