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
package org.openrewrite.java.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.CommentService;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;
import org.openrewrite.trait.Comments.Placement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * {@link CommentService} for the {@code J} LST model. Because Kotlin and Groovy reuse {@code J}
 * (and {@code J.Space}) for whitespace and comments, this single implementation serves Java,
 * Kotlin, and Groovy sources.
 */
@Incubating(since = "8.86.0")
public class JavaCommentService extends CommentService {

    private static final Pattern NEWLINE = Pattern.compile("\\R");

    @Override
    public List<String> getComments(Cursor cursor) {
        Object tree = cursor.getValue();
        if (!(tree instanceof J)) {
            return emptyList();
        }
        List<String> texts = new ArrayList<>();
        for (Comment comment : ((J) tree).getComments()) {
            if (comment instanceof TextComment) {
                texts.add(((TextComment) comment).getText());
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
        // J has no child-content notion for comments, so FIRST_CHILD behaves as BEFORE.
        Tree tree = cursor.getValue();
        if (!(tree instanceof J) || hasEquivalentComment(cursor, text)) {
            return tree;
        }
        J j = (J) tree;

        // The comment's suffix is the whitespace rendered between it and the element. By default reuse
        // the element's own leading indentation so the comment lands on the line above (matching the
        // existing AddCommentTo* recipes); callers can override it (e.g. for a top-of-file element
        // whose prefix is empty) via the suffix parameter.
        String indent = suffix != null ? suffix : j.getPrefix().getWhitespace();
        String commentText = multiline ? text : NEWLINE.matcher(text).replaceAll(" ");

        TextComment comment = new TextComment(multiline, commentText, indent, Markers.EMPTY);
        J withComment = j.withComments(ListUtils.concat(j.getComments(), comment));
        return autoFormat(cursor, withComment);
    }

    /**
     * Run the source file's language-appropriate {@link AutoFormatService} over the element so the
     * inserted comment is properly laid out without the caller having to format afterwards. Resolving
     * the service through {@link SourceFile#service(Class)} keeps this correct for Kotlin and Groovy,
     * which reuse this comment service but supply their own formatter.
     * <p>
     * A comment added {@link Placement#BEFORE} lives in the element's prefix, so only the formatted
     * prefix is transplanted back onto the original element. This deliberately discards any formatting
     * the visitor would otherwise apply to the element's body (e.g. expanding an empty {@code {}}
     * block), keeping the change scoped to the comment.
     */
    private static J autoFormat(Cursor cursor, J withComment) {
        SourceFile sourceFile = cursor.firstEnclosing(SourceFile.class);
        if (sourceFile == null) {
            return withComment;
        }
        AutoFormatService autoFormatService = sourceFile.service(AutoFormatService.class);
        J formatted = autoFormatService.autoFormatVisitor(null)
                .visit(withComment, new InMemoryExecutionContext(), cursor.getParentOrThrow());
        return formatted == null ? withComment : withComment.withPrefix(formatted.getPrefix());
    }

    @Override
    public Tree removeComment(Cursor cursor, String text) {
        Tree tree = cursor.getValue();
        if (!(tree instanceof J) || !hasComment(cursor, text)) {
            return tree;
        }
        J j = (J) tree;
        //noinspection DataFlowIssue
        return j.withComments(ListUtils.map(j.getComments(), c ->
                c instanceof TextComment && text.equals(((TextComment) c).getText()) ? null : c));
    }

    @Override
    public Tree replaceComment(Cursor cursor, String existingText, String newText) {
        Tree tree = cursor.getValue();
        if (!(tree instanceof J) || existingText.equals(newText) || !hasComment(cursor, existingText)) {
            return tree;
        }
        J j = (J) tree;
        return j.withComments(ListUtils.map(j.getComments(), c ->
                c instanceof TextComment && existingText.equals(((TextComment) c).getText()) ?
                        ((TextComment) c).withText(newText) : c));
    }
}
