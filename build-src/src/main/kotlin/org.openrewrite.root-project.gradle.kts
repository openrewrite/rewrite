plugins {
    id("org.openrewrite.base")
    id("org.openrewrite.rewrite")
    id("nebula.release")
    id("io.github.gradle-nexus.publish-plugin")
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
