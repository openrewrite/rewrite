package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wherever whitespace can occur in Java, so can comments (at least block and javadoc style comments).
 * So whitespace and comments are like peanut butter and jelly.
 */
public class Space {
    public static Space EMPTY = new Space(Collections.emptyList(), "");

    private final List<Comment> comments;
    private final String whitespace;

    public Space(List<Comment> comments, String whitespace) {
        this.comments = comments;
        this.whitespace = whitespace;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public String getWhitespace() {
        return whitespace;
    }

    public Space withComments(List<Comment> comments) {
        if(comments.isEmpty() && whitespace.isEmpty()) {
            return Space.EMPTY;
        }
        return new Space(comments, whitespace);
    }

    public Space withWhitespace(String whitespace) {
        if(comments.isEmpty() && whitespace.isEmpty()) {
            return Space.EMPTY;
        }
        return new Space(comments, whitespace);
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
                    if (last == '/' && !inSingleLineComment) {
                        inSingleLineComment = true;
                        comment = new StringBuilder();
                    } else if (last == '*' && inMultiLineComment) {
                        inMultiLineComment = false;
                        comment.setLength(comment.length() - 1); // trim the last '*'
                        comments.add(new Comment(prefix.toString(), Comment.Style.BLOCK, comment.toString()));
                        prefix = new StringBuilder();
                        comment = new StringBuilder();
                    } else if (last == '*' && inJavadoc) {
                        inJavadoc = false;
                        comment.setLength(comment.length() - 1); // trim the last '*'
                        comments.add(new Comment(prefix.toString(), Comment.Style.JAVADOC, comment.toString()));
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
                        comments.add(new Comment(prefix.toString(), Comment.Style.LINE, comment.toString()));
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

        return new Space(comments, prefix.toString());
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
}
