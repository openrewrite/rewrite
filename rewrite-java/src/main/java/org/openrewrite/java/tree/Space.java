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
import lombok.EqualsAndHashCode;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.util.*;

import static java.util.Collections.emptyList;

/**
 * Wherever whitespace can occur in Java, so can comments (at least block and javadoc style comments).
 * So whitespace and comments are like peanut butter and jelly.
 */
@EqualsAndHashCode
public class Space {
    public static final Space EMPTY = new Space("", emptyList());
    public static final Space SINGLE_SPACE = new Space(" ", emptyList());

    private final List<Comment> comments;

    @Nullable
    private final String whitespace;

    /*
     * Most occurrences of spaces will have no comments or markers and will be repeated frequently throughout a source file.
     * e.g.: a single space between keywords, or the common indentation of every line in a block.
     * So use flyweights to avoid storing many instances of functionally identical spaces
     */
    private static final Map<String, Space> flyweights = Collections.synchronizedMap(new WeakHashMap<>());

    static {
        flyweights.put(" ", SINGLE_SPACE);
    }

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

    public List<Comment> getComments() {
        return comments;
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
        } else if (comments.isEmpty() && " ".equals(whitespace)) {
            return SINGLE_SPACE;
        }
        if ((whitespace.isEmpty() && this.whitespace == null) || whitespace.equals(this.whitespace)) {
            return this;
        }
        return build(whitespace, comments);
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public static Space firstPrefix(@Nullable List<? extends J> trees) {
        return trees == null || trees.isEmpty() ? Space.EMPTY : trees.get(0).getPrefix();
    }

    public static Space format(String formatting) {
        return format(formatting, 0, formatting.length());
    }

