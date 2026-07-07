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
package org.openrewrite.json.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.CommentService;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.json.tree.Comment;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.trait.Comments.Placement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * {@link CommentService} for the JSON LST model, which (like {@code J}) keeps comments in the
 * {@link Space} prefix of a node.
 */
@Incubating(since = "8.86.0")
public class JsonCommentService extends CommentService {

    private static final Pattern NEWLINE = Pattern.compile("\\R");

    @Override
    public List<String> getComments(Cursor cursor) {
        Object tree = cursor.getValue();
        if (!(tree instanceof Json)) {
            return emptyList();
        }
        List<String> texts = new ArrayList<>();
        for (Comment comment : ((Json) tree).getPrefix().getComments()) {
            texts.add(comment.getText());
        }
        return texts;
    }

    @Override
    public Tree addComment(Cursor cursor, String text, boolean multiline, Placement placement) {
        return addComment(cursor, text, multiline, placement, null);
    }

    @Override
    public Tree addComment(Cursor cursor, String text, boolean multiline, Placement placement, @Nullable String suffix) {
        // JSON keeps comments in the prefix, so FIRST_CHILD behaves as BEFORE.
        Tree tree = cursor.getValue();
        if (!(tree instanceof Json) || hasEquivalentComment(cursor, text)) {
            return tree;
        }
        Json json = (Json) tree;
        Space prefix = json.getPrefix();
        String indent;
        if (suffix != null) {
            indent = suffix;
        } else {
            indent = prefix.getWhitespace();
        }
        String commentText = multiline ? text : NEWLINE.matcher(text).replaceAll(" ");

        Comment comment = new Comment(multiline, commentText, indent, Markers.EMPTY);
        return json.withPrefix(prefix.withComments(ListUtils.concat(prefix.getComments(), comment)));
    }

    @Override
    public Tree removeComment(Cursor cursor, String text) {
        Tree tree = cursor.getValue();
        if (!(tree instanceof Json) || !hasComment(cursor, text)) {
            return tree;
        }
        Json json = (Json) tree;
        Space prefix = json.getPrefix();
        //noinspection DataFlowIssue
        return json.withPrefix(prefix.withComments(ListUtils.map(prefix.getComments(), c ->
                text.equals(c.getText()) ? null : c)));
    }

    @Override
    public Tree replaceComment(Cursor cursor, String existingText, String newText) {
        Tree tree = cursor.getValue();
        if (!(tree instanceof Json) || existingText.equals(newText) || !hasComment(cursor, existingText)) {
            return tree;
        }
        Json json = (Json) tree;
        Space prefix = json.getPrefix();
        return json.withPrefix(prefix.withComments(ListUtils.map(prefix.getComments(), c ->
                existingText.equals(c.getText()) ? c.withText(newText) : c)));
    }
}
