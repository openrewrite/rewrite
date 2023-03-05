/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.cleanup;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

public class RemoveCompileScope extends Recipe {

    @Override
    public String getDisplayName() {
        return "Removes the compile scope";
    }

    @Override
    public String getDescription() {
        return "Removes the compile scope for all the dependencies as it is the default scope,and it is redundant.";
    }

    @Override
    public List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        // First we collect all the dependencies under dependencyManagement
        // We should not remove the compile scope for dependencies declared with a different scope within dependencyManagement

        final Map<String, String> seen = new HashMap<>();

        for (SourceFile entry : before) {

            List<? extends Content> content = ((Xml.Document) entry).getRoot().getContent();

            if (content == null) {
                continue;
            }

            for (Content pomTags : content) {

                Xml.Tag xmlTag = ((Xml.Tag) pomTags);

                if ("dependencyManagement".equalsIgnoreCase(xmlTag.getName())) {

                    List<? extends Content> xmlContent = xmlTag.getContent();

                    if (xmlContent == null) {
                        continue;
                    }

                    for (Content dependency : xmlContent) {
                        List<? extends Content> dependencyContent = ((Xml.Tag) dependency).getContent();

                        if (dependencyContent == null) {
                            continue;
                        }

                        for (Content dependencyEntry : dependencyContent) {
                            Xml.Tag ee = (Xml.Tag) dependencyEntry;

                            String groupId = "";
                            String artifactId = "";
                            String scope = "";

                            List<? extends Content> dependencyEntryContent = ee.getContent();

                            if (dependencyEntryContent == null) {
                                continue;
                            }

                            for (Content dep : dependencyEntryContent) {

                                Xml.Tag tag = ((Xml.Tag) dep);

                                if (tag.getName().equalsIgnoreCase("artifactId")) {
                                    artifactId = tag.getValue().orElse(null);
                                } else if (tag.getName().equalsIgnoreCase("groupId")) {
                                    groupId = tag.getValue().orElse(null);
                                } else if (tag.getName().equalsIgnoreCase("scope")) {
                                    scope = tag.getValue().orElse(null);
                                }
                            }

                            if (groupId != null && artifactId != null && scope != null) {
                                seen.put(deriveDependencyCoordinates(groupId, artifactId), scope);
                            }
                        }

                    }
                }
            }
        }

        return ListUtils.map(before, s -> (SourceFile) new RemoveCompileScopeVisitor(seen).visit(s, ctx));
    }

    @NotNull
    private static String deriveDependencyCoordinates(String groupId, String artifactId) {
        return String.format("%s:%s", groupId, artifactId);
    }

    private static class RemoveCompileScopeVisitor extends MavenVisitor<ExecutionContext> {
        private final Map<String, String> seen;

        private RemoveCompileScopeVisitor(Map<String, String> seen) {
            this.seen = seen;
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {

            if (isDependencyTag()) {
                String groupId = getGroupId(tag);
                String artifactId = getArtifactId(tag);
                String scope = getScope(tag);

                if (groupId != null && artifactId != null && scope != null) {

                    String seenScope = seen.get(deriveDependencyCoordinates(groupId, artifactId));

                    if ("compile".equalsIgnoreCase(seenScope) || seenScope == null && "compile".equals(scope)) {
                        doAfterVisit(new RemoveContentVisitor<>(tag.getChild("scope").get(), false));
                    }
                }

            }

            return super.visitTag(tag, ctx);
        }

        @Nullable
        private String getScope(Xml.Tag tag) {
            return tag.getChild("scope").flatMap(Xml.Tag::getValue).orElse(null);
        }

        @Nullable
        private String getArtifactId(Xml.Tag tag) {
            return tag.getChild("artifactId").flatMap(Xml.Tag::getValue).orElse(null);
        }

        @Nullable
        private String getGroupId(Xml.Tag tag) {
            return tag.getChild("groupId").flatMap(Xml.Tag::getValue).orElse(null);
        }
    }
}
