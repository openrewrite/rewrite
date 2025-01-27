package org.openrewrite.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

public class PackGradleWrapperScriptsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        TaskContainer tasks = project.getTasks();

        TaskProvider<Copy> pack = tasks.register("copyGradleWrapperScripts", Copy.class);

        pack.configure(task -> {
            project.getLayout().getBuildDirectory().dir("gradle-wrapper");

            task.from(project.getRootDir().toPath().resolve("gradle/wrapper"));
            task.into(project.getProjectDir().toPath().resolve(
                    "resources/main/META-INF/rewrite/gradle-wrapper"));
        });
    }
}
