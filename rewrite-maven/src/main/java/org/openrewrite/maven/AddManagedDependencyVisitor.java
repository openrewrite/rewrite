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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

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

                doAfterVisit(new AddToTagVisitor<>(tag, dependencyTag,
                        new InsertDependencyComparator(tag.getContent() == null ? emptyList() : tag.getContent(), dependencyTag)));

                // If because is provided, add a visitor to prepend the comment before the dependency
                if (because != null) {
                    doAfterVisit(new AddCommentBeforeDependency(groupId, artifactId, because));
                }
            }
            return super.

                    visitTag(tag, ctx);
        }
    }

    @RequiredArgsConstructor
    private static class AddCommentBeforeDependency extends MavenIsoVisitor<ExecutionContext> {
        private final String groupId;
        private final String artifactId;
        private final String because;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (MANAGED_DEPENDENCIES_MATCHER.matches(getCursor()) && tag.getContent() != null) {
                List<Content> contents = new ArrayList<>(tag.getContent());

                return IntStream.range(0, contents.size())
                        .filter(i -> contents.get(i) instanceof Xml.Tag)
                        .mapToObj(i -> new IndexedTag(i, (Xml.Tag) contents.get(i)))
                        .filter(pair -> matchesDependency(pair.tag, groupId, artifactId))
                        .findFirst()
                        .map(pair -> addComment(tag, contents, pair))
                        .orElse(tag);
            }
            return super.visitTag(tag, ctx);
        }

        private Xml.Tag addComment(Xml.Tag tag, List<Content> contents, IndexedTag pair) {
            // Extract the prefix (should be like "\n            ")
            String prefix = pair.tag.getPrefix();

            // Create comment with the same prefix as the dependency tag
            // Add spaces around the comment text for proper XML formatting
            Xml.Comment comment = new Xml.Comment(
                    Tree.randomId(),
                    prefix,
                    Markers.EMPTY,
                    " " + because + " "
            );

            // Insert comment before the dependency
            contents.add(pair.index, comment);

            // Update the dependency to have the same prefix (newline + indentation)
            // so it appears on the next line after the comment
            contents.set(pair.index + 1, pair.tag.withPrefix(prefix));

            return tag.withContent(contents);
        }

        @RequiredArgsConstructor
        private static class IndexedTag {
            final int index;
            final Xml.Tag tag;
        }
    }
}
