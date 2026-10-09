/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveDuplicateDependencies extends Recipe {

    private static final XPathMatcher DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencies");
    private static final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");
    private static final XPathMatcher PROFILE_DEPENDENCIES_MATCHER = new XPathMatcher("/project/profiles/profile/dependencies");
    private static final XPathMatcher PROFILE_MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/profiles/profile/dependencyManagement/dependencies");

    String displayName = "Remove duplicate Maven dependencies";

    String description = "Maven 3.10 and Maven 4 fail the build when a `<dependencies>` or `<dependencyManagement>` section, " +
                         "including those of a profile, declares the same `groupId:artifactId:type:classifier` more than once, " +
                         "regardless of `<scope>`. Earlier Maven versions only warned and kept a single declaration: " +
                         "the first one in a project's `<dependencyManagement>`, and otherwise the last one, " +
                         "in the position of the first. This recipe removes the duplicates, keeping the declaration " +
                         "earlier Maven versions used so that the resolved dependencies stay the same.";

    Duration estimatedEffortPerOccurrence = Duration.ofMinutes(2);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (MANAGED_DEPENDENCIES_MATCHER.matches(getCursor())) {
                    return removeDuplicates(t, false);
                }
                if (DEPENDENCIES_MATCHER.matches(getCursor()) ||
                    PROFILE_DEPENDENCIES_MATCHER.matches(getCursor()) ||
                    PROFILE_MANAGED_DEPENDENCIES_MATCHER.matches(getCursor())) {
                    return removeDuplicates(t, true);
                }
                return t;
            }

            private Xml.Tag removeDuplicates(Xml.Tag dependencies, boolean lastDeclarationWins) {
                Map<String, List<Xml.Tag>> declarations = new HashMap<>();
                for (Xml.Tag dependency : dependencies.getChildren("dependency")) {
                    String key = managementKey(dependency);
                    if (key != null) {
                        declarations.computeIfAbsent(key, k -> new ArrayList<>()).add(dependency);
                    }
                }
                if (declarations.values().stream().allMatch(duplicates -> duplicates.size() == 1)) {
                    return dependencies;
                }

                Set<String> seen = new HashSet<>();
                List<Content> content = new ArrayList<>();
                for (Content c : dependencies.getContent()) {
                    String key = c instanceof Xml.Tag && "dependency".equals(((Xml.Tag) c).getName()) ?
                            managementKey((Xml.Tag) c) : null;
                    if (key == null) {
                        content.add(c);
                    } else if (seen.add(key)) {
                        Xml.Tag first = (Xml.Tag) c;
                        List<Xml.Tag> duplicates = declarations.get(key);
                        Xml.Tag winner = duplicates.get(lastDeclarationWins ? duplicates.size() - 1 : 0);
                        content.add(sameDeclaration(first, winner) ? first : winner.withPrefix(first.getPrefix()));
                    }
                }
                maybeUpdateModel();
                return dependencies.withContent(content);
            }

            private @Nullable String managementKey(Xml.Tag dependency) {
                String artifactId = value(dependency, "artifactId");
                if (artifactId == null) {
                    return null;
                }
                String type = value(dependency, "type");
                String classifier = value(dependency, "classifier");
                return value(dependency, "groupId") + ":" + artifactId + ":" +
                       (type == null ? "jar" : type) +
                       (classifier == null ? "" : ":" + classifier);
            }

            // Raw text, as Maven compares it: a property can resolve differently per profile
            private @Nullable String value(Xml.Tag dependency, String child) {
                return dependency.getChildValue(child)
                        .map(String::trim)
                        .filter(v -> !v.isEmpty())
                        .orElse(null);
            }

            private boolean sameDeclaration(Xml.Tag a, Xml.Tag b) {
                for (String child : Arrays.asList("version", "scope", "optional", "systemPath")) {
                    if (!Objects.equals(value(a, child), value(b, child))) {
                        return false;
                    }
                }
                return exclusions(a).equals(exclusions(b));
            }

            private Set<String> exclusions(Xml.Tag dependency) {
                Set<String> exclusions = new HashSet<>();
                dependency.getChild("exclusions").ifPresent(e -> {
                    for (Xml.Tag exclusion : e.getChildren("exclusion")) {
                        exclusions.add(value(exclusion, "groupId") + ":" + value(exclusion, "artifactId"));
                    }
                });
                return exclusions;
            }
        };
    }
}
