/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.internal;

import lombok.Value;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.marker.GradleBuildscript;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.MavenRepository;

import java.util.List;

/**
 * The repositories a project resolves from, which a recipe can keep where keeping the project would pin its whole
 * dependency graph.
 */
@Value
public class ProjectRepositories {
    List<MavenRepository> repositories;

    /**
     * The repositories of the buildscript classpath, which plugins resolve from.
     */
    List<MavenRepository> classpathRepositories;

    public static ProjectRepositories of(GradleProject gradleProject) {
        return new ProjectRepositories(gradleProject.getMavenRepositories(),
                gradleProject.getBuildscript().getMavenRepositories());
    }

    /**
     * The settings script's buildscript repositories, which a selector given settings resolves everything from.
     */
    public static ProjectRepositories of(GradleSettings gradleSettings) {
        List<MavenRepository> repositories = gradleSettings.getBuildscript().getMavenRepositories();
        return new ProjectRepositories(repositories, repositories);
    }

    /**
     * A selector that downloads metadata from these repositories. It knows no configurations to find a dependency's
     * current version in.
     */
    public DependencyVersionSelector selector(MavenMetadataFailures metadataFailures) {
        return new DependencyVersionSelector(metadataFailures, GradleProject.builder()
                .mavenRepositories(repositories)
                .buildscript(GradleBuildscript.builder().mavenRepositories(classpathRepositories).build())
                .build(), null);
    }
}
