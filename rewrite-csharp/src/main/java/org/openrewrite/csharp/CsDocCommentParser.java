/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp;

import org.openrewrite.Tree;
import org.openrewrite.csharp.tree.CsDocComment;
import org.openrewrite.csharp.tree.CsDocCommentRawComment;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses raw XML documentation comment text into a structured {@link CsDocComment.DocComment} tree.
 * <p>
 * The raw text comes from the C# parser where consecutive {@code ///} lines are grouped
 * into a single text block. The text starts with {@code /} (since the C# parser strips
 * the first {@code //}), and continuation lines include their {@code ///} prefixes.
 */
public class CsDocCommentParser {

    /**
     * Parse a raw doc comment into a structured tree.
     *
     * @param rawComment the raw comment from the C# parser
     * @return a structured DocComment tree
     */
    public static CsDocComment.DocComment parse(CsDocCommentRawComment rawComment) {
        String text = rawComment.getText();
        List<CsDocComment> body = parseContent(text);
        return new CsDocComment.DocComment(
                Tree.randomId(),
                rawComment.getMarkers(),
                body,
                rawComment.getSuffix()
        );
    }

    /**
     * Parse the XML content of a documentation comment into a list of CsDocComment nodes.
     */
    private static List<CsDocComment> parseContent(String text) {
        List<CsDocComment> nodes = new ArrayList<>();
        int pos = 0;
        int len = text.length();

        while (pos < len) {
            char c = text.charAt(pos);

            if (c == '<') {
                // Check for closing tag
                if (pos + 1 < len && text.charAt(pos + 1) == '/') {
                    // This is a closing tag — stop parsing content (caller handles it)
                    break;
                }
                // Parse opening or self-closing XML element
                int tagEnd = findTagEnd(text, pos);
                if (tagEnd < 0) {
                    // Malformed — treat rest as text
                    nodes.add(xmlText(text.substring(pos)));
                    pos = len;
                    break;
                }
                String tagContent = text.substring(pos + 1, tagEnd);
                boolean selfClosing = tagContent.endsWith("/");
                if (selfClosing) {
                    tagContent = tagContent.substring(0, tagContent.length() - 1);
                }

                String tagName = extractTagName(tagContent);
                List<CsDocComment> attributes = parseAttributes(tagContent, tagName.length());

                if (selfClosing) {
                    nodes.add(new CsDocComment.XmlEmptyElement(
                            Tree.randomId(), Markers.EMPTY,
                            tagName, attributes, Collections.emptyList()
                    ));
                } else {
                    // Parse content until matching closing tag
                    int contentStart = tagEnd + 1;
                    int closingTagStart = findClosingTag(text, contentStart, tagName);

                    List<CsDocComment> content;
                    int afterClosingTag;
                    if (closingTagStart < 0) {
                        // No closing tag found — treat remaining as content
                        content = parseContent(text.substring(contentStart));
                        afterClosingTag = len;
                    } else {
                        String innerText = text.substring(contentStart, closingTagStart);
                        content = parseContent(innerText);
                        // Skip past </tagName>
                        afterClosingTag = text.indexOf('>', closingTagStart) + 1;
                        if (afterClosingTag == 0) afterClosingTag = len;
                    }

                    nodes.add(new CsDocComment.XmlElement(
                            Tree.randomId(), Markers.EMPTY,
                            tagName, attributes,
                            Collections.emptyList(), // spaceBeforeClose
                            content,
                            Collections.emptyList()  // closingTagSpaceBeforeClose
                    ));
                    pos = afterClosingTag;
                    continue;
                }
                pos = tagEnd + 1;
            } else if (c == '\n' || c == '\r') {
                // Parse line break with margin
                int lineBreakEnd = pos;
                StringBuilder margin = new StringBuilder();
                margin.append(c);
                lineBreakEnd++;
                if (c == '\r' && lineBreakEnd < len && text.charAt(lineBreakEnd) == '\n') {
                    margin.append('\n');
                    lineBreakEnd++;
                }
                // Consume whitespace and /// prefix
                while (lineBreakEnd < len && (text.charAt(lineBreakEnd) == ' ' || text.charAt(lineBreakEnd) == '\t')) {
                    margin.append(text.charAt(lineBreakEnd));
                    lineBreakEnd++;
                }
                // Check for /// prefix (stored as "///" in the raw text from grouped comments)
                if (lineBreakEnd + 2 < len &&
                    text.charAt(lineBreakEnd) == '/' &&
                    text.charAt(lineBreakEnd + 1) == '/' &&
                    text.charAt(lineBreakEnd + 2) == '/') {
                    margin.append("///");
                    lineBreakEnd += 3;
                }
                nodes.add(new CsDocComment.LineBreak(Tree.randomId(), margin.toString(), Markers.EMPTY));
                pos = lineBreakEnd;
            } else {
                // Parse text content until next tag or line break
                int textEnd = pos;
                while (textEnd < len && text.charAt(textEnd) != '<' &&
                       text.charAt(textEnd) != '\n' && text.charAt(textEnd) != '\r') {
                    // Also stop at </ (closing tag of parent)
                    textEnd++;
                }
                if (textEnd > pos) {
                    nodes.add(xmlText(text.substring(pos, textEnd)));
                }
                pos = textEnd;
            }
        }
        return nodes;
    }

    private static CsDocComment.XmlText xmlText(String text) {
        return new CsDocComment.XmlText(Tree.randomId(), Markers.EMPTY, text);
    }

    /**
     * Find the position of '>' that closes the opening tag starting at pos.
     * Handles quoted attribute values.
     */
    private static int findTagEnd(String text, int pos) {
        boolean inQuote = false;
        char quoteChar = 0;
        for (int i = pos + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuote) {
                if (c == quoteChar) inQuote = false;
            } else if (c == '"' || c == '\'') {
                inQuote = true;
                quoteChar = c;
            } else if (c == '>') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Extract the tag name from tag content (everything after '<' and before '>').
     */
    private static String extractTagName(String tagContent) {
        int end = 0;
        while (end < tagContent.length() && !Character.isWhitespace(tagContent.charAt(end))) {
            end++;
        }
        return tagContent.substring(0, end);
    }

    /**
     * Parse attributes from tag content starting after the tag name.
     */
    private static List<CsDocComment> parseAttributes(String tagContent, int offset) {
        List<CsDocComment> attrs = new ArrayList<>();
        int pos = offset;
        int len = tagContent.length();

        while (pos < len) {
            // Skip whitespace
            int wsStart = pos;
            while (pos < len && Character.isWhitespace(tagContent.charAt(pos))) {
                pos++;
            }
            if (pos >= len) break;

            String leadingSpace = tagContent.substring(wsStart, pos);

            // Parse attribute name
            int nameStart = pos;
            while (pos < len && tagContent.charAt(pos) != '=' &&
                   !Character.isWhitespace(tagContent.charAt(pos)) &&
                   tagContent.charAt(pos) != '/' && tagContent.charAt(pos) != '>') {
                pos++;
            }
            if (pos == nameStart) break;
            String attrName = tagContent.substring(nameStart, pos);

            // Skip whitespace before =
            while (pos < len && Character.isWhitespace(tagContent.charAt(pos))) {
                pos++;
            }

            if (pos >= len || tagContent.charAt(pos) != '=') {
                // Attribute without value
                CsDocComment.XmlText spaceText = xmlText(leadingSpace);
                attrs.add(spaceText);
                attrs.add(new CsDocComment.XmlAttribute(
                        Tree.randomId(), Markers.EMPTY,
                        attrName, null, null
                ));
                continue;
            }
            pos++; // skip '='

            // Skip whitespace after =
            while (pos < len && Character.isWhitespace(tagContent.charAt(pos))) {
                pos++;
            }

            // Parse quoted value
            String value = "";
            if (pos < len && (tagContent.charAt(pos) == '"' || tagContent.charAt(pos) == '\'')) {
                char q = tagContent.charAt(pos);
                int valueStart = pos;
                pos++; // skip opening quote
                while (pos < len && tagContent.charAt(pos) != q) {
                    pos++;
                }
                if (pos < len) pos++; // skip closing quote
                value = tagContent.substring(valueStart, pos);
            }

            // Create the appropriate attribute type
            CsDocComment.XmlText spaceText = xmlText(leadingSpace);
            List<CsDocComment> valueList = Collections.singletonList(xmlText(value));

            if ("cref".equals(attrName)) {
                attrs.add(spaceText);
                attrs.add(new CsDocComment.XmlCrefAttribute(
                        Tree.randomId(), Markers.EMPTY,
                        null, valueList, null // reference resolved later
                ));
            } else if ("name".equals(attrName)) {
                attrs.add(spaceText);
                attrs.add(new CsDocComment.XmlNameAttribute(
                        Tree.randomId(), Markers.EMPTY,
                        null, valueList, null // paramName resolved later
                ));
            } else {
                attrs.add(spaceText);
                attrs.add(new CsDocComment.XmlAttribute(
                        Tree.randomId(), Markers.EMPTY,
                        attrName, null, valueList
                ));
            }
        }
        return attrs;
    }

    /**
     * Find the start of the closing tag {@code </tagName>} for the given tag name.
     * Handles nested elements with the same tag name.
     */
    private static int findClosingTag(String text, int start, String tagName) {
        int depth = 1;
        int pos = start;
        int len = text.length();

        while (pos < len) {
            int nextTag = text.indexOf('<', pos);
            if (nextTag < 0) return -1;

            if (nextTag + 1 < len && text.charAt(nextTag + 1) == '/') {
                // Closing tag
                int nameStart = nextTag + 2;
                int nameEnd = nameStart;
                while (nameEnd < len && text.charAt(nameEnd) != '>' &&
                       !Character.isWhitespace(text.charAt(nameEnd))) {
                    nameEnd++;
                }
                String closingName = text.substring(nameStart, nameEnd);
                if (closingName.equals(tagName)) {
                    depth--;
                    if (depth == 0) {
                        return nextTag;
                    }
                }
                pos = text.indexOf('>', nextTag);
                if (pos < 0) return -1;
                pos++;
            } else {
                // Opening or self-closing tag
                int tagEnd = findTagEnd(text, nextTag);
                if (tagEnd < 0) return -1;
                String content = text.substring(nextTag + 1, tagEnd);
                boolean selfClosing = content.endsWith("/");
                String name = extractTagName(selfClosing ? content.substring(0, content.length() - 1) : content);
                if (!selfClosing && name.equals(tagName)) {
                    depth++;
                }
                pos = tagEnd + 1;
            }
        }
        return -1;
    }
}
