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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.marker.RecipeSearchResult;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
public class FindDependencies extends Recipe {
    private final String groupIdPattern;
    private final String artifactIdPattern;
    private final String scope;

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
                                t = t.withMarker(new RecipeSearchResult(FindDependencies.this));
                            } else {
                                t = t.withMarker(new RecipeSearchResult(FindDependencies.this,
                                        match.get().getCoordinates()));
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
}
