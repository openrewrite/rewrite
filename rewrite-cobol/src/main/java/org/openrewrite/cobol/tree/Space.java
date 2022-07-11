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
import org.openrewrite.cobol.internal.StringWithOriginalPositions;
import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static java.util.Collections.emptyList;

/**
 *
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

    private Space(String whitespace) {
        this.whitespace = whitespace;
    }

    private Space(StringWithOriginalPositions input, int stop, int start) {
        String whitespace = input.preprocessedText.substring(stop, start);
        int ostop = input.originalPositions[stop];
        int ostart = input.originalPositions[start];
        String w = input.originalText.substring(ostop, ostart);
        if(!whitespace.trim().equals("")) {
            System.out.println();
        }
        this.whitespace = whitespace;
    }

    @JsonCreator
    public static Space build(StringWithOriginalPositions input, int stop, int start) {
        //return flyweights.computeIfAbsent(whitespace, k -> new Space(whitespace));
        return new Space(input, stop, start);
    }

    public boolean isEmpty() {
        return this == EMPTY;
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

    public static Space format(String formatting) {
        throw new UnsupportedOperationException("Implement me!");
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
                //"comments=<" + (comments.size() == 1 ? "1 comment" : comments.size() + " comments") +
                "whitespace='" + printedWs + "')";
    }
}
