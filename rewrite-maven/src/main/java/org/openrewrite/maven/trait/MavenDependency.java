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
package org.openrewrite.maven.trait;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.trait.Trait;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Map;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
public class MavenDependency implements Trait<Xml.Tag> {
    private static final XPathMatcher DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencies/dependency");
    private static final XPathMatcher PROFILE_DEPENDENCY_MATCHER = new XPathMatcher("/project/profiles/profile/dependencies/dependency");

    Cursor cursor;

    @Getter
    ResolvedDependency resolvedDependency;

    public static class Matcher extends MavenTraitMatcher<MavenDependency> {
        @Nullable
        protected String groupId;

        @Nullable
        protected String artifactId;

        public Matcher groupId(@Nullable String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Matcher artifactId(@Nullable String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        @Override
        protected @Nullable MavenDependency test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Xml.Tag) {
                Xml.Tag tag = (Xml.Tag) value;

                // `XPathMatcher` is still a bit expensive
                if (!"dependency".equals(tag.getName()) ||
                    (!DEPENDENCY_MATCHER.matches(cursor) &&
                     !PROFILE_DEPENDENCY_MATCHER.matches(cursor))) {
                    return null;
                }

                Map<Scope, List<ResolvedDependency>> dependencies = getResolutionResult(cursor).getDependencies();
                for (Scope scope : Scope.values()) {
                    if (dependencies.containsKey(scope)) {
                        for (ResolvedDependency resolvedDependency : dependencies.get(scope)) {
                            if ((groupId == null || matchesGlob(resolvedDependency.getGroupId(), groupId)) &&
                                (artifactId == null || matchesGlob(resolvedDependency.getArtifactId(), artifactId))) {
                                String scopeName = tag.getChildValue("scope").orElse(null);
                                Scope tagScope = scopeName != null ? Scope.fromName(scopeName) : null;
                                if (tagScope == null && artifactId != null) {
                                    tagScope = getResolutionResult(cursor).getPom().getManagedScope(
                                            groupId,
                                            artifactId,
                                            tag.getChildValue("type").orElse(null),
                                            tag.getChildValue("classifier").orElse(null)
                                    );
                                }
                                if (tagScope == null) {
                                    tagScope = Scope.Compile;
                                }
                                Dependency req = resolvedDependency.getRequested();
                                String reqGroup = req.getGroupId();
                                if ((reqGroup == null || reqGroup.equals(tag.getChildValue("groupId").orElse(null))) &&
                                    req.getArtifactId().equals(tag.getChildValue("artifactId").orElse(null)) &&
                                    scope == tagScope) {
                                    return new MavenDependency(cursor, resolvedDependency);
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }
    }
}
