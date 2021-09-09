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
import org.openrewrite.xml.AutoFormat;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

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
    protected MavenVisitor getVisitor() {
        return new MavenVisitor() {
            @Override
            public Maven visitMaven(Maven maven, ExecutionContext ctx) {
                Maven mvn = super.visitMaven(maven, ctx);
                mvn = (Maven) new OrderVisitor().visit(mvn, ctx);
                assert mvn != null;
                return mvn;
            }
        };
    }

    private static class OrderVisitor extends XmlVisitor<ExecutionContext> {
        @Override
        public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
            Xml.Document doc = (Xml.Document) super.visitDocument(document, executionContext);
            Xml.Tag root = doc.getRoot();
            if (root.getContent() != null) {
                List<Content> content = new ArrayList<>(root.getContent());
                List<Content> updatedOrder = new ArrayList<>();
                for (String order : REQUIRED_ORDER) {
                    for (Iterator<? extends Content> iterator = content.iterator(); iterator.hasNext(); ) {
                        Content c = iterator.next();
                        if (c instanceof Xml.Tag) {
                            Xml.Tag tag = (Xml.Tag) c;
                            if (tag.getName().equals(order)) {
                                updatedOrder.add(c);
                                iterator.remove();
                                break;
                            }
                        }
                    }
                }
                updatedOrder.addAll(content);
                boolean foundChange = false;
                for (int i = 0; i < root.getContent().size(); i++) {
                    if (root.getContent().get(i) != updatedOrder.get(i)) {
                        foundChange = true;
                        break;
                    }
                }

                if (foundChange) {
                    root = root.withContent(updatedOrder);
                    doc = doc.withRoot(root);
                    doAfterVisit(new AutoFormat());
                }
            }
            return doc;
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
            List<? extends Content> gavParentContent = gavParent.getChildren();
            int groupPos = -1;
            int artifactPos = -1;
            int versionPos = -1;
            Map<String, Content> gavGroups = new HashMap<>();
            for (int i = 0; i < gavParentContent.size(); i++) {
                Content content = gavParentContent.get(i);
                Xml.Tag tag = (Xml.Tag) content;
                if ("groupId".equals(tag.getName())) {
                    gavGroups.put("group", tag);
                    groupPos = i;
                } else if ("artifactId".equals(tag.getName())) {
                    gavGroups.put("artifact", tag);
                    artifactPos = i;
                } else if ("version".equals(tag.getName())) {
                    gavGroups.put("version", tag);
                    versionPos = i;
                }
            }
            if ((groupPos > artifactPos ||
                    (versionPos > -1 && (artifactPos > versionPos)))) {
                List<Content> orderedContents = new ArrayList<>();
                for (String type : new String[]{"group", "artifact", "version"}) {
                    Content gavContent = gavGroups.get(type);
                    if(gavContent != null) {
                        orderedContents.add(gavContent);
                    }
                }
                gavParent = gavParent.withContent(orderedContents);
            }
            return gavParent;
        }
    }
}
