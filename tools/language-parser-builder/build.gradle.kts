buildscript {
    configurations.all {
        resolutionStrategy {
            eachDependency {
                if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-compiler-embeddable") {
                    useTarget("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.10")
                }
            }
        }
    }
}
plugins {
    id("org.openrewrite.build.language-library") version("latest.release")
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
    implementation("org.openrewrite:rewrite-java-21")

    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))
    modelAnnotationProcessor("org.projectlombok:lombok:latest.release")
    modelCompileOnly("org.projectlombok:lombok:latest.release")
    modelImplementation("ch.qos.logback:logback-classic:latest.release")
}

tasks.register<JavaExec>("runGenerator") {
    mainClass = "generate.GenerateModel"
    classpath = sourceSets.getByName("model").runtimeClasspath
    workingDir = file("../..")
}

license {
    header = file("../../gradle/licenseHeader.txt")
}
