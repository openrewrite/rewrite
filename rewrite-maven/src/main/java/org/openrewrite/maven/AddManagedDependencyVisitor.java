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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class AddManagedDependencyVisitor extends MavenIsoVisitor<ExecutionContext> {
    private static final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");

    private final String groupId;
    private final String artifactId;
    private final String version;

    @Nullable
    private final String scope;

    @Nullable
    private final String type;

    @Nullable
    private final String classifier;

    @Nullable
    private final String because;

    @Override
    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
        Xml.Document doc = super.visitDocument(document, ctx);

        if (documentHasManagedDependency(doc, ctx)) {
            return document;
        }

        Xml.Tag root = document.getRoot();
        List<? extends Content> rootContent = root.getContent() != null ? root.getContent() : emptyList();

        Xml.Tag dependencyManagementTag = root.getChild("dependencyManagement").orElse(null);
        if (dependencyManagementTag == null) {
            doc = (Xml.Document) new AddToTagVisitor<>(root, Xml.Tag.build("<dependencyManagement>\n<dependencies/>\n</dependencyManagement>"),
                    new MavenTagInsertionComparator(rootContent)).visitNonNull(doc, ctx);
        } else if (!dependencyManagementTag.getChild("dependencies").isPresent()) {
            doc = (Xml.Document) new AddToTagVisitor<>(dependencyManagementTag, Xml.Tag.build("\n<dependencies/>\n"),
                    new MavenTagInsertionComparator(rootContent)).visitNonNull(doc, ctx);
        }

        return (Xml.Document) new InsertDependencyInOrder(groupId, artifactId, version,
                type, scope, classifier, because).visitNonNull(doc, ctx);
    }

    private boolean documentHasManagedDependency(Xml.Document doc, ExecutionContext ctx) {
        AtomicBoolean managedDepExists = new AtomicBoolean(false);
        new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                Xml.Tag tg = super.visitTag(tag, executionContext);
                if (isManagedDependencyTag(groupId, artifactId)) {
                    managedDepExists.set(true);
                }
                return tg;
            }
        }.visitNonNull(doc, ctx);
        return managedDepExists.get();
    }

    private static boolean matchesDependency(Xml.Tag dependencyTag, String groupId, String artifactId) {
        return "dependency".equals(dependencyTag.getName()) &&
                groupId.equals(dependencyTag.getChildValue("groupId").orElse(null)) &&
                artifactId.equals(dependencyTag.getChildValue("artifactId").orElse(null));
    }

    @RequiredArgsConstructor
    private static class InsertDependencyInOrder extends MavenIsoVisitor<ExecutionContext> {
        private final String groupId;
        private final String artifactId;
        private final String version;

        @Nullable
        private final String type;

        @Nullable
        private final String scope;

        @Nullable
        private final String classifier;

        @Nullable
        private final String because;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (MANAGED_DEPENDENCIES_MATCHER.matches(getCursor())) {
                for (Xml.Tag dependency : tag.getChildren()) {
                    if (matchesDependency(dependency, groupId, artifactId)) {
                        return tag;
                    }
                }
                Xml.Tag dependencyTag = Xml.Tag.build(
                        "\n<dependency>\n" +
                        "<groupId>" + groupId + "</groupId>\n" +
                        "<artifactId>" + artifactId + "</artifactId>\n" +
                        "<version>" + version + "</version>\n" +
                        (classifier == null ? "" :
                                "<classifier>" + classifier + "</classifier>\n") +
                        (type == null || "jar".equals(type) ? "" :
                                "<type>" + type + "</type>\n") +
                        (scope == null || "compile".equals(scope) ? "" :
                                "<scope>" + scope + "</scope>\n") +
                        "</dependency>"
                );

                // Add the dependency with optional comment in a single operation to ensure
                // the comment stays with its associated dependency regardless of sorting order
                doAfterVisit(new AddDependencyWithCommentVisitor(tag, dependencyTag, because,
                        new InsertDependencyComparator(tag.getContent() == null ? emptyList() : tag.getContent(), dependencyTag)));
            }
            return super.visitTag(tag, ctx);
        }
    }

    @RequiredArgsConstructor
    private static class AddDependencyWithCommentVisitor extends MavenIsoVisitor<ExecutionContext> {
        private final Xml.Tag scope;
        private final Xml.Tag dependencyTag;
        @Nullable
        private final String because;
        private final Comparator<Content> tagComparator;

        @Override
        public Xml.Tag visitTag(Xml.Tag t, ExecutionContext ctx) {
            if (scope.isScope(t)) {
                t = ensureClosingTag(t, ctx);
                Xml.Tag formattedDependencyTag = formatDependencyTag(ctx);

                List<Content> content = t.getContent() == null ? new ArrayList<>() : new ArrayList<>(t.getContent());
                int insertIndex = findInsertIndex(content);

                if (because != null) {
                    addComment(content, insertIndex, because, formattedDependencyTag.getPrefix());
                    addDependency(content, insertIndex + 1, formattedDependencyTag);
                } else {
                    addDependency(content, insertIndex, formattedDependencyTag);
                }

                t = t.withContent(content);
            }

            return super.visitTag(t, ctx);
        }

        /**
         * Ensures the tag has a proper closing tag (not self-closing) with a newline prefix.
         */
        private Xml.Tag ensureClosingTag(Xml.Tag t, ExecutionContext ctx) {
            if (t.getClosing() == null) {
                t = t.withClosing(autoFormat(new Xml.Tag.Closing(Tree.randomId(), "\n",
                                Markers.EMPTY, t.getName(), ""), null, ctx, getCursor()))
                        .withBeforeTagDelimiterPrefix("");
            }
            if (!t.getClosing().getPrefix().contains("\n")) {
                t = t.withClosing(t.getClosing().withPrefix("\n"));
            }
            return t;
        }

        /**
         * Formats the dependency tag with proper newline prefix and indentation.
         */
        private Xml.Tag formatDependencyTag(ExecutionContext ctx) {
            Xml.Tag formatted = dependencyTag;
            if (!formatted.getPrefix().contains("\n")) {
                formatted = formatted.withPrefix("\n");
            }
            return autoFormat(formatted, null, ctx, getCursor());
        }

        /**
         * Find the insertion position by comparing only with dependency tags.
         * This ensures we don't insert between a comment and its associated dependency.
         */
        private int findInsertIndex(List<Content> content) {
            for (int i = 0; i < content.size(); i++) {
                Content item = content.get(i);
                // Only compare with dependency tags, skip comments
                if (item instanceof Xml.Tag && tagComparator.compare(item, dependencyTag) > 0) {
                    // Found a dependency that should come after ours.
                    // We want to insert before this dependency AND any preceding comments.
                    return findInsertPositionBeforePrecedingComments(content, i);
                }
            }
            return content.size();
        }

        private void addComment(List<Content> content, int insertIndex, String because, String prefix) {
            Xml.Comment comment = new Xml.Comment(
                    Tree.randomId(),
                    prefix,
                    Markers.EMPTY,
                    " " + because + " "
            );
            content.add(insertIndex, comment);
        }

        private void addDependency(List<Content> content, int insertIndex, Xml.Tag dependencyTag) {
            content.add(insertIndex, dependencyTag);
        }

        /**
         * Given an index of a dependency tag, find the insertion position that's before
         * any preceding comments that belong to that dependency.
         */
        private int findInsertPositionBeforePrecedingComments(List<Content> content, int tagIndex) {
            int insertPos = tagIndex;
            // Walk backwards from the tag, skipping any immediately preceding comments
            while (insertPos > 0 && content.get(insertPos - 1) instanceof Xml.Comment) {
                insertPos--;
            }
            return insertPos;
        }
    }
}
