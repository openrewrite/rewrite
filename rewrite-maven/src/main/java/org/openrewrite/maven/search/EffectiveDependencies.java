/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.table.DependencyGraph;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class EffectiveDependencies extends Recipe {
    transient DependencyGraph dependencyGraph = new DependencyGraph(this);

    @Option(displayName = "Scope",
            description = "Match dependencies with the specified scope",
            valid = {"compile", "test", "runtime", "provided"},
            example = "compile")
    String scope;

    @Override
    public String getDisplayName() {
        return "Effective dependencies";
    }

    @Override
    public String getDescription() {
        return "Emit the data of binary dependency relationships.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("scope", "scope is a valid Maven scope", scope,
                s -> Scope.fromName(s) != Scope.Invalid));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Scope aScope = Scope.fromName(scope);

        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                String javaProject = getCursor().firstEnclosingOrThrow(Xml.Document.class).getMarkers()
                        .findFirst(JavaProject.class).map(JavaProject::getProjectName).orElse("");
                String javaSourceSet = getCursor().firstEnclosingOrThrow(Xml.Document.class).getMarkers()
                        .findFirst(JavaSourceSet.class).map(JavaSourceSet::getName).orElse("main");

                for (Map.Entry<Scope, List<ResolvedDependency>> scopedDependencies : mrr.getDependencies().entrySet()) {
                    if (!scopedDependencies.getKey().isInClasspathOf(aScope)) {
                        continue;
                    }
                    emitDependency(mrr.getPom().getGav(), scopedDependencies.getValue(), ctx, javaProject, javaSourceSet);
                }
                return document;
            }
        };
    }

    private void emitDependency(ResolvedGroupArtifactVersion gav, List<ResolvedDependency> dependencies,
                                ExecutionContext ctx, String javaProject, String javaSourceSet) {
        for (ResolvedDependency d : dependencies) {
            dependencyGraph.insertRow(ctx, new DependencyGraph.Row(
                    javaProject,
                    javaSourceSet,
                    String.format("%s:%s:%s", gav.getGroupId(), gav.getArtifactId(), gav.getVersion()),
                    String.format("%s:%s:%s", d.getGav().getGroupId(), d.getGav().getArtifactId(), d.getGav().getVersion())
            ));
            emitDependency(d.getGav(), d.getDependencies(), ctx, javaProject, javaSourceSet);
        }
    }
}