    public static Space format(String formatting, int beginIndex, int toIndex) {
        if (beginIndex == toIndex) {
            return Space.EMPTY;
        } else if (toIndex == beginIndex + 1 && ' ' == formatting.charAt(beginIndex)) {
            return Space.SINGLE_SPACE;
        } else {
            rangeCheck(formatting.length(), beginIndex, toIndex);
        }

        StringBuilder prefix = new StringBuilder();
        StringBuilder comment = new StringBuilder();
        List<Comment> comments = new ArrayList<>(1);

        boolean inSingleLineComment = false;
        boolean inMultiLineComment = false;

        char last = 0;

        for (int i = beginIndex; i < toIndex; i++) {
            char c = formatting.charAt(i);
            switch (c) {
                case '/':
                    if (inSingleLineComment) {
                        comment.append(c);
                    } else if (last == '/' && !inMultiLineComment) {
                        inSingleLineComment = true;
                        comment.setLength(0);
                        prefix.setLength(prefix.length() - 1);
                    } else if (last == '*' && inMultiLineComment && comment.length() > 0) {
                        inMultiLineComment = false;
                        comment.setLength(comment.length() - 1); // trim the last '*'
                        comments.add(new TextComment(true, comment.toString(), prefix.substring(0, prefix.length() - 1), Markers.EMPTY));
                        prefix.setLength(0);
                        comment.setLength(0);
                        continue;
                    } else if (inMultiLineComment) {
                        comment.append(c);
                    } else {
                        prefix.append(c);
                    }
                    break;
                case '\r':
                case '\n':
                    if (inSingleLineComment) {
                        inSingleLineComment = false;
                        comments.add(new TextComment(false, comment.toString(), prefix.toString(), Markers.EMPTY));
                        prefix.setLength(0);
                        comment.setLength(0);
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
                        comment.setLength(0);
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
        // If a file ends with a single-line comment there may be no terminating newline
        if (comment.length() > 0) {
            comments.add(new TextComment(false, comment.toString(), prefix.toString(), Markers.EMPTY));
            prefix.setLength(0);
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

    @SuppressWarnings("ConstantConditions")
    public static <J2 extends J> List<JRightPadded<J2>> formatLastSuffix(@Nullable List<JRightPadded<J2>> trees,
                                                                         Space suffix) {
        if (trees == null) {
            return null;
        }

        if (!trees.isEmpty() && !trees.get(trees.size() - 1).getAfter().equals(suffix)) {
            List<JRightPadded<J2>> formattedTrees = new ArrayList<>(trees);
            formattedTrees.set(
                    formattedTrees.size() - 1,
                    formattedTrees.get(formattedTrees.size() - 1).withAfter(suffix)
            );
            return formattedTrees;
        }

        return trees;
    }

    public static <J2 extends J> List<J2> formatFirstPrefix(List<J2> trees, Space prefix) {
        if (!trees.isEmpty() && !trees.get(0).getPrefix().equals(prefix)) {
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
        if (whitespace != null) {
            for (int i = 0; i < whitespace.length(); i++) {
                char c = whitespace.charAt(i);
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
        ANY,
        ANNOTATED_TYPE_PREFIX,
        ANNOTATION_ARGUMENTS,
        ANNOTATION_ARGUMENT_SUFFIX,
        ANNOTATIONS,
        ANNOTATION_PREFIX,
        ARRAY_ACCESS_PREFIX,
        ARRAY_INDEX_SUFFIX,
        ARRAY_TYPE_PREFIX,
        ASSERT_PREFIX,
        ASSERT_DETAIL,
        ASSERT_DETAIL_PREFIX,
        ASSIGNMENT,
        ASSIGNMENT_OPERATION_PREFIX,
        ASSIGNMENT_OPERATION_OPERATOR,
        ASSIGNMENT_PREFIX,
        BINARY_OPERATOR,
        BINARY_PREFIX,
        BLOCK_END,
        BLOCK_PREFIX,
        BLOCK_STATEMENT_SUFFIX,
        BREAK_PREFIX,
        CASE,
        CASE_PREFIX,
        CASE_BODY,
        CASE_EXPRESSION,
        CASE_SUFFIX,
        CATCH_ALTERNATIVE_SUFFIX,
        CATCH_PREFIX,
        CLASS_DECLARATION_PREFIX,
        CLASS_KIND,
        COMPILATION_UNIT_EOF,
        COMPILATION_UNIT_PREFIX,
        CONTINUE_PREFIX,
        CONTROL_PARENTHESES_PREFIX,
        DIMENSION_PREFIX,
        DIMENSION,
        DIMENSION_SUFFIX,
        DO_WHILE_PREFIX,
        ELSE_PREFIX,
        EMPTY_PREFIX,
        ENUM_VALUE_PREFIX,
        ENUM_VALUE_SET_PREFIX,
        ENUM_VALUE_SUFFIX,
        EXPRESSION_PREFIX,
        EXTENDS,
        FIELD_ACCESS_NAME,
        FIELD_ACCESS_PREFIX,
        FOREACH_ITERABLE_SUFFIX,
        FOREACH_VARIABLE_SUFFIX,
        FOR_BODY_SUFFIX,
        FOR_CONDITION_SUFFIX,
        FOR_CONTROL_PREFIX,
        FOR_EACH_CONTROL_PREFIX,
        FOR_EACH_LOOP_PREFIX,
        FOR_INIT_SUFFIX,
        FOR_PREFIX,
        FOR_UPDATE_SUFFIX,
        IDENTIFIER_PREFIX,
        IF_ELSE_SUFFIX,
        IF_PREFIX,
        IF_THEN_SUFFIX,
        IMPLEMENTS,
        IMPORT_ALIAS_PREFIX,
        PERMITS,
        IMPLEMENTS_SUFFIX,
        IMPORT_PREFIX,
        IMPORT_SUFFIX,
        INSTANCEOF_PREFIX,
        INSTANCEOF_SUFFIX,
        INTERSECTION_TYPE_PREFIX,
        LABEL_PREFIX,
        LABEL_SUFFIX,
        LAMBDA_ARROW_PREFIX,
        LAMBDA_PARAMETER,
        LAMBDA_PARAMETERS_PREFIX,
        LAMBDA_PREFIX,
        LANGUAGE_EXTENSION,
        LITERAL_PREFIX,
        MEMBER_REFERENCE_CONTAINING,
        MEMBER_REFERENCE_NAME,
        MEMBER_REFERENCE_PREFIX,
        METHOD_DECLARATION_PARAMETERS,
        METHOD_DECLARATION_PARAMETER_SUFFIX,
        METHOD_DECLARATION_DEFAULT_VALUE,
        METHOD_DECLARATION_PREFIX,
        METHOD_INVOCATION_ARGUMENTS,
        METHOD_INVOCATION_ARGUMENT_SUFFIX,
        METHOD_INVOCATION_NAME,
        METHOD_INVOCATION_PREFIX,
        METHOD_SELECT_SUFFIX,
        MODIFIER_PREFIX,
        MULTI_CATCH_PREFIX,
        NAMED_VARIABLE_SUFFIX,
        NEW_ARRAY_INITIALIZER,
        NEW_ARRAY_INITIALIZER_SUFFIX,
        NEW_ARRAY_PREFIX,
        NEW_CLASS_ARGUMENTS,
        NEW_CLASS_ARGUMENTS_SUFFIX,
        NEW_CLASS_ENCLOSING_SUFFIX,
        NEW_CLASS_PREFIX,
        NEW_PREFIX,
        NULLABLE_TYPE_PREFIX,
        NULLABLE_TYPE_SUFFIX,
        PACKAGE_PREFIX,
        PACKAGE_SUFFIX,
        PARAMETERIZED_TYPE_PREFIX,
        PARENTHESES_PREFIX,
        PARENTHESES_SUFFIX,
        PERMITS_SUFFIX,
        PRIMITIVE_PREFIX,
        RECORD_STATE_VECTOR,
        RECORD_STATE_VECTOR_SUFFIX,
        RETURN_PREFIX,
        STATEMENT_PREFIX,
        STATIC_IMPORT,
        STATIC_INIT_SUFFIX,
        SWITCH_PREFIX,
        SWITCH_EXPRESSION_PREFIX,
        SYNCHRONIZED_PREFIX,
        TERNARY_FALSE,
        TERNARY_PREFIX,
        TERNARY_TRUE,
        THROWS,
        THROWS_SUFFIX,
        THROW_PREFIX,
        TRY_FINALLY,
        TRY_PREFIX,
        TRY_RESOURCE,
        TRY_RESOURCES,
        TRY_RESOURCE_SUFFIX,
        TYPE_BOUNDS,
        TYPE_BOUND_SUFFIX,
        TYPE_CAST_PREFIX,
        TYPE_PARAMETERS,
        TYPE_PARAMETERS_PREFIX,
        TYPE_PARAMETER_SUFFIX,
        UNARY_OPERATOR,
        UNARY_PREFIX,
        UNKNOWN_PREFIX,
        UNKNOWN_SOURCE_PREFIX,
        VARARGS,
        VARIABLE_DECLARATIONS_PREFIX,
        VARIABLE_INITIALIZER,
        VARIABLE_PREFIX,
        WHILE_BODY_SUFFIX,
        WHILE_CONDITION,
        WHILE_PREFIX,
        WILDCARD_BOUND,
        WILDCARD_PREFIX,
        YIELD_PREFIX,
    }

    static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                    "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex < 0) {
            throw new StringIndexOutOfBoundsException(fromIndex);
        }
        if (toIndex > arrayLength) {
            throw new StringIndexOutOfBoundsException(toIndex);
        }
    }
}
