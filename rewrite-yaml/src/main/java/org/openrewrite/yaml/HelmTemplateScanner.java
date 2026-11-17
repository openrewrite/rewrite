/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.yaml;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the template actions in a YAML source file, such as Helm's {@code {{ .Values.name }}}, so the parser can
 * set them aside before SnakeYAML reads the file.
 * <p>
 * Helm renders charts with Go's {@code text/template}, and every action Go accepts ends here where Go ends it:
 * <ul>
 *     <li>An action opens at any "{{" and closes at the first "}}" outside a string, raw string or character
 *     literal, so it may span lines.</li>
 *     <li>A double-quoted string or character literal, which may hold backslash escapes, closes on the line it
 *     opens. A raw string, in backticks, may span lines.</li>
 *     <li>A comment opens with a slash and asterisk straight after "{{" (or after a "-" and whitespace), ends at
 *     the first asterisk and slash, and must close with "}}", optionally preceded by whitespace and a "-".</li>
 * </ul>
 * YAML files also carry other syntax in "{{ }}", such as GitHub Actions expressions, Jinja and Mustache, which Go
 * would reject. So the scanner does not check that an action is well-formed Go; it reads what Go would reject in
 * the way these syntaxes intend:
 * <ul>
 *     <li>Names, numbers and punctuation are not checked, so {@code matrix.java-version}, {@code count-1} and
 *     Jinja's {@code {{-name-}}} are accepted.</li>
 *     <li>A quote that does not close on its line, such as the apostrophe in {@code {{! don't }}}, is an ordinary
 *     character, as is a backtick with no closing backtick after it.</li>
 * </ul>
 * An action that spans lines is rejected if it holds a colon followed by whitespace or a line break outside its
 * strings and literals, which Go never accepts. That is the shape of a YAML mapping key, so a stray "{{" does not
 * swallow the YAML up to some later "}}". An action is also rejected if the file ends before it closes. A rejected
 * "{{" is left as text, and scanning carries on after it.
 */
final class HelmTemplateScanner {

    /**
     * An action, from the start of its "{{" to the end of its "}}".
     */
    @Value
    static class Span {
        int start;
        int end;
    }

    private final String source;

    private HelmTemplateScanner(String source) {
        this.source = source;
    }

    static List<Span> scan(String source) {
        return new HelmTemplateScanner(source).scanAll();
    }

    private List<Span> scanAll() {
        List<Span> spans = new ArrayList<>();
        int open = source.indexOf("{{");
        while (open >= 0) {
            int end = actionEnd(open);
            if (end >= 0) {
                spans.add(new Span(open, end));
                open = source.indexOf("{{", end);
            } else {
                open = source.indexOf("{{", open + 2);
            }
        }
        return spans;
    }

    /**
     * @param open the index of the action's "{{"
     * @return the index just past the action's closing "}}", or -1 if the action is rejected
     */
    private int actionEnd(int open) {
        int i = open + 2;
        if (charAt(i) == '-' && isWhitespace(charAt(i + 1))) {
            i += 2;
        }
        if (source.startsWith("/*", i)) {
            return commentEnd(i + 2);
        }

        boolean spansLines = false;
        boolean holdsMappingKey = false;
        while (i < source.length()) {
            if (source.startsWith("}}", i)) {
                return spansLines && holdsMappingKey ? -1 : i + 2;
            }
            char c = source.charAt(i);
            if (c == '"' || c == '\'') {
                i = quotedEnd(i);
            } else if (c == '`') {
                int closing = source.indexOf('`', i + 1);
                i = closing < 0 ? i + 1 : closing + 1;
            } else {
                if (c == '\n' || c == '\r') {
                    spansLines = true;
                } else if (c == ':' && (i + 1 == source.length() || isWhitespace(source.charAt(i + 1)))) {
                    holdsMappingKey = true;
                }
                i++;
            }
        }
        return -1;
    }

    /**
     * @param body the index just past the slash and asterisk opening the comment
     * @return the index just past the comment's closing "}}", or -1 if the comment is rejected
     */
    private int commentEnd(int body) {
        int commentClose = source.indexOf("*/", body);
        if (commentClose < 0) {
            return -1;
        }
        int i = commentClose + 2;
        if (source.startsWith("}}", i)) {
            return i + 2;
        }
        if (isWhitespace(charAt(i)) && charAt(i + 1) == '-' && source.startsWith("}}", i + 2)) {
            return i + 4;
        }
        return -1;
    }

    /**
     * @param openingQuote the index of a double or single quote
     * @return the index just past the closing quote if it is on the same line, otherwise just past the opening
     * quote, which is then an ordinary character
     */
    private int quotedEnd(int openingQuote) {
        char quote = source.charAt(openingQuote);
        int i = openingQuote + 1;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\n' || c == '\r') {
                break;
            } else if (c == quote) {
                return i + 1;
            } else if (c == '\\') {
                i++;
                if (i < source.length() && (source.charAt(i) == '\n' || source.charAt(i) == '\r')) {
                    break;
                }
            }
            i++;
        }
        return openingQuote + 1;
    }

    private int charAt(int i) {
        return i < source.length() ? source.charAt(i) : -1;
    }

    private static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}
