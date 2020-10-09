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
package org.openrewrite.internal;

import io.micrometer.core.lang.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class StringUtils {
    private StringUtils() {
    }

    public static String trimIndent(String text) {
        int indentLevel = indentLevel(text);

        StringBuilder trimmed = new StringBuilder();
        AtomicBoolean dropWhile = new AtomicBoolean(false);
        int[] charArray = text.replaceAll("\\s+$", "").chars()
                .filter(c -> {
                    dropWhile.set(dropWhile.get() || !(c == '\n' || c == '\r'));
                    return dropWhile.get();
                })
                .toArray();

        for (int i = 0; i < charArray.length; i++) {
            boolean nonWhitespaceEncountered = false;
            int j = i;
            for (; j < charArray.length; j++) {
                int c = charArray[j];
                if (j - i >= indentLevel || (nonWhitespaceEncountered |= !Character.isWhitespace(c))) {
                    trimmed.append((char) c);
                }
                if (c == '\r' || c == '\n') {
                    break;
                }
            }
            i = j;
        }

        return trimmed.toString();
    }

    static int indentLevel(String text) {
        Stream<String> lines = Arrays.stream(text.replaceAll("\\s+$", "").split("\\r?\\n"));

        AtomicBoolean dropWhile = new AtomicBoolean(false);
        AtomicBoolean takeWhile = new AtomicBoolean(true);
        SortedMap<Integer, Long> indentFrequencies = lines
                .filter(l -> {
                    dropWhile.set(dropWhile.get() || !l.isEmpty());
                    return dropWhile.get();
                })
                .map(l -> {
                    takeWhile.set(true);
                    return (int) l.chars()
                            .filter(c -> {
                                takeWhile.set(takeWhile.get() && Character.isWhitespace(c));
                                return takeWhile.get();
                            })
                            .count();
                })
                .collect(groupingBy(identity(), TreeMap::new, counting()));
        return mostCommonIndent(indentFrequencies);
    }

    public static int mostCommonIndent(SortedMap<Integer, Long> indentFrequencies) {
        // the frequency with which each indent level is an integral divisor of longer indent levels
        SortedMap<Integer, Integer> indentFrequencyAsDivisors = new TreeMap<>();
        for (Map.Entry<Integer, Long> indentFrequency : indentFrequencies.entrySet()) {
            int indent = indentFrequency.getKey();
            int freq;
            switch(indent) {
                case 0:
                    freq = indentFrequency.getValue().intValue();
                    break;
                case 1:
                    // gcd(1, N) == 1, so we can avoid the test for this case
                    freq = (int) indentFrequencies.tailMap(indent).values().stream().mapToLong(l -> l).sum();
                    break;
                default:
                    freq = (int) indentFrequencies.tailMap(indent).entrySet().stream()
                            .filter(inF -> gcd(inF.getKey(), indent) != 0)
                            .mapToLong(Map.Entry::getValue)
                            .sum();
            }

            indentFrequencyAsDivisors.put(indent, freq);
        }

        if(indentFrequencies.getOrDefault(0, 0L) > 1) {
            return 0;
        }

        return indentFrequencyAsDivisors.entrySet().stream()
                .max((e1, e2) -> {
                    int valCompare = e1.getValue().compareTo(e2.getValue());
                    return valCompare != 0 ?
                            valCompare :
                            // take the smallest indent otherwise, unless it would be zero
                            e1.getKey() == 0 ? -1 : e2.getKey().compareTo(e1.getKey());
                })
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    static int gcd(int n1, int n2) {
        return n2 == 0 ? n1 : gcd(n2, n1 % n2);
    }

    /**
     * Check if the String is null or has only whitespaces.
     *
     * Modified from apache commons lang StringUtils.
     *
     * @param string String to check
     * @return {@code true} if the String is null or has only whitespaces
     */
    public static boolean isBlank(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return true;
        }
        for (int i = 0; i < string.length(); i++) {
            if (!Character.isWhitespace(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String readFully(InputStream inputStream) {
        try(InputStream is = inputStream) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }

            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) +
                value.substring(1);
    }

    public static String uncapitalize(String value) {
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }


    /**
     * Given a prefix comprised of segments that are exclusively whitespace, single line comments, or multi-line comments,
     * return a list of each segment.
     *
     * Operates on C-style comments:
     *  // single line
     *  /* multi-line
     *
     * If the provided input contains anything except whitespace and c-style comments this will probably explode
     */
    public static List<String> splitCStyleComments(String text) {
        List<String> result = new ArrayList<>();
        if(text == null || text.equals("")) {
            result.add("");
            return result;
        }
        int segmentStartIndex = 0;
        boolean inSingleLineComment = false;
        boolean inMultiLineComment = false;
        for(int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            char previous = (i > 0) ? text.charAt(i - 1) : '\0';
            if(i == text.length() - 1) {
                result.add(text.substring(segmentStartIndex, i + 1));
                break;
            }

            if(inSingleLineComment && current == '\n') {
                result.add(text.substring(segmentStartIndex, i));
                segmentStartIndex = i;
                inSingleLineComment = false;
            } else if(inMultiLineComment && previous == '*' && current == '/') {
                result.add(text.substring(segmentStartIndex, i + 1));
                segmentStartIndex = i + 1;
                inMultiLineComment = false;
            } else if(!inMultiLineComment && !inSingleLineComment && previous == '/' && current == '/') {
                inSingleLineComment = true;
            } else if(!inMultiLineComment && !inSingleLineComment && previous == '/' && current == '*') {
                inMultiLineComment = true;
            }
        }
        return result;
    }

    /**
     * Return a copy of the supplied text that contains exactly the desired number of newline characters.
     * If the text has more than the requested number of newlines, newlines after the desired count is reached are removed.
     * If the text has fewer than the requested number of newlines, additional newlines are prepended to the text.
     */
    public static String ensureNewlineCount(String text, int desiredNewlineCount) {
        int newlinesSoFar = 0;
        StringBuilder result = new StringBuilder();
        for(int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if(current == '\n') {
                newlinesSoFar++;
                if(newlinesSoFar > desiredNewlineCount) {
                    continue;
                }
            }
            result.append(current);
        }
        while(newlinesSoFar < desiredNewlineCount) {
            result.insert(0, '\n');
            newlinesSoFar++;
        }

        return result.toString();
    }

    /**
     * Return a copy of the supplied text that contains exactly the desired number of newlines appear before characters
     * that indicate the beginning of a C-style comment, "//" and "/*".
     *
     * If the supplied text does not contain a comment indicator then this is equivalent to calling ensureNewlineCount()
     */
    public static String ensureNewlineCountBeforeComment(String text, int desiredNewlineCount) {
        int newlinesSoFar = 0;
        StringBuilder result = new StringBuilder();
        boolean foundComment = false;
        for(int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            char previous = (i > 0) ? text.charAt(i - 1) : '\0';
            if(current == '\n' && !foundComment) {
                newlinesSoFar++;
                if(newlinesSoFar > desiredNewlineCount) {
                    continue;
                }
            } else if((previous == '/' && current == '/') || (previous == '/' && current == '*')) {
                foundComment = true;
            }
            result.append(current);
        }
        while(newlinesSoFar < desiredNewlineCount) {
            result.insert(0, '\n');
            newlinesSoFar++;
        }

        return result.toString();
    }

}
