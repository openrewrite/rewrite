/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ruby.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.CommentService;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.tree.RubyTextComment;
import org.openrewrite.trait.Comments.Placement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * Ruby comments are {@link RubyTextComment}s in an element's prefix, so the Java implementation —
 * which only sees {@code org.openrewrite.java.tree.TextComment} and would print {@code //}
 * delimiters — cannot serve them. Text is the comment body without its delimiters, which for a
 * {@code =begin}/{@code =end} block excludes the newlines that keep them on lines of their own.
 * <p>
 * A {@code =begin} block is only a comment when it starts a line at column 0, so adding one to an
 * indented element needs an explicit {@code suffix} from the caller.
 */
@Incubating(since = "8.86.0")
public class RubyCommentService extends CommentService {

    private static final Pattern NEWLINE = Pattern.compile("\\R");

    @Override
    public List<String> getComments(Cursor cursor) {
        Object tree = cursor.getValue();
        if (!(tree instanceof J)) {
            return emptyList();
        }
        List<String> texts = new ArrayList<>();
        for (Comment comment : ((J) tree).getComments()) {
            if (comment instanceof RubyTextComment) {
                texts.add(body((RubyTextComment) comment));
            }
        }
        return texts;
    }

    @Override
    public Tree addComment(Cursor cursor, String text, boolean multiline, Placement placement) {
        return addComment(cursor, text, multiline, placement, null);
    }

    @Override
    public Tree addComment(Cursor cursor, String text, boolean multiline, Placement placement, @Nullable String suffix) {
        // Ruby keeps comments in the prefix, so FIRST_CHILD behaves as BEFORE.
        Tree tree = cursor.getValue();
        if (!(tree instanceof J) || hasEquivalentComment(cursor, text)) {
            return tree;
        }
        J j = (J) tree;
        // The whitespace rendered between the comment and the element it precedes; reusing the
        // element's own indentation lands the comment on the line above it. A Ruby comment runs to
        // the end of its line, so the separator has to carry a newline whatever the caller asked
        // for — an element at the start of a file has no leading whitespace of its own.
        String indent = suffix != null ? suffix : j.getPrefix().getWhitespace();
        if (!indent.contains("\n")) {
            indent = "\n" + indent;
        }
        String commentText = multiline ? "\n" + text + "\n" : NEWLINE.matcher(text).replaceAll(" ");
        return j.withComments(ListUtils.concat(j.getComments(),
                new RubyTextComment(multiline, commentText, indent, Markers.EMPTY)));
    }

    @Override
    public Tree removeComment(Cursor cursor, String text) {
        Tree tree = cursor.getValue();
        if (!(tree instanceof J) || !hasComment(cursor, text)) {
            return tree;
        }
        //noinspection DataFlowIssue
        return ((J) tree).withComments(ListUtils.map(((J) tree).getComments(), c ->
                c instanceof RubyTextComment && text.equals(body((RubyTextComment) c)) ? null : c));
    }

    @Override
    public Tree replaceComment(Cursor cursor, String existingText, String newText) {
        Tree tree = cursor.getValue();
        if (!(tree instanceof J) || existingText.equals(newText) || !hasComment(cursor, existingText)) {
            return tree;
        }
        return ((J) tree).withComments(ListUtils.map(((J) tree).getComments(), c ->
                c instanceof RubyTextComment && existingText.equals(body((RubyTextComment) c)) ?
                        withBody((RubyTextComment) c, newText) : c));
    }

    /**
     * The body between the delimiters: for a block comment that excludes both the newline that ends
     * the {@code =begin} line (and any tag on it, e.g. {@code =begin rdoc}) and the one that starts
     * the {@code =end} line.
     */
    private static String body(RubyTextComment comment) {
        if (!comment.isMultiline()) {
            return comment.getText();
        }
        String text = comment.getText();
        int start = text.indexOf('\n');
        int end = text.lastIndexOf('\n');
        return start < 0 || end <= start ? text : text.substring(start + 1, end);
    }

    private static RubyTextComment withBody(RubyTextComment comment, String body) {
        if (!comment.isMultiline()) {
            return comment.withText(body);
        }
        String text = comment.getText();
        int start = text.indexOf('\n');
        int end = text.lastIndexOf('\n');
        return start < 0 || end <= start ? comment.withText("\n" + body + "\n") :
                comment.withText(text.substring(0, start + 1) + body + text.substring(end));
    }
}
