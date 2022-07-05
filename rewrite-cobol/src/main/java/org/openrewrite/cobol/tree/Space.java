/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.cobol.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.EqualsAndHashCode;
import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static java.util.Collections.emptyList;

/**
 * Wherever whitespace can occur in protobuf, so can comments (at least block style comments).
 * So whitespace and comments are like peanut butter and jelly.
 */
@EqualsAndHashCode
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public class Space {
    public static final Space EMPTY = new Space("");

    @Nullable
    private final String whitespace;

    /*
     * Most occurrences of spaces will have no comments or markers and will be repeated frequently throughout a source file.
     * e.g.: a single space between keywords, or the common indentation of every line in a block.
     * So use flyweights to avoid storing many instances of functionally identical spaces
     */
    private static final Map<String, Space> flyweights = new WeakHashMap<>();

    private Space(@Nullable String whitespace) {
        this.whitespace = whitespace == null || whitespace.isEmpty() ? null : whitespace;
    }

    @JsonCreator
    public static Space build(@Nullable String whitespace) {
        if (whitespace == null || whitespace.isEmpty()) {
            return Space.EMPTY;
        }
        return flyweights.computeIfAbsent(whitespace, k -> new Space(whitespace));
    }

    public String getIndent() {
        return getWhitespaceIndent(whitespace);
    }

    public String getLastWhitespace() {
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

    public String getWhitespace() {
        return whitespace == null ? "" : whitespace;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public static Space firstPrefix(@Nullable List<? extends Cobol> trees) {
        return trees == null || trees.isEmpty() ? Space.EMPTY : trees.iterator().next().getPrefix();
    }

    public static Space format(String formatting) {
        return build(formatting);
    }

    @SuppressWarnings("ConstantConditions")
    public static <P extends Cobol> List<CobolRightPadded<P>> formatLastSuffix(@Nullable List<CobolRightPadded<P>> trees,
                                                                               Space suffix) {
        if (trees == null) {
            return null;
        }

        if (!trees.isEmpty() && !trees.get(trees.size() - 1).getAfter().equals(suffix)) {
            List<CobolRightPadded<P>> formattedTrees = new ArrayList<>(trees);
            formattedTrees.set(
                    formattedTrees.size() - 1,
                    formattedTrees.get(formattedTrees.size() - 1).withAfter(suffix)
            );
            return formattedTrees;
        }

        return trees;
    }

    public static <P extends Cobol> List<P> formatFirstPrefix(List<P> trees, Space prefix) {
        if (!trees.isEmpty() && !trees.get(0).getPrefix().equals(prefix)) {
            List<P> formattedTrees = new ArrayList<>(trees);
            formattedTrees.set(0, formattedTrees.get(0).withPrefix(prefix));
            return formattedTrees;
        }

        return trees;
    }

    @Override
    public String toString() {
        return whitespace;
    }
}
