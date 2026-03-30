/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.python.tree;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public final class PySpace {

    public static final class SpaceBuilder {
        private @Nullable String initialWhitespace;
        private @Nullable List<Comment> comments;

        private @Nullable StringBuilder whitespaceBuilder;
        private @Nullable String nextComment;

        private String finishWhitespace() {
            if (whitespaceBuilder == null) {
                return "";
            }
            String ws = whitespaceBuilder.toString();
            whitespaceBuilder.setLength(0);
            return ws;
        }

        private void finishComment() {
            String whitespace = finishWhitespace();
            if (nextComment != null) {
                if (comments == null) {
                    comments = new ArrayList<>();
                }
                comments.add(new PyComment(nextComment, whitespace, false, Markers.EMPTY));
            } else if (!whitespace.isEmpty()) {
                if (this.initialWhitespace != null) {
                    throw new IllegalStateException("unexpected");
                }
                this.initialWhitespace = whitespace;
            }
        }

        @SuppressWarnings("UnusedReturnValue")
        public SpaceBuilder addWhitespace(String whitespace) {
            if (whitespaceBuilder == null) {
                whitespaceBuilder = new StringBuilder();
            }
            whitespaceBuilder.append(whitespace);
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public SpaceBuilder addComment(String commentWithHash) {
            finishComment();
            nextComment = commentWithHash;
            return this;
        }

        public Space build() {
            finishComment();
            Space space = Space.build(
                    initialWhitespace == null ? "" : initialWhitespace,
                    comments == null ? emptyList() : comments
            );
            reset();
            return space;
        }

        public SpaceBuilder reset() {
            this.initialWhitespace = null;
            this.whitespaceBuilder.setLength(0);
            this.comments = null;
            return this;
        }
    }

    public static Space appendWhitespace(Space space, String whitespace) {
        if (!space.getComments().isEmpty()) {
            return space.withComments(
                    ListUtils.mapLast(
                            space.getComments(),
                            comment -> comment.withSuffix(comment.getSuffix() + whitespace)
                    )
            );
        }
        return space.withWhitespace(
                space.getWhitespace() + whitespace
        );
    }

    public static Space appendComment(Space space, String commentWithHash) {
        final String commentText = validateComment(commentWithHash);
        return space.withComments(ListUtils.concat(
                space.getComments(),
                new PyComment(commentText, "", false, Markers.EMPTY)
        ));
    }

    private static String validateComment(String commentWithHash) {
        if (!commentWithHash.startsWith("#")) {
            throw new IllegalArgumentException("comment should start with a hash");
        }
        if (commentWithHash.contains("\n")) {
            throw new IllegalArgumentException("comment cannot contain newlines");
        }
        return commentWithHash.substring(1);
    }

    public enum IndentStartMode {
        LINE_START,
        AFTER_STATEMENT,
    }

    public enum IndentEndMode {
        STATEMENT_START,
        REST_OF_LINE,
    }

    public static Space reindent(Space original, String indentWithoutNewline, IndentStartMode startMode, IndentEndMode endMode) {

        if (indentWithoutNewline.contains("\n")) {
            throw new IllegalArgumentException("argument to `deindent` should not contain newline: " + Space.build(indentWithoutNewline, emptyList()));
        }

        if (indentWithoutNewline.isEmpty()) {
            return original;
        }

        switch (endMode) {
            case REST_OF_LINE:
                if (!original.getLastWhitespace().endsWith("\n")) {
                    throw new IllegalStateException("expected statement suffix to end with a newline: " + original);
                }
                break;
            case STATEMENT_START:
                if (!original.getComments().isEmpty() || original.getLastWhitespace().contains("\n")) {
                    if (!original.getLastWhitespace().endsWith("\n")) {
                        throw new IllegalStateException("expected statement prefix to end with an indent placeholder: " + original);
                    }
                } else {
                    if (!original.getLastWhitespace().isEmpty()) {
                        throw new IllegalStateException("expected statement prefix to end with an indent placeholder: " + original);
                    }
                }
                break;
        }

        Space space = Space.build(original.getWhitespace(), emptyList());

        List<Comment> originalComments = original.getComments();

        if (original.getComments().isEmpty()) {
            if (startMode == IndentStartMode.LINE_START && endMode == IndentEndMode.STATEMENT_START) {
                if (original.getWhitespace().isEmpty() || original.getWhitespace().endsWith("\n")) {
                    space = appendWhitespace(space, indentWithoutNewline);
                }
            }
        } else {
            for (int i = 0; i < originalComments.size(); i++) {
                Comment originalComment = originalComments.get(i);
                PyComment comment = (PyComment) originalComment;

                if (comment.isAlignedToIndent() && (i != 0 || startMode == IndentStartMode.LINE_START)) {
                    if (space.getLastWhitespace().isEmpty() || space.getLastWhitespace().endsWith("\n")) {
                        space = appendWhitespace(space, indentWithoutNewline);
                    }
                }

                space = space.withComments(ListUtils.concat(
                        space.getComments(),
                        comment
                ));
            }

            if (endMode == IndentEndMode.STATEMENT_START) {
                space = appendWhitespace(space, indentWithoutNewline);
            }
        }

        return space;
    }

    public static Space deindent(Space original, String indentWithoutNewline, IndentStartMode startMode, IndentEndMode endMode) {
        if (indentWithoutNewline.contains("\n")) {
            throw new IllegalArgumentException("argument to `deindent` should not contain newline: " + Space.build(indentWithoutNewline, emptyList()));
        }

        if (indentWithoutNewline.isEmpty()) {
            return original;
        }

        final String indentWithNewline = "\n" + indentWithoutNewline;

        switch (endMode) {
            case REST_OF_LINE:
                if (!original.getLastWhitespace().endsWith("\n")) {
                    throw new IllegalStateException("expected statement suffix to end with a newline");
                }
                break;
            case STATEMENT_START:
                if (!original.getComments().isEmpty() || original.getLastWhitespace().contains("\n")) {
                    if (!original.getLastWhitespace().endsWith(indentWithNewline)) {
                        throw new IllegalStateException("expected statement prefix to end with an indent");
                    }
                } else {
                    if (!original.getLastWhitespace().equals(indentWithoutNewline)) {
                        throw new IllegalStateException("expected statement prefix to end with an indent");
                    }
                }
                break;
        }

        Space space;

        boolean currentlyIndented;

        if (startMode == IndentStartMode.LINE_START && original.getWhitespace().equals(indentWithoutNewline)) {
            if (endMode == IndentEndMode.STATEMENT_START || !original.getComments().isEmpty()) {
                currentlyIndented = true;
                space = Space.EMPTY;
            } else {
                // weird coincidence; maybe not possible?
                currentlyIndented = false;
                space = Space.build(original.getWhitespace(), emptyList());
            }
        } else if (original.getWhitespace().endsWith(indentWithNewline)) {
            currentlyIndented = true;
            space = Space.build(
                    original.getWhitespace().substring(
                            // just keep the newline
                            0, original.getWhitespace().length() - indentWithoutNewline.length()
                    ),
                    emptyList()
            );
        } else {
            currentlyIndented = false;
            space = Space.build(original.getWhitespace(), emptyList());
        }

        List<Comment> originalComments = original.getComments();
        for (int i = 0; i < originalComments.size(); i++) {
            Comment originalComment = originalComments.get(i);
            PyComment comment = (PyComment) originalComment;
            comment = comment.withAlignedToIndent(currentlyIndented);

            final boolean isLastComment = i == originalComments.size() - 1;
            if (!isLastComment || endMode == IndentEndMode.STATEMENT_START) {
                currentlyIndented = comment.getSuffix().endsWith(indentWithNewline);
                if (currentlyIndented) {
                    comment = comment.withSuffix(
                            comment.getSuffix().substring(
                                    // just keep the newline
                                    0, comment.getSuffix().length() - indentWithoutNewline.length()
                            )
                    );
                }
            } else {
                currentlyIndented = false;
            }
            space = space.withComments(
                    ListUtils.concat(space.getComments(), comment)
            );
        }

        return space;
    }

    public static Space stripIndent(Space space, String expectedIndent) {
        if (space.getComments().isEmpty()) {
            final String ws = space.getWhitespace();
            if (!ws.endsWith(expectedIndent)) {
                throw new IllegalStateException("expected statement prefix to end with block indent");
            }
            space = space.withWhitespace(
                    ws.substring(0, ws.length() - expectedIndent.length())
            );
        } else {
            space = space.withComments(
                    ListUtils.mapLast(
                            space.getComments(),
                            lastComment -> {
                                final String suffix = lastComment.getSuffix();
                                if (!suffix.endsWith(expectedIndent)) {
                                    throw new IllegalStateException("expected statement prefix to end with block indent");
                                }
                                return lastComment.withSuffix(
                                        suffix.substring(0, suffix.length() - expectedIndent.length())
                                );
                            }
                    )
            );
        }

        return space;
    }


    public enum Location {
        ASSERT_ELEMENT_SUFFIX,
        ASSERT_PREFIX,
        ASYNC_PREFIX,
        AWAIT_PREFIX,
        BINARY_NEGATION,
        BINARY_OPERATOR,
        BINARY_PREFIX,
        CHAINED_ASSIGNMENT_PREFIX,
        CHAINED_ASSIGNMENT_VARIABLE_SUFFIX,
        COLLECTION_LITERAL_ELEMENT_SUFFIX,
        COLLECTION_LITERAL_PREFIX,
        COMPILATION_UNIT_STATEMENT_SUFFIX,
        COMPREHENSION_EXPRESSION_CLAUSE_ASYNC_SUFFIX,
        COMPREHENSION_EXPRESSION_CLAUSE_ITERATED_LIST,
        COMPREHENSION_EXPRESSION_CLAUSE_PREFIX,
        COMPREHENSION_EXPRESSION_CONDITION_PREFIX,
        COMPREHENSION_EXPRESSION_PREFIX,
        COMPREHENSION_EXPRESSION_SUFFIX,
        DEL_PREFIX,
        DEL_TARGET_SUFFIX,
        DICT_ENTRY,
        DICT_ENTRY_KEY_SUFFIX,
        DICT_LITERAL_ELEMENT_SUFFIX,
        DICT_LITERAL_PREFIX,
        ELSE_WRAPPER_PREFIX,
        ERROR_FROM_PREFIX,
        ERROR_FROM_SOURCE,
        EXCEPTION_TYPE_PREFIX,
        EXPRESSION_TYPE_TREE_PREFIX,
        FORMATTED_STRING_PREFIX,
        FORMATTED_STRING_VALUE_DEBUG_SUFFIX,
        FORMATTED_STRING_VALUE_EXPRESSION_SUFFIX,
        FORMATTED_STRING_VALUE_PREFIX,
        KEY_VALUE_PREFIX,
        KEY_VALUE_SUFFIX,
        LITERAL_TYPE_PREFIX,
        MATCH_CASE_GUARD,
        MATCH_CASE_PATTERN_CHILD_PREFIX,
        MATCH_CASE_PATTERN_PREFIX,
        MATCH_CASE_PREFIX,
        MATCH_PATTERN_ELEMENT_SUFFIX,
        MULTI_IMPORT_FROM_SUFFIX,
        MULTI_IMPORT_NAME_PREFIX,
        MULTI_IMPORT_NAME_SUFFIX,
        MULTI_IMPORT_PREFIX,
        NAMED_ARGUMENT,
        NAMED_ARGUMENT_PREFIX,
        PASS_PREFIX,
        SLICE_PREFIX,
        SLICE_START_SUFFIX,
        SLICE_STEP_SUFFIX,
        SLICE_STOP_SUFFIX,
        SPECIAL_PARAMETER_PREFIX,
        STAR_PREFIX,
        TOP_LEVEL_STATEMENT,
        TRAILING_ELSE_WRAPPER_ELSE_BLOCK,
        TRAILING_ELSE_WRAPPER_PREFIX,
        TYPE_ALIAS_PREFIX,
        TYPE_ALIAS_VALUE,
        TYPE_HINTED_EXPRESSION_PREFIX,
        TYPE_HINT_PREFIX,
        UNION_ELEMENT_SUFFIX,
        UNION_TYPE_PREFIX,
        VARIABLE_SCOPE_NAME_SUFFIX,
        VARIABLE_SCOPE_PREFIX,
        YIELD_FROM_PREFIX,
    }

    private PySpace() {
    }
}
