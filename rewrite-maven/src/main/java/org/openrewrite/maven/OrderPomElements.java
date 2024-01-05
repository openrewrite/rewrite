/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;

import static org.openrewrite.maven.MavenTagInsertionComparator.canonicalOrdering;

public class OrderPomElements extends Recipe {
    @Override
    public String getDisplayName() {
        return "Order POM elements";
    }

    @Override
    public String getDescription() {
        return "Order POM elements according to the [recommended](http://maven.apache.org/developers/conventions/code.html#pom-code-convention) order.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-3423");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document mvn = super.visitDocument(document, ctx);
                Xml.Tag root = mvn.getRoot();
                if (root.getContent() != null) {
                    Map<String, List<Content>> groupedContents = new HashMap<>(root.getContent().size());

                    // Group comments with the next non-comment content.
                    List<Content> groupedContent = new ArrayList<>();

                    // Collect content that does not exist in `REQUIRED_ORDER`.
                    List<Content> otherContent = new ArrayList<>();

                    // Save the first UUID of grouped content and the size of the grouped content.
                    // The group sizes are used to preserve the original layout of new lines.
                    Map<UUID, Integer> groupSizes = new HashMap<>();

                    for (Content content : root.getContent()) {
                        if (content instanceof Xml.Comment) {
                            groupedContent.add(content);
                        } else if (content instanceof Xml.Tag) {
                            groupedContent.add(content);
                            groupedContents.put(((Xml.Tag) content).getName(), groupedContent);

                            groupSizes.put(groupedContent.get(0).getId(), groupedContent.size());
                            groupedContent = new ArrayList<>();
                        } else {
                            groupedContent.add((content));
                            otherContent.addAll(groupedContent);

                            groupSizes.put(groupedContent.get(0).getId(), groupedContent.size());
                            groupedContent = new ArrayList<>();
                        }
                    }

                    List<Content> updatedOrder = new ArrayList<>(root.getContent().size());
                    // Apply required order.
                    for (String order : canonicalOrdering) {
                        if (groupedContents.containsKey(order)) {
                            updatedOrder.addAll(groupedContents.get(order));
                            groupedContents.remove(order);
                        }
                    }

                    // Add remaining tags that may not have been in the `REQUIRED_ORDER` list.
                    for (List<Content> value : groupedContents.values()) {
                        updatedOrder.addAll(value);
                    }

                    // Add non-tag content.
                    updatedOrder.addAll(otherContent);

                    int beforeIndex = 0;
                    int afterIndex = 0;
                    for (int i = 0; i < root.getContent().size() &&
                                    beforeIndex != root.getContent().size() &&
                                    afterIndex != updatedOrder.size(); i++) {

                        Content original = root.getContent().get(beforeIndex);
                        Content updated = updatedOrder.get(afterIndex);

                        updatedOrder.set(afterIndex, (Content) updated.withPrefix(original.getPrefix()));

                        beforeIndex += groupSizes.get(original.getId());
                        afterIndex += groupSizes.get(updated.getId());
                    }

                    boolean foundChange = false;
                    for (int i = 0; i < root.getContent().size(); i++) {
                        if (root.getContent().get(i) != updatedOrder.get(i)) {
                            foundChange = true;
                            break;
                        }
                    }

                    if (foundChange) {
                        root = root.withContent(updatedOrder);
                        mvn = mvn.withRoot(root);
                        mvn = autoFormat(mvn, ctx);
                    }
                }
                return mvn;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag tg = super.visitTag(tag, ctx);
                if ("dependency".equals(tg.getName()) || "parent".equals(tg.getName())) {
                    tg = orderGav(tg);
                }
                return tg;
            }

            private Xml.Tag orderGav(Xml.Tag gavParent) {
                if (gavParent.getContent() != null) {
                    int groupPos = -1;
                    int artifactPos = -1;
                    int versionPos = -1;
                    Map<String, List<Content>> gavGroups = new HashMap<>();
                    List<Content> groupedContent = new ArrayList<>();
                    List<Content> otherContent = new ArrayList<>();
                    for (int i = 0; i < gavParent.getContent().size(); i++) {
                        Content content = gavParent.getContent().get(i);
                        if (content instanceof Xml.Comment) {
                            groupedContent.add(content);
                        } else if (content instanceof Xml.Tag) {
                            Xml.Tag tag = (Xml.Tag) content;
                            if ("groupId".equals(tag.getName())) {
                                groupPos = i;
                            } else if ("artifactId".equals(tag.getName())) {
                                artifactPos = i;
                            } else if ("version".equals(tag.getName())) {
                                versionPos = i;
                            }

                            switch (tag.getName()) {
                                case "groupId":
                                case "artifactId":
                                case "version":
                                    groupedContent.add(tag);
                                    gavGroups.put(tag.getName(), groupedContent);
                                    groupedContent = new ArrayList<>();
                                    break;
                                default:
                                    otherContent.addAll(groupedContent);
                                    otherContent.add(content);
                                    groupedContent = new ArrayList<>();
                                    break;
                            }
                        } else {
                            otherContent.addAll(groupedContent);
                            otherContent.add(content);
                            groupedContent = new ArrayList<>();
                        }
                    }

                    if ((groupPos > artifactPos ||
                         (versionPos > -1 && (artifactPos > versionPos)))) {
                        List<Content> orderedContents = new ArrayList<>();
                        for (String type : new String[]{"groupId", "artifactId", "version"}) {
                            List<Content> gavContents = gavGroups.get(type);
                            if (gavContents != null) {
                                orderedContents.addAll(gavContents);
                            }
                        }
                        orderedContents.addAll(otherContent);
                        gavParent = gavParent.withContent(orderedContents);
                    }
                }
                return gavParent;
            }
        };
    }
}
