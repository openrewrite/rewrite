/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.xml.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.CommentService;
import org.openrewrite.marker.Markers;
import org.openrewrite.trait.Comments.Placement;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

/**
 * {@link CommentService} for the XML LST model. Unlike Java/JSON/YAML, an XML comment is a real
 * {@link Xml.Comment} node rather than prefix metadata. The {@code multiline} flag is ignored, since
 * XML comments are inherently block comments.
 * <p>
 * Placement determines where the comment goes and what owner is returned:
 * <ul>
 *     <li>{@code BEFORE} — a sibling that immediately precedes the element within the parent's
 *     content. The service walks up to the parent {@link Xml.Tag}, inspects the run of comment
 *     siblings directly before the element, and returns the modified <em>parent</em>. If the element
 *     has no parent tag (e.g. the document root), there is nowhere to host a preceding sibling and the
 *     element is returned unchanged.</li>
 *     <li>{@code FIRST_CHILD} — the first child of the tag at the cursor; the modified tag is
 *     returned. Useful for "comment inside this element" recipes.</li>
 * </ul>
 */
@Incubating(since = "8.86.0")
public class XmlCommentService extends CommentService {

    @Override
    public List<String> getComments(Cursor cursor) {
        Xml.Tag parent = parentTag(cursor);
        List<? extends Content> content = parent == null ? null : parent.getContent();
        if (content == null) {
            return emptyList();
        }
        int idx = indexOf(content, cursor.getValue());
        if (idx < 0) {
            return emptyList();
        }
        List<String> texts = new ArrayList<>();
        for (int i = runStart(content, idx); i < idx; i++) {
            texts.add(((Xml.Comment) content.get(i)).getText());
        }
        return texts;
    }

    @Override
    public Tree addComment(Cursor cursor, String text, boolean multiline, Placement placement) {
        if (placement == Placement.FIRST_CHILD) {
            return addAsFirstChild(cursor, text);
        }
        return addBefore(cursor, text);
    }

    private Tree addBefore(Cursor cursor, String text) {
        Xml.Tag parent = parentTag(cursor);
        if (parent == null) {
            return cursor.getValue();
        }
        if (hasEquivalentComment(cursor, text)) {
            return parent;
        }
        List<? extends Content> content = parent.getContent();
        int idx = content == null ? -1 : indexOf(content, cursor.getValue());
        if (idx < 0) {
            return parent;
        }
        Content element = content.get(idx);
        Xml.Comment comment = new Xml.Comment(randomId(), element.getPrefix(), Markers.EMPTY, text);
        List<Content> newContent = new ArrayList<>(content);
        newContent.add(idx, comment);
        return parent.withContent(newContent);
    }

    /**
     * Insert the comment as the first child of the tag at the cursor, deduping against the tag's own
     * existing child comments.
     */
    private Tree addAsFirstChild(Cursor cursor, String text) {
        Object value = cursor.getValue();
        if (!(value instanceof Xml.Tag)) {
            return (Tree) value;
        }
        Xml.Tag tag = (Xml.Tag) value;
        List<? extends Content> content = tag.getContent();
        if (content != null) {
            for (Content c : content) {
                if (c instanceof Xml.Comment && equivalent(text, ((Xml.Comment) c).getText())) {
                    return tag;
                }
            }
        }
        String prefix = content == null || content.isEmpty() ? "" : content.get(0).getPrefix();
        Xml.Comment comment = new Xml.Comment(randomId(), prefix, Markers.EMPTY, text);
        List<Content> newContent = content == null ? new ArrayList<>() : new ArrayList<>(content);
        newContent.add(0, comment);
        return tag.withContent(newContent);
    }

    @Override
    public Tree removeComment(Cursor cursor, String text) {
        Xml.Tag parent = parentTag(cursor);
        if (parent == null) {
            return cursor.getValue();
        }
        if (!hasComment(cursor, text)) {
            return parent;
        }
        List<? extends Content> content = parent.getContent();
        int idx = indexOf(content, cursor.getValue());
        int runStart = runStart(content, idx);
        List<Content> newContent = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            Content c = content.get(i);
            if (i >= runStart && i < idx && text.equals(((Xml.Comment) c).getText())) {
                continue;
            }
            newContent.add(c);
        }
        return parent.withContent(newContent);
    }

    @Override
    public Tree replaceComment(Cursor cursor, String existingText, String newText) {
        Xml.Tag parent = parentTag(cursor);
        if (parent == null) {
            return cursor.getValue();
        }
        if (existingText.equals(newText) || !hasComment(cursor, existingText)) {
            return parent;
        }
        List<? extends Content> content = parent.getContent();
        int idx = indexOf(content, cursor.getValue());
        int runStart = runStart(content, idx);
        List<Content> newContent = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            Content c = content.get(i);
            if (i >= runStart && i < idx && existingText.equals(((Xml.Comment) c).getText())) {
                newContent.add(((Xml.Comment) c).withText(newText));
            } else {
                newContent.add(c);
            }
        }
        return parent.withContent(newContent);
    }

    private static Xml.@Nullable Tag parentTag(Cursor cursor) {
        Cursor parent = cursor.getParentTreeCursor();
        return parent.getValue() instanceof Xml.Tag ? parent.getValue() : null;
    }

    /**
     * @return The index of {@code element} within {@code content} by reference identity, or -1.
     */
    private static int indexOf(List<? extends Content> content, Object element) {
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i) == element) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return The index at which the contiguous run of {@link Xml.Comment} siblings immediately
     * preceding {@code idx} begins (equal to {@code idx} when there are none).
     */
    private static int runStart(List<? extends Content> content, int idx) {
        int i = idx;
        while (i - 1 >= 0 && content.get(i - 1) instanceof Xml.Comment) {
            i--;
        }
        return i;
    }
}
