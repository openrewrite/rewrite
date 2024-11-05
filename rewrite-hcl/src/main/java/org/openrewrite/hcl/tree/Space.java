/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.hcl.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

import java.util.*;

import static java.util.Collections.emptyList;

/**
 * Comments can occur wherever whitespace can.
 * So whitespace and comments are like peanut butter and jelly.
 */
@EqualsAndHashCode
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public class Space {
    public static final Space EMPTY = new Space("", emptyList());

    @Getter
    private final List<Comment> comments;

    @Nullable
    private final String whitespace;

    /*
     * Most occurrences of spaces will have no comments or markers and will be repeated frequently throughout a source file.
     * e.g.: a single space between keywords, or the common indentation of every line in a block.
     * So use flyweights to avoid storing many instances of functionally identical spaces
     */
    private static final Map<String, Space> flyweights = Collections.synchronizedMap(new WeakHashMap<>());

    private Space(@Nullable String whitespace, List<Comment> comments) {
        this.comments = comments;
        this.whitespace = whitespace == null || whitespace.isEmpty() ? null : whitespace;
    }

    @JsonCreator
    public static Space build(@Nullable String whitespace, List<Comment> comments) {
        if (comments.isEmpty()) {
            if (whitespace == null || whitespace.isEmpty()) {
                return Space.EMPTY;
            } else if (whitespace.length() <= 100) {
                //noinspection StringOperationCanBeSimplified
                return flyweights.computeIfAbsent(whitespace, k -> new Space(new String(whitespace), comments));
            }
        }
        return new Space(whitespace, comments);
    }

    public String getIndent() {
        if (!comments.isEmpty()) {
            return getWhitespaceIndent(comments.get(comments.size() - 1).getSuffix());
        }
        return getWhitespaceIndent(whitespace);
    }

    public String getLastWhitespace() {
        if (!comments.isEmpty()) {
            return comments.get(comments.size() - 1).getSuffix();
        }
        return whitespace == null ? "" : whitespace;
    }

    public Space withLastWhitespace(String whitespace) {
        if (!comments.isEmpty()) {
            return withComments(ListUtils.map(comments, (i, comment) -> {
                if (i == comments.size() - 1) {
                    return comment.withSuffix(comment.getSuffix() + whitespace);
                }
                return comment;
            }));
        } else {
            return withWhitespace(whitespace);
        }
    }

    private String getWhitespaceIndent(@Nullable String whitespace) {
        if (whitespace == null) {
            return "";
        }
        int lastNewline = whitespace.lastIndexOf('\n');
        if (lastNewline >= 0) {
            return whitespace.substring(lastNewline + 1);
        } else if (lastNewline == whitespace.length() - 1) {
            return "";
        }
        return whitespace;
    }

    public String getWhitespace() {
        return whitespace == null ? "" : whitespace;
    }

    public Space withComments(List<Comment> comments) {
        if (comments == this.comments) {
            return this;
        }
        if (comments.isEmpty() && (whitespace == null || whitespace.isEmpty())) {
            return Space.EMPTY;
        }
        return build(whitespace, comments);
    }

    public Space withWhitespace(String whitespace) {
        if (comments.isEmpty() && whitespace.isEmpty()) {
            return Space.EMPTY;
        }

        if ((whitespace.isEmpty() && this.whitespace == null) || whitespace.equals(this.whitespace)) {
            return this;
        }
        return build(whitespace, comments);
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public static Space firstPrefix(@Nullable List<? extends Hcl> trees) {
        return trees == null || trees.isEmpty() ? Space.EMPTY : trees.iterator().next().getPrefix();
    }

    public static Space format(String formatting) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder comment = new StringBuilder();
        List<Comment> comments = new ArrayList<>();

        boolean inSingleLineComment = false;
        Comment.Style inLineSlashOrHashComment = null;
        boolean inMultiLineComment = false;

        char last = 0;

        char[] charArray = formatting.toCharArray();
        for (char c : charArray) {
            switch (c) {
                case '#':
                    if (Comment.Style.LINE_SLASH.equals(inLineSlashOrHashComment)) {
                        comment.append(c);
                    } else {
                        if (inSingleLineComment) {
                            comment.append(c);
                        } else {
                            inSingleLineComment = true;
                            inLineSlashOrHashComment = Comment.Style.LINE_HASH;
                            comment = new StringBuilder();
                        }
                    }
                    break;
                case '/':
                    if (Comment.Style.LINE_HASH.equals(inLineSlashOrHashComment)) {
                        comment.append(c);
                    } else {
                        if (inSingleLineComment) {
                            comment.append(c);
                        } else if (last == '/' && !inMultiLineComment) {
                            inSingleLineComment = true;
                            inLineSlashOrHashComment = Comment.Style.LINE_SLASH;
                            comment = new StringBuilder();
                        } else if (last == '*' && inMultiLineComment && comment.length() > 0) {
                            inMultiLineComment = false;
                            comment.setLength(comment.length() - 1); // trim the last '*'
                            comments.add(new Comment(Comment.Style.INLINE, comment.toString(), prefix.toString(), Markers.EMPTY));
                            prefix = new StringBuilder();
                            comment = new StringBuilder();
                            continue;
                        } else {
                            comment.append(c);
                        }
                    }
                    break;
                case '\r':
                case '\n':
                    if (inSingleLineComment) {
                        inSingleLineComment = false;
                        comments.add(new Comment(inLineSlashOrHashComment, comment.toString(), prefix.toString(), Markers.EMPTY));
                        inLineSlashOrHashComment = null;
                        prefix = new StringBuilder();
                        comment = new StringBuilder();
                        prefix.append(c);
                    } else if (!inMultiLineComment) {
                        prefix.append(c);
                    } else {
                        comment.append(c);
                    }
                    break;
                case '*':
                    if (inSingleLineComment) {
                        comment.append(c);
                    } else if (last == '/' && !inMultiLineComment) {
                        inMultiLineComment = true;
                        comment = new StringBuilder();
                    } else {
                        comment.append(c);
                    }
                    break;
                default:
                    if (inSingleLineComment || inMultiLineComment) {
                        comment.append(c);
                    } else {
                        prefix.append(c);
                    }
            }
            last = c;
        }

        // Shift the whitespace on each comment forward to be a suffix of the comment before it, and the
        // whitespace on the first comment to be the whitespace of the tree element. The remaining prefix is the suffix
        // of the last comment.
        String whitespace = prefix.toString();
        if (!comments.isEmpty()) {
            for (int i = comments.size() - 1; i >= 0; i--) {
                Comment c = comments.get(i);
                String next = c.getSuffix();
                comments.set(i, c.withSuffix(whitespace));
                whitespace = next;
            }
        }

        return build(whitespace, comments);
    }

    public static <H extends Hcl> List<H> formatFirstPrefix(List<H> trees, Space prefix) {
        if (!trees.isEmpty() && !trees.get(0).getPrefix().equals(prefix)) {
            List<H> formattedTrees = new ArrayList<>(trees);
            formattedTrees.set(0, formattedTrees.get(0).withPrefix(prefix));
            return formattedTrees;
        }

        return trees;
    }

    private static final String[] spaces = {
            "·₁", "·₂", "·₃", "·₄", "·₅", "·₆", "·₇", "·₈", "·₉", "·₊"
    };

    private static final String[] tabs = {
            "-₁", "-₂", "-₃", "-₄", "-₅", "-₆", "-₇", "-₈", "-₉", "-₊"
    };

    @Override
    public String toString() {
        StringBuilder printedWs = new StringBuilder();
        int lastNewline = 0;
        if (whitespace != null) {
            char[] charArray = whitespace.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                char c = charArray[i];
                if (c == '\n') {
                    printedWs.append("\\n");
                    lastNewline = i + 1;
                } else if (c == '\r') {
                    printedWs.append("\\r");
                    lastNewline = i + 1;
                } else if (c == ' ') {
                    printedWs.append(spaces[(i - lastNewline) % 10]);
                } else if (c == '\t') {
                    printedWs.append(tabs[(i - lastNewline) % 10]);
                }
            }
        }

        return "Space(" +
                "comments=<" + (comments.size() == 1 ? "1 comment" : comments.size() + " comments") +
                ">, whitespace='" + printedWs + "')";
    }

    public enum Location {
        ATTRIBUTE,
        ATTRIBUTE_ACCESS,
        ATTRIBUTE_ACCESS_NAME,
        ATTRIBUTE_ASSIGNMENT,
        BINARY,
        BINARY_OPERATOR,
        BLOCK,
        BLOCK_CLOSE,
        BLOCK_OPEN,
        CONDITIONAL,
        CONDITIONAL_TRUE_PREFIX,
        CONDITIONAL_FALSE_PREFIX,
        CONFIG_FILE,
        CONFIG_FILE_EOF,
        EMPTY,
        EXPRESSION_PREFIX,
        FOR_CONDITION,
        FOR_INTRO,
        FOR_OBJECT,
        FOR_OBJECT_SUFFIX,
        FOR_TUPLE,
        FOR_TUPLE_SUFFIX,
        FOR_UPDATE,
        FOR_UPDATE_VALUE,
        FOR_UPDATE_VALUE_ELLIPSIS,
        FOR_VARIABLES,
        FOR_VARIABLE_SUFFIX,
        FUNCTION_CALL,
        FUNCTION_CALL_ARGUMENTS,
        FUNCTION_CALL_ARGUMENT_SUFFIX,
        HEREDOC,
        HEREDOC_END,
        INDEX,
        INDEX_POSITION,
        INDEX_POSITION_SUFFIX,
        IDENTIFIER,
        LITERAL,
        OBJECT_VALUE,
        OBJECT_VALUE_ATTRIBUTES,
        OBJECT_VALUE_ATTRIBUTE_COMMA,
        OBJECT_VALUE_ATTRIBUTE_SUFFIX,
        ONE_LINE_BLOCK,
        PARENTHETICAL_EXPRESSION,
        PARENTHESES_SUFFIX,
        QUOTED_TEMPLATE,
        SPLAT,
        SPLAT_OPERATOR,
        SPLAT_OPERATOR_PREFIX,
        SPLAT_OPERATOR_SUFFIX,
        TEMPLATE_INTERPOLATION,
        TUPLE,
        TUPLE_VALUES,
        TUPLE_VALUE_SUFFIX,
        UNARY,
        VARIABLE_EXPRESSION
    }
}
