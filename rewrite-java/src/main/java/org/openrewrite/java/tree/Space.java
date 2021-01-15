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
package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wherever whitespace can occur in Java, so can comments (at least block and javadoc style comments).
 * So whitespace and comments are like peanut butter and jelly.
 */
@EqualsAndHashCode
public class Space {
    public static Space EMPTY = new Space("", Collections.emptyList());

    private final List<Comment> comments;
    private final String whitespace;

    private Space(
            @JsonProperty("whitespace") String whitespace,
            @JsonProperty("comments") List<Comment> comments) {
        this.comments = comments;
        this.whitespace = whitespace;
    }

    @JsonCreator
    public static Space build(
            @JsonProperty("whitespace") String whitespace,
            @JsonProperty("comments") List<Comment> comments) {
        if (whitespace.isEmpty() && comments.isEmpty()) {
            return Space.EMPTY;
        }
        return new Space(whitespace, comments);
    }

    @JsonIgnore
    public String getIndent() {
        int lastNewline = whitespace.lastIndexOf('\n');
        if (lastNewline >= 0) {
            return whitespace.substring(lastNewline + 1);
        } else if (lastNewline == whitespace.length() - 1) {
            return "";
        }
        return whitespace;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public String getWhitespace() {
        return whitespace;
    }

    public Space withComments(List<Comment> comments) {
        if (comments.isEmpty() && whitespace.isEmpty()) {
            return Space.EMPTY;
        }
        return build(whitespace, comments);
    }

    public Space withWhitespace(String whitespace) {
        if (comments.isEmpty() && whitespace.isEmpty()) {
            return Space.EMPTY;
        }
        if (!whitespace.equals(this.whitespace)) {
            return build(whitespace, comments);
        }
        return this;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public static Space firstPrefix(@Nullable List<? extends J> trees) {
        return trees == null || trees.isEmpty() ? Space.EMPTY : trees.iterator().next().getPrefix();
    }

    public static Space format(String formatting) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder comment = new StringBuilder();
        List<Comment> comments = new ArrayList<>();

        boolean inSingleLineComment = false;
        boolean inMultiLineComment = false;
        boolean inJavadoc = false;

        char last = 0;

        char[] charArray = formatting.toCharArray();
        for (char c : charArray) {
            switch (c) {
                case '/':
                    if (last == '/' && !inSingleLineComment && !inMultiLineComment && !inJavadoc) {
                        inSingleLineComment = true;
                        comment = new StringBuilder();
                    } else if (last == '*' && inMultiLineComment) {
                        inMultiLineComment = false;
                        comment.setLength(comment.length() - 1); // trim the last '*'
                        comments.add(new Comment(Comment.Style.BLOCK, comment.toString(), prefix.toString()));
                        prefix = new StringBuilder();
                        comment = new StringBuilder();
                    } else if (last == '*' && inJavadoc) {
                        inJavadoc = false;
                        comment.setLength(comment.length() - 1); // trim the last '*'
                        comments.add(new Comment(Comment.Style.JAVADOC, comment.toString(), prefix.toString()));
                        prefix = new StringBuilder();
                        comment = new StringBuilder();
                    } else {
                        comment.append(c);
                    }
                    break;
                case '\r':
                case '\n':
                    if (inSingleLineComment) {
                        inSingleLineComment = false;
                        comments.add(new Comment(Comment.Style.LINE, comment.toString(), prefix.toString()));
                        prefix = new StringBuilder();
                        comment = new StringBuilder();
                        prefix.append(c);
                    } else if (!inMultiLineComment && !inJavadoc) {
                        prefix.append(c);
                    } else {
                        comment.append(c);
                    }
                    break;
                case '*':
                    if (last == '/' && !inMultiLineComment && !inJavadoc) {
                        inMultiLineComment = true;
                        comment = new StringBuilder();
                    } else if (last == '*' && inMultiLineComment && comment.toString().isEmpty()) {
                        inMultiLineComment = false;
                        inJavadoc = true;
                        comment = new StringBuilder();
                    } else {
                        comment.append(c);
                    }
                    break;
                default:
                    if (inSingleLineComment || inMultiLineComment || inJavadoc) {
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

    public static <J2 extends J> List<JRightPadded<J2>> formatLastSuffix(@Nullable List<JRightPadded<J2>> trees,
                                                                         Space suffix) {
        if (trees == null) {
            return null;
        }

        if (!trees.isEmpty()) {
            List<JRightPadded<J2>> formattedTrees = new ArrayList<>(trees);
            formattedTrees.set(
                    formattedTrees.size() - 1,
                    formattedTrees.get(formattedTrees.size() - 1).withAfter(suffix)
            );
            return formattedTrees;
        }

        return trees;
    }

    public static <J2 extends J> List<J2> formatFirstPrefix(@Nullable List<J2> trees, Space prefix) {
        if (trees == null) {
            return null;
        }

        if (!trees.isEmpty()) {
            List<J2> formattedTrees = new ArrayList<>(trees);
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

        return "Space(" +
                "comments=<" + (comments.size() == 1 ? "1 comment" : comments.size() + " comments") +
                ">, whitespace='" + printedWs + "')";
    }
}
