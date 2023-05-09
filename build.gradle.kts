plugins {
    id("org.openrewrite.build.root") version("1.11.6")
    id("org.openrewrite.build.java-base") version("1.11.6")
    id("org.openrewrite.rewrite") version("latest.release")
}

repositories {
    mavenCentral()
}

dependencies {
    rewrite(project(":rewrite-core"))
}

rewrite {
    failOnDryRunResults = true
    activeRecipe("org.openrewrite.self.Rewrite")
}


allprojects {
    group = "org.openrewrite"
    description = "Eliminate tech-debt. Automatically."
}
