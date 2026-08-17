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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class SortDependencies extends Recipe {

    @Override
    public String getDisplayName() {
        return "Sort dependencies";
    }

    @Override
    public String getDescription() {
        return "Sort dependencies alphabetically by groupId then artifactId. " +
               "Test-scoped dependencies are sorted after non-test dependencies. " +
               "Imported BOMs retain their original positions. Applies to both `<dependencies>` and " +
               "`<dependencyManagement>` sections.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!"dependencies".equals(t.getName()) || t.getContent() == null) {
                    return t;
                }

                // Group comments with their following dependency tag
                List<DependencyGroup> groups = new ArrayList<>();
                List<Content> currentComments = new ArrayList<>();

                for (Content content : t.getContent()) {
                    if (content instanceof Xml.Tag) {
                        Xml.Tag dependency = (Xml.Tag) content;
                        groups.add(new DependencyGroup(dependency, currentComments, isImportedBom(dependency)));
                        currentComments = new ArrayList<>();
                    } else {
                        currentComments.add(content);
                    }
                }

                // If there are fewer than 2 dependency tags, nothing to sort
                if (groups.size() < 2) {
                    return t;
                }

                List<DependencyGroup> sortedReplacements = groups.stream()
                        .filter(group -> !group.importedBom)
                        .sorted(Comparator.<DependencyGroup, Boolean>comparing(
                            group -> "test".equals(group.tag.getChildValue("scope").orElse(null))
                        ).thenComparing(
                            group -> groupArtifactSortKey(group.tag)
                        ))
                        .collect(Collectors.toList());

                List<DependencyGroup> reorderedGroups = new ArrayList<>(groups);
                int replacementIndex = 0;
                for (int i = 0; i < groups.size(); i++) {
                    if (!groups.get(i).importedBom) {
                        reorderedGroups.set(i, sortedReplacements.get(replacementIndex++));
                    }
                }

                // Check if order actually changed
                boolean changed = false;
                for (int i = 0; i < groups.size(); i++) {
                    if (groups.get(i).tag != reorderedGroups.get(i).tag) {
                        changed = true;
                        break;
                    }
                }

                if (!changed) {
                    return t;
                }

                // Rebuild content preserving original whitespace prefixes
                List<Content> newContent = new ArrayList<>();
                for (int i = 0; i < reorderedGroups.size(); i++) {
                    DependencyGroup original = groups.get(i);
                    DependencyGroup reordered = reorderedGroups.get(i);

                    // Apply the prefix from the original position to the reordered content
                    for (int j = 0; j < reordered.precedingContent.size(); j++) {
                        Content c = reordered.precedingContent.get(j);
                        if (j == 0 && !original.precedingContent.isEmpty()) {
                            c = (Content) c.withPrefix(original.precedingContent.get(0).getPrefix());
                        } else if (j == 0) {
                            c = (Content) c.withPrefix(original.tag.getPrefix());
                        }
                        newContent.add(c);
                    }

                    Xml.Tag reorderedTag = reordered.tag;
                    if (reordered.precedingContent.isEmpty()) {
                        // Apply prefix from the original group's first element
                        if (!original.precedingContent.isEmpty()) {
                            reorderedTag = reorderedTag.withPrefix(original.precedingContent.get(0).getPrefix());
                        } else {
                            reorderedTag = reorderedTag.withPrefix(original.tag.getPrefix());
                        }
                    }
                    newContent.add(reorderedTag);
                }

                // Append any trailing non-tag content
                newContent.addAll(currentComments);

                return t.withContent(newContent);
            }

            private boolean isImportedBom(Xml.Tag tag) {
                return matches(tag, "type", "pom") && matches(tag, "scope", "import");
            }

            private boolean matches(Xml.Tag tag, String childName, String expected) {
                String value = tag.getChildValue(childName).orElse(null);
                if (value == null) {
                    return false;
                }
                String resolved = getResolutionResult().getPom().getValue(value);
                // A placeholder that the effective model can't resolve, such as one defined only in an
                // inactive profile, might still be an import; leave those dependencies where they are.
                return resolved == null || ResolvedPom.placeholderHelper.hasPlaceholders(resolved) ||
                       expected.equals(resolved.trim());
            }
        };
    }

    // Compared only as a whole key; `:` cannot occur in a groupId or artifactId
    private static String groupArtifactSortKey(Xml.Tag dependency) {
        return dependency.getChildValue("groupId").orElse("") + ":" +
               dependency.getChildValue("artifactId").orElse("");
    }

    private static class DependencyGroup {
        final Xml.Tag tag;
        final List<Content> precedingContent;
        final boolean importedBom;

        DependencyGroup(Xml.Tag tag, List<Content> precedingContent, boolean importedBom) {
            this.tag = tag;
            this.precedingContent = precedingContent;
            this.importedBom = importedBom;
        }
    }
}
