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
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.*;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;

public class OrderPomElements extends Recipe {
    private static final List<String> REQUIRED_ORDER = Arrays.asList(
            "modelVersion",
            "parent",
            "groupId",
            "artifactId",
            "version",
            "packaging",
            "name",
            "description",
            "url",
            "inceptionYear",
            "organization",
            "licenses",
            "developers",
            "contributors",
            "mailingLists",
            "prerequisites",
            "modules",
            "scm",
            "issueManagement",
            "ciManagement",
            "distributionManagement",
            "properties",
            "dependencyManagement",
            "dependencies",
            "repositories",
            "pluginRepositories",
            "build",
            "reporting",
            "profiles");

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
    protected MavenVisitor getVisitor() {
        return new MavenVisitor() {
            @Override
            public Maven visitMaven(Maven maven, ExecutionContext ctx) {
                Maven mvn = super.visitMaven(maven, ctx);
                Xml.Tag root = mvn.getRoot();
                if (root.getContent() != null) {
                    List<Content> previousContent = new ArrayList<>();
                    List<Content> otherContent = new ArrayList<>();
                    Map<String, List<Content>> groupedContents = new HashMap<>();
                    for (Content content : root.getContent()) {
                        if (content instanceof Xml.Comment) {
                            previousContent.add(content);
                        } else if (content instanceof Xml.Tag) {
                            previousContent.add(content);
                            groupedContents.put(((Xml.Tag) content).getName(), previousContent);
                            previousContent = new ArrayList<>();
                        } else {
                            previousContent.add((content));
                            otherContent.addAll(previousContent);
                            previousContent = new ArrayList<>();
                        }
                    }

                    List<Content> updatedOrder = new ArrayList<>();
                    for (String order : REQUIRED_ORDER) {
                        if (groupedContents.containsKey(order)) {
                            updatedOrder.addAll(groupedContents.get(order));
                            groupedContents.remove(order);
                        }
                    }

                    for (List<Content> value : groupedContents.values()) {
                        updatedOrder.addAll(value);
                    }

                    updatedOrder.addAll(otherContent);

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
                        mvn = (Maven) new AutoFormatVisitor<>().visitNonNull(mvn, ctx);
                    }
                }
                return mvn;
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                Xml.Tag tg = (Xml.Tag) super.visitTag(tag, executionContext);
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
                    List<Content> prevContent = new ArrayList<>();
                    List<Content> otherContent = new ArrayList<>();
                    for (int i = 0; i < gavParent.getContent().size(); i++) {
                        Content content = gavParent.getContent().get(i);
                        if (content instanceof Xml.Comment) {
                            prevContent.add(content);
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
                                    prevContent.add(tag);
                                    gavGroups.put(tag.getName(), prevContent);
                                    prevContent = new ArrayList<>();
                                    break;
                                default:
                                    otherContent.addAll(prevContent);
                                    otherContent.add(content);
                                    prevContent = new ArrayList<>();
                                    break;
                            }
                        } else {
                            otherContent.addAll(prevContent);
                            otherContent.add(content);
                            prevContent = new ArrayList<>();
                        }
                    }

                    if ((groupPos > artifactPos ||
                            (versionPos > -1 && (artifactPos > versionPos)))) {
                        List<Content> orderedContents = new ArrayList<>();
                        for (String type : new String[]{"groupId", "artifactId", "version"}) {
                            List<Content> gavContents = gavGroups.get(type);
                            if(gavContents != null) {
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
