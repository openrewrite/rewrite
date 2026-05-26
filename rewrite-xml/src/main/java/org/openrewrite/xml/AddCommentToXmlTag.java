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
package org.openrewrite.xml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.24.0")
@Value
@EqualsAndHashCode(callSuper = false)
public class AddCommentToXmlTag extends Recipe {

    @Option(displayName = "XPath",
            description = "An XPath expression used to find matching tags.",
            example = "/project/dependencies/dependency")
    String xPath;

    @Option(displayName = "Comment text",
            description = "The text to add as a comment..",
            example = "This is excluded due to CVE <X> and will be removed when we upgrade the next version is available.")
    String commentText;

    String displayName = "Add a comment to an XML tag";

    String description = "Adds a comment as the first element in an XML tag.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            final XPathMatcher matcher = new XPathMatcher(xPath);

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);

                // (1) Prepend a sibling comment before any self-closing child tag that matches.
                if (t.getContent() != null) {
                    List<? extends Content> existing = t.getContent();
                    List<Content> updated = null;
                    for (int i = 0; i < existing.size(); i++) {
                        Content c = existing.get(i);
                        if (c instanceof Xml.Tag && ((Xml.Tag) c).getClosing() == null &&
                                matcher.matches(new Cursor(getCursor(), c))) {
                            boolean alreadyHasComment = i > 0 && isMatchingComment(existing.get(i - 1));
                            if (!alreadyHasComment) {
                                if (updated == null) {
                                    updated = new ArrayList<>(existing.subList(0, i));
                                }
                                updated.add(new Xml.Comment(randomId(), c.getPrefix(), Markers.EMPTY, commentText));
                            }
                        }
                        if (updated != null) {
                            updated.add(c);
                        }
                    }
                    if (updated != null) {
                        t = t.withContent(updated);
                    }
                }

                // (2) For non-self-closing matched tags (or a self-closing root with no parent
                // tag to host a sibling), insert the comment as the first child of the tag.
                if (matcher.matches(getCursor()) && (t.getClosing() != null || !hasAncestorTag())) {
                    List<? extends Content> existing = t.getContent();
                    if (existing != null) {
                        for (Content c : existing) {
                            if (isMatchingComment(c)) {
                                return t;
                            }
                        }
                    }
                    String prefix = (existing == null || existing.isEmpty()) ? "" : existing.get(0).getPrefix();
                    Xml.Comment comment = new Xml.Comment(randomId(), prefix, Markers.EMPTY, commentText);
                    List<Content> contents;
                    if (existing == null || existing.isEmpty()) {
                        contents = new ArrayList<>();
                        contents.add(comment);
                    } else {
                        contents = new ArrayList<>(existing.size() + 1);
                        contents.add(comment);
                        contents.addAll(existing);
                    }
                    t = t.withContent(contents);
                }

                return t;
            }

            private boolean isMatchingComment(Content c) {
                return c instanceof Xml.Comment && commentText.equals(((Xml.Comment) c).getText());
            }

            private boolean hasAncestorTag() {
                for (Cursor c = getCursor().getParent(); c != null; c = c.getParent()) {
                    if (c.getValue() instanceof Xml.Tag) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
