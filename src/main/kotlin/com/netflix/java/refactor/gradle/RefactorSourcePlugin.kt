package com.netflix.java.refactor.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class RefactorSourcePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType(JavaPlugin::class.java) {
            project.tasks.create("fixSourceLint", RefactorAndFixSourceTask::class.java)
        }
    }
}