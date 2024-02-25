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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;

public class StringUtils {
    private StringUtils() {
    }

    public static String trimIndentPreserveCRLF(@Nullable String text) {
        if (text == null) {
            //noinspection DataFlowIssue
            return null;
        }
        return trimIndent((text.endsWith("\r\n") ? text.substring(0, text.length() - 2) : text)
                .replace('\r', '⏎'))
                .replace('⏎', '\r');
    }

    /**
     * Detects a common minimal indent of all the input lines and removes the indent from each line.
     * <p>
     * This is modeled after Kotlin's trimIndent and is useful for pruning the indent from multi-line text blocks.
     * <p>
     * Note: Blank lines do not affect the detected indent level.
     *
     * @param text A string that have a common indention
     * @return A mutated version of the string that removed the common indention.
     */
    public static String trimIndent(String text) {
        if (text.isEmpty()) {
            return text;
        }

        int indentLevel = minCommonIndentLevel(text);

        // The logic for trimming the start of the string is consistent with the functionality of Kotlin's trimIndent.
        char startChar = text.charAt(0);
        int start = 0;
        if (startChar == '\n' || startChar == '\r') {
            //If the string starts with a line break, always trim it.
            int i = 1;
            for (; i < text.length(); i++) {
                char c = text.charAt(i);
                if (!Character.isWhitespace(c)) {
                    //If there is any non-whitespace on the first line, do not trim the line.
                    start = 1;
                    break;
                } else if (c == '\n' || c == '\r') {
                    if (i - 1 <= indentLevel) {
                        //If the first line is only whitespace and the line size is less than indent size, trim it.
                        start = i;
                    } else {
                        //If the line size is equal or greater than indent, do not trim the line.
                        start = 1;
                    }
                    break;
                }
            }
        }

        //If the last line of the string is only whitespace, trim it.
        int end = text.length() - 1;
        while (end > start) {
            char endChar = text.charAt(end);
            if (!Character.isWhitespace(endChar)) {
                end = text.length();
                break;
            } else if (endChar == '\n' || endChar == '\r') {
                break;
            }
            end--;
        }
        if (end == start) {
            end++;
        }
        StringBuilder trimmed = new StringBuilder();
        for (int i = start; i < end; i++) {
            int j = i;
            for (; j < end; j++) {
                char c = text.charAt(j);
                if (c == '\r' || c == '\n') {
                    trimmed.append(c);
                    break;
                }
                if (j - i >= indentLevel) {
                    trimmed.append(c);
                }
            }
            i = j;
        }

        return trimmed.toString();
    }


