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

import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.copyOfRange;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class StringUtils {
    private final static FileSystem FS = FileSystems.getDefault();

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
            switch (indent) {
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

        if (indentFrequencies.getOrDefault(0, 0L) > 1) {
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
     * <p>
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

    /**
     * Check if the String is empty string or null.
     *
     * @param string String to check
     * @return {@code true} if the String is null or empty string
     */
    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

    public static String readFully(InputStream inputStream) {
        try (InputStream is = inputStream) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }

            byte[] bytes = bos.toByteArray();
            return new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) +
                value.substring(1);
    }

    public static String uncapitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }


    /**
     * Given a prefix comprised of segments that are exclusively whitespace, single line comments, or multi-line comments,
     * return a list of each segment.
     * <p>
     * Operates on C-style comments:
     * // single line
     * /* multi-line
     * <p>
     * If the provided input contains anything except whitespace and c-style comments this will probably explode
     *
     * @param text The space to split
     * @return A list of raw whitespace, single line comments, and multi-line comments
     */
    public static List<String> splitCStyleComments(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.equals("")) {
            result.add("");
            return result;
        }
        int segmentStartIndex = 0;
        boolean inSingleLineComment = false;
        boolean inMultiLineComment = false;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            char previous = (i > 0) ? text.charAt(i - 1) : '\0';
            if (i == text.length() - 1) {
                result.add(text.substring(segmentStartIndex, i + 1));
                break;
            }

            if (inSingleLineComment && current == '\n') {
                result.add(text.substring(segmentStartIndex, i));
                segmentStartIndex = i;
                inSingleLineComment = false;
            } else if (inMultiLineComment && previous == '*' && current == '/') {
                result.add(text.substring(segmentStartIndex, i + 1));
                segmentStartIndex = i + 1;
                inMultiLineComment = false;
            } else if (!inMultiLineComment && !inSingleLineComment && previous == '/' && current == '/') {
                inSingleLineComment = true;
            } else if (!inMultiLineComment && !inSingleLineComment && previous == '/' && current == '*') {
                inMultiLineComment = true;
            }
        }
        return result;
    }

    /**
     * Return a copy of the supplied text that contains exactly the desired number of newlines appear before characters
     * that indicate the beginning of a C-style comment, "//" and "/*".
     * <p>
     * If the supplied text does not contain a comment indicator then this is equivalent to calling ensureNewlineCount()
     *
     * @param text                Original text to add newlines to
     * @param desiredNewlineCount The number of newlines that should be before the text
     * @return A copy of the supplied text with the desired number of newlines
     */
    public static String ensureNewlineCountBeforeComment(String text, int desiredNewlineCount) {
        StringBuilder result = new StringBuilder();
        int prefixEndsIndex = indexOfNonWhitespace(text);
        String suffix;
        if (prefixEndsIndex == -1) {
            prefixEndsIndex = text.length();
            suffix = "";
        } else {
            suffix = text.substring(prefixEndsIndex);
        }
        int newlinesSoFar = 0;
        for (int i = prefixEndsIndex - 1; i >= 0 && newlinesSoFar < desiredNewlineCount; i--) {
            char current = text.charAt(i);
            if (current == '\n') {
                newlinesSoFar++;
            }
            result.append(current);
        }
        while (newlinesSoFar < desiredNewlineCount) {
            result.append('\n');
            newlinesSoFar++;
        }
        // Since iterated back-to-front, reverse things back into the usual order
        result.reverse();
        result.append(suffix);
        return result.toString();
    }

    public static int indexOfNonWhitespace(String text) {
        return indexOf(text, it -> !(it == ' ' || it == '\t' || it == '\n' || it == '\r'));
    }

    /**
     * @param text Text to scan
     * @param test The predicate to match
     * @return The index of the first character for which the predicate returns <code>true</code>,
     * or <code>-1</code> if no character in the string matches the predicate.
     */
    public static int indexOf(String text, Predicate<Character> test) {
        for (int i = 0; i < text.length(); i++) {
            if (test.test(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return the number of times a substring occurs within a target string.
     *
     * @param text      A target string
     * @param substring The substring to search for
     * @return the number of times the substring is found in the target. 0 if no occurances are found.
     */
    public static int countOccurrences(@NonNull String text, @NonNull String substring) {

        if (text.isEmpty() || substring.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int index = text.indexOf(substring); index >= 0; index = text.indexOf(substring, index + substring.length())) {
            count++;
        }
        return count;
    }

    /**
     * This method will search and replace the first occurrence of a matching substring. There is a a replaceFirst method
     * on the String class but that version leverages regular expressions and is a magnitude slower than this simple
     * replacement.
     *
     * @param text        The source string to search
     * @param match       The substring that is being searched for
     * @param replacement The replacement.
     * @return The original string with the first occurrence replaced or the original text if a match is not found.
     */
    public static String replaceFirst(@NonNull String text, @NonNull String match, @NonNull String replacement) {
        int start = text.indexOf(match);
        if (match.isEmpty() || text.isEmpty() || start == -1) {
            return text;
        } else {
            StringBuilder newValue = new StringBuilder(text.length());
            newValue.append(text, 0, start);
            newValue.append(replacement);
            int end = start + match.length();
            if (end < text.length()) {
                newValue.append(text, end, text.length());
            }
            return newValue.toString();
        }
    }

    public static String repeat(String s, int count) {
        if (count == 1) {
            return s;
        }

        byte[] value = s.getBytes();
        int len = value.length;
        if (len == 0 || count == 0) {
            return "";
        }
        if (len == 1) {
            final byte[] single = new byte[count];
            Arrays.fill(single, value[0]);
            return new String(single);
        }
        int limit = len * count;
        byte[] multiple = new byte[limit];
        System.arraycopy(value, 0, multiple, 0, len);
        int copied = len;
        for (; copied < limit - copied; copied <<= 1) {
            System.arraycopy(multiple, 0, multiple, copied, copied);
        }
        System.arraycopy(multiple, 0, multiple, copied, limit - copied);
        return new String(multiple);
    }

    public static boolean matchesGlob(@Nullable String value, @Nullable String globPattern) {
        if ("*".equals(globPattern)) {
            return true;
        }
        if (null == globPattern) {
            return false;
        }
        if (null == value) {
            value = "";
        }
        PathMatcher pm = FS.getPathMatcher("glob:" + globPattern);
        Path path = Paths.get("");
        if (value.contains("/")) {
            String[] parts = value.split("/");
            if (parts.length > 1) {
                for (int i = 0, len = parts.length; i < len; i++) {
                    path = Paths.get("", copyOfRange(parts, i, parts.length));
                    if (!isBlank(parts[i])) {
                        break;
                    }
                }
            } else {
                path = Paths.get(parts[0]);
            }
        } else {
            path = Paths.get(value);
        }
        return pm.matches(path);
    }

    public static String indent(String text) {
        StringBuilder indent = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\n' || c == '\r') {
                return indent.toString();
            } else if (Character.isWhitespace(c)) {
                indent.append(c);
            } else {
                return indent.toString();
            }
        }
        return indent.toString();
    }

    /**
     * Locate the greatest common margin of a multi-line string
     *
     * @param multiline A string of one or more lines.
     * @return The greatest common margin consisting only of whitespace characters.
     */
    public static String greatestCommonMargin(String multiline) {
        String gcm = null;
        StringBuilder margin = new StringBuilder();
        boolean skipRestOfLine = false;
        char[] charArray = multiline.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (c == '\n') {
                if (i < charArray.length - 1 && charArray[i + 1] == '\n') {
                    i++;
                    continue;
                } else if(i > 0) {
                    if (margin.length() == 0) {
                        return "";
                    } else {
                        gcm = commonMargin(gcm, margin);
                        margin = new StringBuilder();
                    }
                }
                skipRestOfLine = false;
            } else if (Character.isWhitespace(c) && !skipRestOfLine) {
                margin.append(c);
            } else {
                skipRestOfLine = true;
            }
        }
        return gcm == null ? "" : gcm;
    }

    public static String commonMargin(@Nullable CharSequence s1, CharSequence s2) {
        if (s1 == null) {
            String s = s2.toString();
            return s.substring(s.lastIndexOf('\n') + 1);
        }
        for (int i = 0; i < s1.length() && i < s2.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i) || !Character.isWhitespace(s1.charAt(i))) {
                return s1.toString().substring(0, i);
            }
        }
        return s2.length() < s1.length() ? s2.toString() : s1.toString();
    }
}
