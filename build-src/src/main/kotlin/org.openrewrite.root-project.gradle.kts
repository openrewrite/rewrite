plugins {
    `java-library`
    id("org.openrewrite.java-base")
    id("nebula.release")
    id("io.github.gradle-nexus.publish-plugin")
    id("org.openrewrite.rewrite")
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

configure<nebula.plugin.release.git.base.ReleasePluginExtension> {
    defaultVersionStrategy = nebula.plugin.release.NetflixOssStrategies.SNAPSHOT(project)
}

configure<org.openrewrite.gradle.RewriteExtension> {
    activeRecipes = listOf("org.openrewrite.java.format.AutoFormat")
}

defaultTasks("build")
