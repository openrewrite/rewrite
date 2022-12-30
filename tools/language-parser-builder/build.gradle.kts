plugins {
    id("org.openrewrite.build.language-library")
}

tasks.withType<JavaCompile> {
    options.release.set(null as Int?) // remove `--release 8` set in `org.openrewrite.java-base`
}

sourceSets {
    create("model") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}
val modelImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
val modelAnnotationProcessor: Configuration by configurations.getting
val modelCompileOnly: Configuration by configurations.getting

configurations["modelRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

dependencies {
    compileOnly("org.projectlombok:lombok:latest.release")
    compileOnly("org.openrewrite:rewrite-test")
    implementation("org.openrewrite:rewrite-java-17")

    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))
    modelAnnotationProcessor("org.projectlombok:lombok:latest.release")
    modelCompileOnly("org.projectlombok:lombok:latest.release")
    modelImplementation("ch.qos.logback:logback-classic:latest.release")
}
