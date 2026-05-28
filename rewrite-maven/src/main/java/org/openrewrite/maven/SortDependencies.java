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
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
               "Applies to both `<dependencies>` and `<dependencyManagement>` sections.";
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
                        groups.add(new DependencyGroup((Xml.Tag) content, currentComments));
                        currentComments = new ArrayList<>();
                    } else {
                        currentComments.add(content);
                    }
                }

                // If there are fewer than 2 dependency tags, nothing to sort
                if (groups.size() < 2) {
                    return t;
                }

                List<DependencyGroup> sorted = new ArrayList<>(groups);
                sorted.sort(Comparator.<DependencyGroup, Boolean>comparing(
                    g -> "test".equals(g.tag.getChildValue("scope").orElse(null))
                ).thenComparing(
                    g -> g.tag.getChildValue("groupId").orElse("") + ":" +
                         g.tag.getChildValue("artifactId").orElse("")
                ));

                // Check if order actually changed
                boolean changed = false;
                for (int i = 0; i < groups.size(); i++) {
                    if (groups.get(i).tag != sorted.get(i).tag) {
                        changed = true;
                        break;
                    }
                }

                if (!changed) {
                    return t;
                }

                // Rebuild content preserving original whitespace prefixes
                List<Content> newContent = new ArrayList<>();
                for (int i = 0; i < sorted.size(); i++) {
                    DependencyGroup original = groups.get(i);
                    DependencyGroup reordered = sorted.get(i);

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
        };
    }

    private static class DependencyGroup {
        final Xml.Tag tag;
        final List<Content> precedingContent;

        DependencyGroup(Xml.Tag tag, List<Content> precedingContent) {
            this.tag = tag;
            this.precedingContent = precedingContent;
        }
    }
}
