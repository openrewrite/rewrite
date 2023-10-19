plugins {
    id("org.openrewrite.build.root") version("latest.release")
    id("org.openrewrite.build.java-base") version("latest.release")
}

repositories {
    mavenCentral()
}

allprojects {
    group = "org.openrewrite"
    description = "Eliminate tech-debt. Automatically."
}
