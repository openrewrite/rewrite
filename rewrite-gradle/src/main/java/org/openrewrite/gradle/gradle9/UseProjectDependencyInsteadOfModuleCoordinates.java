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
package org.openrewrite.gradle.gradle9;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.internal.GradleParseUtils;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Optional;

public class UseProjectDependencyInsteadOfModuleCoordinates extends Recipe {

    @Getter
    final String displayName = "Use `project(...)` dependency notation instead of the current project's module coordinates";

    @Getter
    final String description = "Gradle 9.3 deprecates depending on the current project by its own `group:name:version` module " +
            "coordinates. In Gradle 9.x such a declaration resolves to the project's local outgoing variants, " +
            "but in Gradle 10 it will instead attempt resolution from a repository. This recipe replaces a " +
            "dependency declaration whose coordinates match the current project with the equivalent " +
            "`project(\"<path>\")` notation. Requires the `GradleProject` marker (available when parsed by the " +
            "OpenRewrite Gradle plugin) to know the current project's coordinates and path.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                // Cheap, allocation-free rejections first; defer the ancestor walk to the rare candidate that
                // is a bare configuration call taking a single string-literal coordinate.
                if (m.getSelect() != null || m.getArguments().isEmpty() || !(m.getArguments().get(0) instanceof J.Literal)) {
                    return m;
                }
                J.Literal coordinate = (J.Literal) m.getArguments().get(0);
                if (!(coordinate.getValue() instanceof String) || !withinDependenciesNotConstraints(getCursor())) {
                    return m;
                }

                GradleProject gp = getGradleProject(getCursor().firstEnclosing(SourceFile.class));
                if (gp == null || gp.getGroup() == null) {
                    return m;
                }
                String[] parts = ((String) coordinate.getValue()).split(":");
                if (parts.length < 2) {
                    return m;
                }
                if (!gp.getGroup().equals(parts[0]) || !gp.getName().equals(parts[1])) {
                    return m;
                }

                // The `OmitParentheses` marker (Groovy command syntax, e.g. `implementation 'a:b:c'`) lives on
                // the argument element itself, so carry the original coordinate's markers and prefix over to the
                // replacement to preserve whether the enclosing call uses parentheses.
                Expression projectNotation = GradleParseUtils.parseMethodInvocation(ctx, "project('" + projectPath(gp) + "')\n")
                        .withPrefix(coordinate.getPrefix())
                        .withMarkers(coordinate.getMarkers());
                return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> projectNotation));
            }
        });
    }

    private static boolean withinDependenciesNotConstraints(Cursor cursor) {
        boolean insideDependencies = false;
        Cursor c = cursor.getParent();
        while (c != null) {
            if (c.getValue() instanceof J.MethodInvocation) {
                String name = ((J.MethodInvocation) c.getValue()).getSimpleName();
                if ("constraints".equals(name)) {
                    return false;
                }
                if ("dependencies".equals(name)) {
                    insideDependencies = true;
                }
            }
            c = c.getParent();
        }
        return insideDependencies;
    }

    private static @Nullable GradleProject getGradleProject(@Nullable SourceFile sourceFile) {
        if (sourceFile == null) {
            return null;
        }
        Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
        return maybeGp.orElse(null);
    }

    private static String projectPath(GradleProject gp) {
        String path = gp.getPath();
        return path == null || path.isEmpty() ? ":" : path;
    }
}
