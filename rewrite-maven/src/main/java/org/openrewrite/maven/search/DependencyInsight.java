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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.marker.XmlSearchResult;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

/**
 * Find direct and transitive dependencies, marking first order dependencies that
 * either match or transitively include a dependency matching {@link #groupIdPattern} and
 * {@link #artifactIdPattern}.
 */
@Incubating(since = "7.0.0")
@EqualsAndHashCode(callSuper = true)
@Value
public class DependencyInsight extends Recipe {

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
            valid = {"compile", "test", "runtime", "provided"})
    String scope;

    UUID searchId = randomId();

    @Override
    public Validated validate() {
        return super.validate().and(Validated.test("scope", "scope is a valid Maven scope", scope, s -> {
            try {
                //noinspection ResultOfMethodCallIgnored
                Scope.fromName(s);
                return true;
            } catch(Throwable t) {
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
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern groupIdMatcher = Pattern.compile(groupIdPattern.replace(".", "\\.")
                .replace("*", ".*"));
        Pattern artifactIdMatcher = Pattern.compile(artifactIdPattern.replace(".", "\\.")
                .replace("*", ".*"));
        Scope aScope = Scope.fromName(scope);

        return new MavenVisitor() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext context) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, context);

                if (isDependencyTag()) {
                    Pom.Dependency dependency = findDependency(t);
                    if(dependency != null) {
                        Set<Pom.Dependency> dependencies = model.getDependencies(dependency, aScope);
                        Optional<Pom.Dependency> match = dependencies.stream().filter(this::dependencyMatches).findFirst();
                        if(match.isPresent()) {
                            if(dependencyMatches(dependency)) {
                                t = t.withMarkers(t.getMarkers().addIfAbsent(new XmlSearchResult(searchId, DependencyInsight.this)));
                            } else {
                                t = t.withMarkers(t.getMarkers().addIfAbsent(new XmlSearchResult(searchId,DependencyInsight.this,
                                        match.get().getCoordinates())));
                            }
                        }
                    }
                }

                return t;
            }

            private boolean dependencyMatches(Pom.Dependency d) {
                return groupIdMatcher.matcher(d.getGroupId()).matches() &&
                        artifactIdMatcher.matcher(d.getArtifactId()).matches();
            }
        };
    }

    /**
     * This method will search the Maven tree for a dependency (including any transitive references) and return
     * true if the dependency is found.
     *
     * @param maven The maven tree to search.
     * @param groupIdPattern The artifact's group ID
     * @param artifactIdPattern The artifact's ID
     * @param scope An optional scope.
     *
     * @return true if the dependency is found.
     */
    public static boolean isDependencyPresent(Maven maven, String groupIdPattern, String artifactIdPattern, @Nullable String scope) {
        DependencyInsight insight = new DependencyInsight(groupIdPattern, artifactIdPattern, scope);
        return insight.getVisitor().visit(maven, new InMemoryExecutionContext()) != maven;
    }
}
