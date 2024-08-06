/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.search;

import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.table.ClasspathTypeCount;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptyList;

public class ClasspathTypeCounts extends ScanningRecipe<Set<ClasspathTypeCounts.ProjectSourceSet>> {
    private final transient ClasspathTypeCount counts = new ClasspathTypeCount(this);

    @Override
    public String getDisplayName() {
        return "Study the size of the classpath by source set";
    }

    @Override
    public String getDescription() {
        return "Emit one data table row per source set in a project, with the number of types in the source set.";
    }

    @Override
    public Set<ProjectSourceSet> getInitialValue(ExecutionContext ctx) {
        return new LinkedHashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<ProjectSourceSet> acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J preVisit(J tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaProject javaProject = tree.getMarkers().findFirst(JavaProject.class).orElse(null);
                    JavaSourceSet sourceSet = tree.getMarkers().findFirst(JavaSourceSet.class).orElse(null);
                    if (javaProject != null && sourceSet != null) {
                        acc.add(new ProjectSourceSet(javaProject, sourceSet));
                    }
                }
                stopAfterPreVisit();
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Set<ProjectSourceSet> acc, ExecutionContext ctx) {
        for (ProjectSourceSet projectSourceSet : acc) {
            counts.insertRow(ctx, new ClasspathTypeCount.Row(
                    projectSourceSet.getJavaProject().getProjectName(),
                    projectSourceSet.getSourceSet().getName(),
                    projectSourceSet.getSourceSet().getClasspath().size()
            ));
        }
        return emptyList();
    }

    @Value
    public static class ProjectSourceSet {
        JavaProject javaProject;
        JavaSourceSet sourceSet;
    }
}
