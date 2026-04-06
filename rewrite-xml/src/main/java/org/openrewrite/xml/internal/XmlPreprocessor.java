/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.xml.internal;

/**
 * Pre-scans XML source to identify {@code <name>} sequences that are not actual XML tags
 * and replaces their {@code <} with a sentinel character so the ANTLR lexer treats them as text.
 * <p>
 * This handles malformed XML where bare {@code <} appears in text content, e.g.:
 * {@code <baz>some element value <other></baz>}
 * <p>
 * The sentinel character {@code \uFDD0} is a Unicode noncharacter reserved for internal use
 * and will never appear in valid XML content.
 */
public class XmlPreprocessor {

    /**
     * Sentinel character used to replace non-tag {@code <} in the source.
     * Unicode noncharacter U+FDD0, guaranteed not to appear in well-formed text.
     */
    public static final char SENTINEL = '\uFDD0';

    /**
     * Pre-process the XML source, replacing {@code <} for non-tag sequences with the sentinel.
     *
     * @param source the raw XML source
     * @return the preprocessed source with non-tag {@code <} replaced
     */
    public static String preprocess(String source) {
        if (source.isEmpty()) {
            return source;
        }

        // First pass: collect all tag open positions and their names, determine which are real tags.
        // A "real" open tag <name> has a matching </name> or is self-closing <name/>.
        // We do this by scanning for all potential tags and using a stack-based matching algorithm.

        int len = source.length();
        // Track positions of '<' that should be replaced with sentinel
        boolean[] replacePositions = new boolean[len];
        // Stack of open tag entries: [position, nameStart, nameEnd]
        int[][] stack = new int[256][3];
        int stackSize = 0;

        int i = 0;
        while (i < len) {
            char c = source.charAt(i);

            // Skip CDATA sections
            if (c == '<' && i + 8 < len && source.startsWith("<![CDATA[", i)) {
                int end = source.indexOf("]]>", i + 9);
                if (end >= 0) {
                    i = end + 3;
                    continue;
                }
            }

            // Skip comments
            if (c == '<' && i + 3 < len && source.startsWith("<!--", i)) {
                int end = source.indexOf("-->", i + 4);
                if (end >= 0) {
                    i = end + 3;
                    continue;
                }
            }

            // Skip processing instructions
            if (c == '<' && i + 1 < len && source.charAt(i + 1) == '?') {
                int end = source.indexOf("?>", i + 2);
                if (end >= 0) {
                    i = end + 2;
                    continue;
                }
            }

            // Skip DTD declarations
            if (c == '<' && i + 1 < len && source.charAt(i + 1) == '!') {
                // Already handled CDATA and comments above, so this is DOCTYPE or other markup declaration
                int end = indexOf(source, '>', i + 2);
                if (end >= 0) {
                    i = end + 1;
                    continue;
                }
            }

            // JSP elements: <%...%>
            if (c == '<' && i + 1 < len && source.charAt(i + 1) == '%') {
                int end = source.indexOf("%>", i + 2);
                if (end >= 0) {
                    i = end + 2;
                    continue;
                }
            }

            // Close tag </name>
            if (c == '<' && i + 1 < len && source.charAt(i + 1) == '/') {
                int nameStart = i + 2;
                int nameEnd = scanName(source, nameStart, len);
                if (nameEnd > nameStart) {
                    String closeName = source.substring(nameStart, nameEnd);
                    // Pop stack until we find matching open tag; mark unmatched ones for replacement
                    boolean found = false;
                    for (int s = stackSize - 1; s >= 0; s--) {
                        String openName = source.substring(stack[s][1], stack[s][2]);
                        if (openName.equals(closeName)) {
                            // Found match — mark everything above it as non-tags
                            for (int u = stackSize - 1; u > s; u--) {
                                replacePositions[stack[u][0]] = true;
                            }
                            stackSize = s;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // Close tag with no matching open — skip it (the parser will handle the error)
                    }
                    // Advance past close tag
                    int closeEnd = indexOf(source, '>', nameEnd);
                    if (closeEnd >= 0) {
                        i = closeEnd + 1;
                    } else {
                        i = nameEnd;
                    }
                    continue;
                }
            }

            // Open tag <name or self-closing <name.../>
            if (c == '<' && i + 1 < len && isNameStartChar(source.charAt(i + 1))) {
                int nameStart = i + 1;
                int nameEnd = scanName(source, nameStart, len);
                if (nameEnd > nameStart) {
                    // Scan forward to see if this is self-closing or a normal open tag
                    int tagEnd = scanToTagEnd(source, nameEnd, len);
                    if (tagEnd >= 0) {
                        boolean selfClosing = tagEnd > 0 && source.charAt(tagEnd - 1) == '/';
                        if (!selfClosing) {
                            // Push onto stack
                            if (stackSize >= stack.length) {
                                int[][] newStack = new int[stack.length * 2][3];
                                System.arraycopy(stack, 0, newStack, 0, stack.length);
                                stack = newStack;
                            }
                            stack[stackSize][0] = i;
                            stack[stackSize][1] = nameStart;
                            stack[stackSize][2] = nameEnd;
                            stackSize++;
                        }
                        i = tagEnd + 1;
                        continue;
                    }
                    // If we can't find '>' this looks like a non-tag '<' — mark it
                    replacePositions[i] = true;
                    i = nameEnd;
                    continue;
                }
            }

            i++;
        }

        // Anything remaining on the stack at the end is unmatched — mark for replacement
        for (int s = 0; s < stackSize; s++) {
            replacePositions[stack[s][0]] = true;
        }

        // Check if any replacements are needed
        boolean anyReplacements = false;
        for (int p = 0; p < len; p++) {
            if (replacePositions[p]) {
                anyReplacements = true;
                break;
            }
        }
        if (!anyReplacements) {
            return source;
        }

        // Build the result with sentinels
        StringBuilder sb = new StringBuilder(len);
        for (int p = 0; p < len; p++) {
            if (replacePositions[p]) {
                sb.append(SENTINEL);
            } else {
                sb.append(source.charAt(p));
            }
        }
        return sb.toString();
    }

    /**
     * Scan a Name starting at the given position, per XML spec NameStartChar (NameChar)*
     */
    private static int scanName(String source, int start, int len) {
        if (start >= len || !isNameStartChar(source.charAt(start))) {
            return start;
        }
        int i = start + 1;
        while (i < len && isNameChar(source.charAt(i))) {
            i++;
        }
        return i;
    }

    /**
     * Scan forward from position to find the closing '>' of a tag, handling quoted attributes.
     * Returns the index of '>', or -1 if not found before end of source or another '<'.
     */
    private static int scanToTagEnd(String source, int start, int len) {
        int i = start;
        while (i < len) {
            char c = source.charAt(i);
            if (c == '>') {
                return i;
            }
            if (c == '<') {
                // Hit another tag open — this isn't a valid tag
                return -1;
            }
            // Skip quoted attribute values
            if (c == '"') {
                i++;
                while (i < len && source.charAt(i) != '"') {
                    i++;
                }
            } else if (c == '\'') {
                i++;
                while (i < len && source.charAt(i) != '\'') {
                    i++;
                }
            }
            i++;
        }
        return -1;
    }

    private static int indexOf(String source, char target, int from) {
        return source.indexOf(target, from);
    }

    private static boolean isNameStartChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == ':' ||
                (c >= '\u00C0' && c <= '\u00D6') ||
                (c >= '\u00D8' && c <= '\u00F6') ||
                (c >= '\u00F8' && c <= '\u02FF') ||
                (c >= '\u0370' && c <= '\u037D') ||
                (c >= '\u037F' && c <= '\u1FFF') ||
                (c >= '\u200C' && c <= '\u200D') ||
                (c >= '\u2070' && c <= '\u218F') ||
                (c >= '\u3001' && c <= '\uD7FF') ||
                (c >= '\uF900' && c <= '\uFDCF') ||
                (c >= '\uFDF0' && c <= '\uFFFD');
    }

    private static boolean isNameChar(char c) {
        return isNameStartChar(c) || c == '-' || c == '.' ||
                (c >= '0' && c <= '9') ||
                c == '\u00B7' ||
                (c >= '\u0300' && c <= '\u036F') ||
                (c >= '\u203F' && c <= '\u2040');
    }
}
