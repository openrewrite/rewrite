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
package org.openrewrite.properties.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.CommentService;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.trait.Comments.Placement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

/**
 * {@link CommentService} for the {@code .properties} LST model. Like XML, a properties comment is a
 * real {@link Properties.Comment} sibling node within the {@link Properties.File} content rather than
 * prefix metadata, so the service walks up to the file and edits the run of comment siblings that
 * immediately precede the element. The {@code multiline} flag and any explicit suffix are ignored,
 * since a properties comment is always a single {@code #} line.
 */
@Incubating(since = "8.86.0")
public class PropertiesCommentService extends CommentService {

    private static final Pattern NEWLINE = Pattern.compile("\\R");

    @Override
    public List<String> getComments(Cursor cursor) {
        Properties.File file = parentFile(cursor);
        List<Properties.Content> content = file == null ? null : file.getContent();
        if (content == null) {
            return emptyList();
        }
        int idx = indexOf(content, cursor.getValue());
        if (idx < 0) {
            return emptyList();
        }
        List<String> texts = new ArrayList<>();
        for (int i = runStart(content, idx); i < idx; i++) {
            texts.add(((Properties.Comment) content.get(i)).getMessage());
        }
        return texts;
    }

    @Override
    public Tree addComment(Cursor cursor, String text, boolean multiline, Placement placement) {
        // Properties comments are flat siblings of the file, so FIRST_CHILD behaves as BEFORE.
        Properties.File file = parentFile(cursor);
        if (file == null) {
            return (Tree) cursor.getValue();
        }
        if (hasEquivalentComment(cursor, text)) {
            return file;
        }
        List<Properties.Content> content = file.getContent();
        int idx = indexOf(content, cursor.getValue());
        if (idx < 0) {
            return file;
        }
        Properties.Content element = content.get(idx);
        // The first line of a file carries no leading newline; every later line is separated from the
        // previous one by the newline rendered as its own prefix.
        String commentPrefix = idx == 0 ? "" : "\n";
        Properties.Comment comment = new Properties.Comment(randomId(), commentPrefix, Markers.EMPTY,
                Properties.Comment.Delimiter.HASH_TAG, NEWLINE.matcher(text).replaceAll(" "));
        // Push the element onto the line below the comment when it shared the comment's line.
        Properties.Content newElement = element.getPrefix().contains("\n") ?
                element : (Properties.Content) element.withPrefix("\n" + element.getPrefix());
        List<Properties.Content> newContent = new ArrayList<>(content);
        newContent.set(idx, newElement);
        newContent.add(idx, comment);
        return file.withContent(newContent);
    }

    @Override
    public Tree removeComment(Cursor cursor, String text) {
        Properties.File file = parentFile(cursor);
        if (file == null) {
            return (Tree) cursor.getValue();
        }
        if (!hasComment(cursor, text)) {
            return file;
        }
        List<Properties.Content> content = file.getContent();
        int idx = indexOf(content, cursor.getValue());
        int runStart = runStart(content, idx);
        List<Properties.Content> newContent = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            Properties.Content c = content.get(i);
            if (i >= runStart && i < idx && text.equals(((Properties.Comment) c).getMessage())) {
                continue;
            }
            newContent.add(c);
        }
        return file.withContent(newContent);
    }

    @Override
    public Tree replaceComment(Cursor cursor, String existingText, String newText) {
        Properties.File file = parentFile(cursor);
        if (file == null) {
            return (Tree) cursor.getValue();
        }
        if (existingText.equals(newText) || !hasComment(cursor, existingText)) {
            return file;
        }
        List<Properties.Content> content = file.getContent();
        int idx = indexOf(content, cursor.getValue());
        int runStart = runStart(content, idx);
        List<Properties.Content> newContent = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            Properties.Content c = content.get(i);
            if (i >= runStart && i < idx && existingText.equals(((Properties.Comment) c).getMessage())) {
                newContent.add(((Properties.Comment) c).withMessage(newText));
            } else {
                newContent.add(c);
            }
        }
        return file.withContent(newContent);
    }

    private static Properties.@Nullable File parentFile(Cursor cursor) {
        Cursor parent = cursor.getParentTreeCursor();
        return parent.getValue() instanceof Properties.File ? parent.getValue() : null;
    }

    /**
     * @return The index of {@code element} within {@code content} by reference identity, or -1.
     */
    private static int indexOf(List<Properties.Content> content, Object element) {
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i) == element) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return The index at which the contiguous run of {@link Properties.Comment} siblings immediately
     * preceding {@code idx} begins (equal to {@code idx} when there are none).
     */
    private static int runStart(List<Properties.Content> content, int idx) {
        int i = idx;
        while (i - 1 >= 0 && content.get(i - 1) instanceof Properties.Comment) {
            i--;
        }
        return i;
    }
}
