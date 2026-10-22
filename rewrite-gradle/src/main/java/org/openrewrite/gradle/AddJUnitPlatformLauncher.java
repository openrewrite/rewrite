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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.tree.ResolvedDependency;

@EqualsAndHashCode(callSuper = false)
public class AddJUnitPlatformLauncher extends ScanningRecipe<AddDependency.Scanned> {
    @Override
    public String getDisplayName() {
        return "Add JUnit Platform Launcher";
    }

    @Override
    public String getDescription() {
        return "Add the JUnit Platform Launcher to the test runtime dependencies, aligned with the resolved JUnit Platform version.";
    }

    @Override
    public AddDependency.Scanned getInitialValue(ExecutionContext ctx) {
        return addDependency("1.x").getInitialValue(ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AddDependency.Scanned acc) {
        return addDependency("1.x").getScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AddDependency.Scanned acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                GradleProject project = ((JavaSourceFile) tree).getMarkers().findFirst(GradleProject.class).orElse(null);
                if (project == null) {
                    return tree;
                }
                String version = "1.x";
                for (String configurationName : new String[]{"testRuntimeClasspath", "testCompileClasspath"}) {
                    GradleDependencyConfiguration configuration = project.getConfiguration(configurationName);
                    if (configuration == null) {
                        continue;
                    }
                    ResolvedDependency platform = configuration.findResolvedDependency("org.junit.platform", "junit-platform-engine");
                    if (platform == null) {
                        platform = configuration.findResolvedDependency("org.junit.platform", "junit-platform-commons");
                    }
                    if (platform != null) {
                        version = platform.getVersion();
                        break;
                    }
                }
                return addDependency(version).getVisitor(acc).visit(tree, ctx);
            }
        };
    }

    private static AddDependency addDependency(String version) {
        return new AddDependency("org.junit.platform", "junit-platform-launcher", version, null,
                "testRuntimeOnly", "org.junit.jupiter.api.Test", null, null, null, true);
    }
}
