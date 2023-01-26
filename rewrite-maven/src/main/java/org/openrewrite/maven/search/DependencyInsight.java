/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

/**
 * Find direct and transitive dependencies, marking first order dependencies that
 * either match or transitively include a dependency matching {@link #groupIdPattern} and
 * {@link #artifactIdPattern}.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class DependencyInsight extends Recipe {
    transient DependenciesInUse dependenciesInUse = new DependenciesInUse(this);

    @Option(displayName = "Group pattern",
            description = "Group glob pattern used to match dependencies.",
            example = "com.fasterxml.jackson.module")
    String groupIdPattern;

    @Option(displayName = "Artifact pattern",
            description = "Artifact glob pattern used to match dependencies.",
            example = "jackson-module-*")
    String artifactIdPattern;

    @Option(displayName = "Scope",
            description = "Match dependencies with the specified scope",
            valid = {"compile", "test", "runtime", "provided"},
            example = "compile")
    String scope;

    UUID searchId = randomId();

    @Override
    public Validated validate() {
        return super.validate().and(Validated.test("scope", "scope is a valid Maven scope", scope, s -> {
            try {
                //noinspection ResultOfMethodCallIgnored
                Scope.fromName(s);
                return true;
            } catch (Throwable t) {
                return false;
            }
        }));
    }

    @Override
    public String getDisplayName() {
        return "Maven dependency insight";
    }

    @Override
    public String getDescription() {
        return "Find direct and transitive dependencies matching a group, artifact, and scope. " +
               "Results include dependencies that either directly match or transitively include a matching dependency.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Scope aScope = Scope.fromName(scope);

        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isDependencyTag()) {
                    ResolvedDependency dependency = findDependency(t, aScope);
                    if (dependency != null) {
                        ResolvedDependency match = dependency.findDependency(groupIdPattern, artifactIdPattern);
                        if (match != null) {
                            if (match == dependency) {
                                t = SearchResult.found(t);
                            } else {
                                t = SearchResult.found(t, match.getGav().toString());
                            }

                            Optional<JavaProject> javaProject = getCursor().firstEnclosingOrThrow(Xml.Document.class).getMarkers()
                                    .findFirst(JavaProject.class);
                            Optional<JavaSourceSet> javaSourceSet = getCursor().firstEnclosingOrThrow(Xml.Document.class).getMarkers()
                                    .findFirst(JavaSourceSet.class);

                            dependenciesInUse.insertRow(ctx, new DependenciesInUse.Row(
                                    javaProject.map(JavaProject::getProjectName).orElse(""),
                                    javaSourceSet.map(JavaSourceSet::getName).orElse("main"),
                                    match.getGroupId(),
                                    match.getArtifactId(),
                                    match.getVersion(),
                                    match.getDatedSnapshotVersion(),
                                    StringUtils.isBlank(match.getRequested().getScope()) ? "compile" :
                                            match.getRequested().getScope(),
                                    match.getDepth()
                            ));
                        }
                    }
                }

                return t;
            }
        };
    }
}
