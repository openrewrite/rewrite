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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.SpringDependencyManagementPlugin;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds {@code junit-platform-launcher} to {@code testRuntimeOnly}, in the version of the JUnit Platform the project
 * already uses, since a launcher from another JUnit generation (e.g. 1.x on JUnit 6) fails at test startup.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddJUnitPlatformLauncher extends ScanningRecipe<AddDependency.Scanned> {

    private static final String JUNIT_PLATFORM_GROUP = "org.junit.platform";
    private static final GroupArtifact LAUNCHER = new GroupArtifact(JUNIT_PLATFORM_GROUP, "junit-platform-launcher");

    // Used when nothing tells which JUnit Platform the project is on
    private static final String FALLBACK_VERSION = "1.x";

    String displayName = "Add JUnit Platform Launcher";

    String description = "Add the JUnit Platform Launcher to the buildscript dependencies. " +
            "The launcher is added without a version when a BOM manages it, as JUnit's own `junit-bom` does " +
            "from JUnit 5.6 on, and otherwise in the version of the JUnit Platform already on the test runtime classpath.";

    private static AddDependency addDependency(@Nullable String version) {
        return new AddDependency(LAUNCHER.getGroupId(), LAUNCHER.getArtifactId(), version, null,
                "testRuntimeOnly", "org.junit.jupiter.api.Test", null, null, null, true);
    }

    @Override
    public AddDependency.Scanned getInitialValue(ExecutionContext ctx) {
        return addDependency(null).getInitialValue(ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AddDependency.Scanned acc) {
        return addDependency(null).getScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AddDependency.Scanned acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                GradleProject gp = tree.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (gp == null) {
                    return tree;
                }
                return addDependency(launcherVersion(gp, ctx)).getVisitor(acc).visit(tree, ctx);
            }
        };
    }

    /**
     * @return null when the launcher's version is already managed, otherwise the version to add it in.
     */
    private static @Nullable String launcherVersion(GradleProject gp, ExecutionContext ctx) {
        SpringDependencyManagementPlugin springDependencyManagement = gp.getSpringDependencyManagementPlugin();
        if (springDependencyManagement != null &&
                springDependencyManagement.getManagedVersions().containsKey(LAUNCHER.getGroupId() + ":" + LAUNCHER.getArtifactId())) {
            return null;
        }
        GradleDependencyConfiguration testRuntimeClasspath = gp.getConfiguration("testRuntimeClasspath");
        if (testRuntimeClasspath == null) {
            return FALLBACK_VERSION;
        }
        // A platform is typically declared on a configuration testRuntimeClasspath extends from, like testImplementation
        List<GradleDependencyConfiguration> configurations = new ArrayList<>(testRuntimeClasspath.allExtendsFrom());
        configurations.add(0, testRuntimeClasspath);
        for (GradleDependencyConfiguration configuration : configurations) {
            if (configuration.getPlatformManagedVersion(LAUNCHER, gp.getMavenRepositories(), ctx) != null) {
                return null;
            }
        }
        // Since JUnit 5.6 every JUnit artifact brings in junit-bom as a platform, which supplies the launcher's version
        if (testRuntimeClasspath.findResolvedDependency("org.junit", "junit-bom") != null) {
            return null;
        }
        // junit-jupiter-api brings in junit-platform-commons, junit-jupiter-engine brings in junit-platform-engine
        for (String platformArtifact : new String[]{"junit-platform-engine", "junit-platform-commons"}) {
            ResolvedDependency resolved = testRuntimeClasspath.findResolvedDependency(JUNIT_PLATFORM_GROUP, platformArtifact);
            if (resolved != null) {
                return resolved.getVersion();
            }
        }
        return FALLBACK_VERSION;
    }
}
