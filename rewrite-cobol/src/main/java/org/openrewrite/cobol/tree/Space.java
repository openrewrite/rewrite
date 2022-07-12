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
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;
import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static java.util.Collections.emptyList;
import static org.openrewrite.cobol.internal.StringWithOriginalPositions.quote;

/**
 *
 */
@EqualsAndHashCode
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public class Space {
    public static final Space EMPTY = new Space("");

    private final String rawText;

    List<String> whitespaces = new ArrayList<>();
    List<String> lineBreaks = new ArrayList<>();
    List<String> comments = new ArrayList<>();
    List<String> sequences = new ArrayList<>();
    String lastWhiteSpace;

    /*
     * Most occurrences of spaces will have no comments or markers and will be repeated frequently throughout a source file.
     * e.g.: a single space between keywords, or the common indentation of every line in a block.
     * So use flyweights to avoid storing many instances of functionally identical spaces
     */
    private static final Map<String, Space> flyweights = new WeakHashMap<>();

    private Space(String rawText) {
        this.rawText = rawText;
    }

    private Space(StringWithOriginalPositions input, int stop, int start) {
        String rawText = input.preprocessedText.substring(stop, start);

//        int ostop = input.originalPositions[stop];
//        int ostart = input.originalPositions[start];
//        String w = input.originalText.substring(ostop, ostart);
//        if(!whitespace.trim().equals("")) {
//            System.out.println();
//        }

        int current = 0;
        for(int i=0; i<rawText.length(); i++) {
            int from;
            int length;
            if(rawText.charAt(i)=='\r' && i+1 < rawText.length() && rawText.charAt(i+1)=='\n') {
                from = i;
                length = 2;
            } else if(rawText.charAt(i)=='\n') {
                from = i;
                length = 1;
            } else if(rawText.charAt(i)=='\r') {
                from = i;
                length = 1;
            } else {
                continue;
            }

            String whitespace = rawText.substring(current, from);
            String lineBreak = rawText.substring(from, from+length);
            String comment = input.lines.get(input.lineNumber).getCommentArea();
            String sequence = input.lines.get(input.lineNumber+1).getSequenceArea();

            whitespaces.add(whitespace);
            lineBreaks.add(lineBreak);
            comments.add(comment);
            sequences.add(sequence);

            current = from+length;
            input.lineNumber++;
        }
        lastWhiteSpace = rawText.substring(current);

        this.rawText = rawText;
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

    public String getRawText() {
        return rawText;
    }

    public String getWhitespace() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<whitespaces.size(); i++) {
            sb.append(whitespaces.get(i));
            sb.append(comments.get(i));
            sb.append(lineBreaks.get(i));
            sb.append(sequences.get(i));
        }
        sb.append(lastWhiteSpace);
        return sb.toString();
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
        StringBuilder sb = new StringBuilder();
        sb.append("Space(");
        for(int i=0; i<whitespaces.size(); i++) {
            sb.append("ws[" + i + "]=" + quote(whitespaces.get(i)) + "," );
            sb.append("comment[" + i + "]=" + quote(comments.get(i)) + "," );
            sb.append("break[" + i + "]=" + quote(lineBreaks.get(i)) + "," );
            sb.append("sequence[" + i + "]=" + quote(sequences.get(i)) + "," );
        }
        sb.append("last=" + quote(lastWhiteSpace));
        sb.append(")");
        return sb.toString();
    }
}