    /**
     * This method will count the number of white space characters that precede any content for each line contained
     * in string. It will not compute a white space count for a line, if the entire line is blank (only made up of white
     * space characters).
     * <P><P>
     * It will compute the minimum common number of white spaces across all lines and return that minimum.
     *
     * @param text A string with zero or more line breaks.
     * @return The minimum count of white space characters preceding each line of content.
     */
    private static int minCommonIndentLevel(String text) {
        int minIndent = Integer.MAX_VALUE;
        int whiteSpaceCount = 0;
        boolean contentEncountered = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                if (contentEncountered) {
                    minIndent = Math.min(whiteSpaceCount, minIndent);
                    if (minIndent == 0) {
                        break;
                    }
                }
                whiteSpaceCount = 0;
                contentEncountered = false;
            } else if (!contentEncountered && Character.isWhitespace(c)) {
                whiteSpaceCount++;
            } else {
                contentEncountered = true;
            }
        }
        if (contentEncountered) {
            minIndent = Math.min(whiteSpaceCount, minIndent);
        }
        return minIndent;
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

    public static boolean isNotEmpty(@Nullable String string) {
        return string != null && !string.isEmpty();
    }

    public static String readFully(@Nullable InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        return readFully(inputStream, StandardCharsets.UTF_8);
    }

    /**
     * If the input stream is coming from a stream with an unknown encoding, use
     * {@link EncodingDetectingInputStream#readFully()} instead.
     *
     * @param inputStream An input stream.
     * @return the full contents of the input stream interpreted as a string of the specified encoding
     */
    public static String readFully(InputStream inputStream, Charset charset) {
        try (InputStream is = inputStream) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }

            byte[] bytes = bos.toByteArray();
            return new String(bytes, charset);
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

    public static boolean containsOnlyWhitespaceAndComments(String text) {
        int i = 0;
        char[] chars = text.toCharArray();
        boolean inSingleLineComment = false;
        boolean inMultilineComment = false;
        while (i < chars.length) {
            char c = chars[i];
            if (inSingleLineComment && c == '\n') {
                inSingleLineComment = false;
                continue;
            }
            if (i < chars.length - 1) {
                String s = String.valueOf(c) + chars[i + 1];
                switch (s) {
                    case "//": {
                        inSingleLineComment = true;
                        i += 2;
                        continue;
                    }
                    case "/*": {
                        inMultilineComment = true;
                        i += 2;
                        continue;
                    }
                    case "*/": {
                        inMultilineComment = false;
                        i += 2;
                        continue;
                    }
                }
            }
            if (!inSingleLineComment && !inMultilineComment && !Character.isWhitespace(c)) {
                return false;
            }
            i++;
        }
        return true;
    }

    public static int indexOfNonWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!(c == ' ' || c == '\t' || c == '\n' || c == '\r')) {
                return i;
            }
        }
        return -1;
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
     * @return the number of times the substring is found in the target. 0 if no occurrences are found.
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
        if (globPattern == null) {
            return false;
        }
        if (value == null) {
            value = "";
        }

        return matchesGlob(
                globPattern.replace(wrongFileSeparatorChar, File.separatorChar),
                value.replace(wrongFileSeparatorChar, File.separatorChar),
                false
        );
    }

    private static final char wrongFileSeparatorChar = File.separatorChar == '/' ? '\\' : '/';

    private static boolean matchesGlob(String pattern, String str, boolean caseSensitive) {
        int patIdxStart = 0;
        int patIdxEnd = pattern.length() - 1;
        int strIdxStart = 0;
        int strIdxEnd = str.length() - 1;

        if (!pattern.contains("*")) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return false; // Pattern and string do not have the same size
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                char ch = pattern.charAt(i);
                if (ch != '?' && different(caseSensitive, ch, str.charAt(i))) {
                    return false; // Character mismatch
                }
            }
            return true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            char ch = pattern.charAt(patIdxStart);
            if (ch == '*') {
                break;
            }
            if (ch != '?'
                && different(caseSensitive, ch, str.charAt(strIdxStart))) {
                return false; // Character mismatch
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            return allStars(pattern, patIdxStart, patIdxEnd);
        } else if (patIdxStart > patIdxEnd) {
            // String not exhausted by pattern is. Failure
            return false;
        }

        // Process characters after last star
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            char ch = pattern.charAt(patIdxEnd);
            if (ch == '*') {
                break;
            }
            if (ch != '?' && different(caseSensitive, ch, str.charAt(strIdxEnd))) {
                return false; // Character mismatch
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            return allStars(pattern, patIdxStart, patIdxEnd);
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (pattern.charAt(i) == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    char ch = pattern.charAt(patIdxStart + j + 1);
                    if (ch != '?' && different(caseSensitive, ch, str.charAt(strIdxStart + i + j))) {
                        continue strLoop;
                    }
                }
                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }
            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        return allStars(pattern, patIdxStart, patIdxEnd);
    }

    private static boolean allStars(String chars, int start, int end) {
        for (int i = start; i <= end; ++i) {
            if (chars.charAt(i) != '*') {
                return false;
            }
        }
        return true;
    }

    private static boolean different(boolean caseSensitive, char ch, char other) {
        return caseSensitive
                ? ch != other
                : Character.toUpperCase(ch) != Character.toUpperCase(other);
    }

    public static String indent(String text) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
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
                } else if (i > 0) {
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

    public static boolean isNumeric(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * See <a href="https://eclipse.org/aspectj/doc/next/progguide/semantics-pointcuts.html#type-patterns">https://eclipse.org/aspectj/doc/next/progguide/semantics-pointcuts.html#type-patterns</a>
     * <p>
     * An embedded * in an identifier matches any sequence of characters, but
     * does not match the package (or inner-type) separator ".".
     * <p>
     * The ".." wildcard matches any sequence of characters that start and end with a ".", so it can be used to pick out all
     * types in any subpackage, or all inner types. e.g. <code>within(com.xerox..*)</code> picks out all join points where
     * the code is in any declaration of a type whose name begins with "com.xerox.".
     */
    public static String aspectjNameToPattern(String name) {
        int length = name.length();
        StringBuilder sb = new StringBuilder(length);
        char prev = 0;
        for (int i = 0; i < length; i++) {
            boolean isLast = i == length - 1;
            char c = name.charAt(i);
            switch (c) {
                case '.':
                    if (prev != '.' && (isLast || name.charAt(i + 1) != '.')) {
                        sb.append("[.$]");
                    } else if (prev == '.') {
                        sb.append("\\.(.+\\.)?");
                    }
                    break;
                case '*':
                    sb.append("[^.]*");
                    break;
                case '$':
                case '[':
                case ']':
                    sb.append('\\');
                    // fall-through
                default:
                    sb.append(c);
            }
            prev = c;
        }
        return sb.toString();
    }

    /**
     * @param s1 first string
     * @param s2 second string
     * @return length of the longest substring common to both strings
     * @see <a href="https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Longest_common_substring#Java_-_O(n)_storage"></a>
     */
    public static int greatestCommonSubstringLength(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0;
        }

        int m = s1.length();
        int n = s2.length();
        int cost;
        int maxLen = 0;
        int[] p = new int[n];
        int[] d = new int[n];

        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                // calculate cost/score
                if (s1.charAt(i) != s2.charAt(j)) {
                    cost = 0;
                } else {
                    if ((i == 0) || (j == 0)) {
                        cost = 1;
                    } else {
                        cost = p[j - 1] + 1;
                    }
                }
                d[j] = cost;

                if (cost > maxLen) {
                    maxLen = cost;
                }
            } // for {}

            int[] swap = p;
            p = d;
            d = swap;
        }

        return maxLen;
    }

    /**
     * @return Considering C-style comments to be whitespace, return the index of the next non-whitespace character.
     */
    public static int indexOfNextNonWhitespace(int cursor, String source) {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int length = source.length();
        for (; cursor < length; cursor++) {
            char current = source.charAt(cursor);
            if (inSingleLineComment) {
                inSingleLineComment = current != '\n';
                continue;
            } else if (length > cursor + 1) {
                char next = source.charAt(cursor + 1);
                if (current == '/' && next == '/') {
                    inSingleLineComment = true;
                    cursor++;
                    continue;
                } else if (current == '/' && next == '*') {
                    inMultiLineComment = true;
                    cursor++;
                    continue;
                } else if (current == '*' && next == '/') {
                    inMultiLineComment = false;
                    cursor++;
                    continue;
                }
            }
            if (!inMultiLineComment && !Character.isWhitespace(current)) {
                break; // found it!
            }
        }
        return cursor;
    }

    public static String formatUriForPropertiesFile(String uri) {
        return uri.replaceAll("(?<!\\\\)://", "\\\\://");
    }
}
