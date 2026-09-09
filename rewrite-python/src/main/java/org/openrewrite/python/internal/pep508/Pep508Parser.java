/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.python.internal.pep508;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of pypa/packaging's {@code _tokenizer.Tokenizer} and {@code _parser} recursive
 * descent parser for PEP 508 dependency specifiers and environment markers.
 */
class Pep508Parser {
    static class SyntaxException extends RuntimeException {
        SyntaxException(String message) {
            super(message);
        }
    }

    static final class ParsedRequirement {
        final String name;
        final String url;
        final List<String> extras;
        final String specifier;
        final @Nullable List<Object> marker;

        ParsedRequirement(String name, String url, List<String> extras, String specifier,
                          @Nullable List<Object> marker) {
            this.name = name;
            this.url = url;
            this.extras = extras;
            this.specifier = specifier;
            this.marker = marker;
        }
    }

    /**
     * A marker comparison operand: either an environment variable or a quoted literal.
     */
    static final class Operand {
        final String value;
        final boolean variable;

        Operand(String value, boolean variable) {
            this.value = value;
            this.variable = variable;
        }
    }

    /**
     * A single {@code lhs op rhs} marker comparison.
     */
    static final class Comparison {
        final Operand lhs;
        final String op;
        final Operand rhs;

        Comparison(Operand lhs, String op, Operand rhs) {
            this.lhs = lhs;
            this.op = op;
            this.rhs = rhs;
        }
    }

    private static final class Token {
        final String name;
        final String text;

        Token(String name, String text) {
            this.name = name;
            this.text = text;
        }
    }

    private static final Map<String, Pattern> RULES = new HashMap<>();

    static {
        RULES.put("LEFT_PARENTHESIS", Pattern.compile("\\("));
        RULES.put("RIGHT_PARENTHESIS", Pattern.compile("\\)"));
        RULES.put("LEFT_BRACKET", Pattern.compile("\\["));
        RULES.put("RIGHT_BRACKET", Pattern.compile("]"));
        RULES.put("SEMICOLON", Pattern.compile(";"));
        RULES.put("COMMA", Pattern.compile(","));
        RULES.put("QUOTED_STRING", Pattern.compile("(?:('[^']*')|(\"[^\"]*\"))"));
        RULES.put("OP", Pattern.compile("(?:===|==|~=|!=|<=|>=|<|>)"));
        RULES.put("BOOLOP", Pattern.compile("\\b(?:or|and)\\b"));
        RULES.put("IN", Pattern.compile("\\bin\\b"));
        RULES.put("NOT", Pattern.compile("\\bnot\\b"));
        RULES.put("VARIABLE", Pattern.compile("\\b(?:" +
                "python_version" +
                "|python_full_version" +
                "|os[._]name" +
                "|sys[._]platform" +
                "|platform_(?:release|system)" +
                "|platform[._](?:version|machine|python_implementation)" +
                "|python_implementation" +
                "|implementation_(?:name|version)" +
                "|extras?" +
                "|dependency_groups" +
                ")\\b"));
        RULES.put("SPECIFIER", Pattern.compile(PythonVersionSpecifier.SPECIFIER_REGEX, Pattern.CASE_INSENSITIVE));
        RULES.put("AT", Pattern.compile("@"));
        RULES.put("URL", Pattern.compile("[^ \\t]+"));
        RULES.put("IDENTIFIER", Pattern.compile("\\b[a-zA-Z0-9][a-zA-Z0-9._-]*\\b"));
        RULES.put("VERSION_PREFIX_TRAIL", Pattern.compile("\\.\\*"));
        RULES.put("VERSION_LOCAL_LABEL_TRAIL", Pattern.compile("\\+[a-z0-9]+(?:[-_.][a-z0-9]+)*"));
        RULES.put("WS", Pattern.compile("[ \\t]+"));
        RULES.put("END", Pattern.compile("$"));
    }

    private final String source;
    private int position;
    private @Nullable Token nextToken;

    private Pep508Parser(String source) {
        this.source = source;
    }

    // ---------------------------------------------------------------------
    // Tokenizer
    // ---------------------------------------------------------------------

    private boolean check(String name) {
        return check(name, false);
    }

    private boolean check(String name, boolean peek) {
        Pattern pattern = RULES.get(name);
        Matcher m = pattern.matcher(source);
        m.region(position, source.length());
        m.useTransparentBounds(true);
        m.useAnchoringBounds(false);
        if (!m.lookingAt()) {
            return false;
        }
        if (!peek) {
            nextToken = new Token(name, m.group());
        }
        return true;
    }

    private void consume(String name) {
        if (check(name)) {
            read();
        }
    }

    private Token expect(String name, String expected) {
        if (!check(name)) {
            throw new SyntaxException("Expected " + expected);
        }
        return read();
    }

    private Token read() {
        Token token = nextToken;
        if (token == null) {
            throw new IllegalStateException("read() without successful check()");
        }
        position += token.text.length();
        nextToken = null;
        return token;
    }

    // ---------------------------------------------------------------------
    // Requirement parsing
    // ---------------------------------------------------------------------

    static ParsedRequirement parseRequirement(String source) {
        Pep508Parser t = new Pep508Parser(source);
        t.consume("WS");
        Token name = t.expect("IDENTIFIER", "package name at the start of dependency specifier");
        t.consume("WS");
        List<String> extras = t.parseExtras();
        t.consume("WS");
        ParsedRequirement details = t.parseRequirementDetails(name.text, extras);
        t.expect("END", "end of dependency specifier");
        return details;
    }

    private ParsedRequirement parseRequirementDetails(String name, List<String> extras) {
        String specifier = "";
        String url = "";
        List<Object> marker = null;

        if (check("AT")) {
            read();
            consume("WS");
            url = expect("URL", "URL after @").text;
            if (check("END", true)) {
                return new ParsedRequirement(name, url, extras, specifier, null);
            }
            expect("WS", "whitespace after URL");
            if (check("END", true)) {
                return new ParsedRequirement(name, url, extras, specifier, null);
            }
            marker = parseRequirementMarker("semicolon (after URL and whitespace)");
        } else {
            specifier = parseSpecifier();
            consume("WS");
            if (check("END", true)) {
                return new ParsedRequirement(name, url, extras, specifier, null);
            }
            marker = parseRequirementMarker(specifier.isEmpty() ?
                    "semicolon (after name with no version specifier)" :
                    "comma (within version specifier), semicolon (after version specifier)");
        }

        return new ParsedRequirement(name, url, extras, specifier, marker);
    }

    private List<Object> parseRequirementMarker(String expected) {
        if (!check("SEMICOLON")) {
            throw new SyntaxException("Expected " + expected + " or end");
        }
        read();
        List<Object> marker = parseMarkerList();
        consume("WS");
        return marker;
    }

    private List<String> parseExtras() {
        List<String> extras = new ArrayList<>();
        if (!check("LEFT_BRACKET", true)) {
            return extras;
        }
        check("LEFT_BRACKET");
        read();
        consume("WS");
        if (check("IDENTIFIER")) {
            extras.add(read().text);
            while (true) {
                consume("WS");
                if (check("IDENTIFIER", true)) {
                    throw new SyntaxException("Expected comma between extra names");
                }
                if (!check("COMMA")) {
                    break;
                }
                read();
                consume("WS");
                extras.add(expect("IDENTIFIER", "extra name after comma").text);
            }
        }
        consume("WS");
        if (!check("RIGHT_BRACKET")) {
            throw new SyntaxException("Expected matching RIGHT_BRACKET for LEFT_BRACKET, after extras");
        }
        read();
        return extras;
    }

    private String parseSpecifier() {
        boolean opened = false;
        if (check("LEFT_PARENTHESIS")) {
            read();
            opened = true;
        }
        consume("WS");
        String specifiers = parseVersionMany();
        consume("WS");
        if (opened) {
            if (!check("RIGHT_PARENTHESIS")) {
                throw new SyntaxException(
                        "Expected matching RIGHT_PARENTHESIS for LEFT_PARENTHESIS, after version specifier");
            }
            read();
        }
        return specifiers;
    }

    private String parseVersionMany() {
        StringBuilder parsed = new StringBuilder();
        while (check("SPECIFIER")) {
            String specifier = read().text;
            parsed.append(specifier);
            if (check("VERSION_PREFIX_TRAIL", true)) {
                if (specifier.startsWith("!=") ||
                        (specifier.startsWith("==") && !specifier.startsWith("==="))) {
                    throw new SyntaxException(
                            ".* suffix cannot be used with pre-release, post-release, dev or local versions");
                }
                throw new SyntaxException(".* suffix can only be used with `==` or `!=` operators");
            }
            if (check("VERSION_LOCAL_LABEL_TRAIL", true)) {
                throw new SyntaxException("Local version label can only be used with `==` or `!=` operators");
            }
            consume("WS");
            if (!check("COMMA")) {
                break;
            }
            parsed.append(read().text);
            consume("WS");
        }
        return parsed.toString();
    }

    // ---------------------------------------------------------------------
    // Marker parsing
    // ---------------------------------------------------------------------

    /**
     * Parses a complete marker expression into packaging's nested list structure:
     * elements are {@link Comparison}, the strings {@code "and"}/{@code "or"}, or nested lists.
     */
    static List<Object> parseMarker(String source) {
        Pep508Parser t = new Pep508Parser(source);
        List<Object> result = t.parseMarkerList();
        t.expect("END", "end of marker expression");
        return result;
    }

    private List<Object> parseMarkerList() {
        List<Object> expression = new ArrayList<>();
        expression.add(parseMarkerAtom());
        while (check("BOOLOP")) {
            Token token = read();
            Object right = parseMarkerAtom();
            expression.add(token.text);
            expression.add(right);
        }
        return expression;
    }

    private Object parseMarkerAtom() {
        consume("WS");
        Object marker;
        if (check("LEFT_PARENTHESIS", true)) {
            check("LEFT_PARENTHESIS");
            read();
            consume("WS");
            marker = parseMarkerList();
            consume("WS");
            if (!check("RIGHT_PARENTHESIS")) {
                throw new SyntaxException(
                        "Expected matching RIGHT_PARENTHESIS for LEFT_PARENTHESIS, after marker expression");
            }
            read();
        } else {
            marker = parseMarkerItem();
        }
        consume("WS");
        return marker;
    }

    private Comparison parseMarkerItem() {
        consume("WS");
        Operand lhs = parseMarkerVar();
        consume("WS");
        String op = parseMarkerOp();
        consume("WS");
        Operand rhs = parseMarkerVar();
        consume("WS");
        return new Comparison(lhs, op, rhs);
    }

    private Operand parseMarkerVar() {
        if (check("VARIABLE")) {
            String name = read().text.replace('.', '_');
            // Setuptools legacy alias.
            if ("python_implementation".equals(name)) {
                name = "platform_python_implementation";
            }
            return new Operand(name, true);
        }
        if (check("QUOTED_STRING")) {
            String text = read().text;
            return new Operand(unescapePythonString(text.substring(1, text.length() - 1)), false);
        }
        throw new SyntaxException("Expected a marker variable or quoted string");
    }

    private String parseMarkerOp() {
        if (check("IN")) {
            read();
            return "in";
        }
        if (check("NOT")) {
            read();
            expect("WS", "whitespace after 'not'");
            expect("IN", "'in' after 'not'");
            return "not in";
        }
        if (check("OP")) {
            return read().text;
        }
        throw new SyntaxException(
                "Expected marker operator, one of <=, <, !=, ==, >=, >, ~=, ===, in, not in");
    }

    /**
     * Python string-literal escape processing, mirroring {@code ast.literal_eval} on the
     * quoted marker value; unknown escapes keep their backslash, malformed ones fail.
     */
    static String unescapePythonString(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (i >= s.length()) {
                throw new SyntaxException("Invalid quoted string");
            }
            char e = s.charAt(i++);
            switch (e) {
                case '\n':
                    break;
                case '\\':
                case '\'':
                case '"':
                    sb.append(e);
                    break;
                case 'a':
                    sb.append('\u0007');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'v':
                    sb.append('\u000B');
                    break;
                case 'x': {
                    if (i + 2 > s.length()) {
                        throw new SyntaxException("Invalid \\x escape");
                    }
                    sb.append((char) parseHex(s.substring(i, i + 2)));
                    i += 2;
                    break;
                }
                case 'u': {
                    if (i + 4 > s.length()) {
                        throw new SyntaxException("Invalid \\u escape");
                    }
                    sb.append((char) parseHex(s.substring(i, i + 4)));
                    i += 4;
                    break;
                }
                case 'U': {
                    if (i + 8 > s.length()) {
                        throw new SyntaxException("Invalid \\U escape");
                    }
                    sb.appendCodePoint(parseHex(s.substring(i, i + 8)));
                    i += 8;
                    break;
                }
                case 'N':
                    throw new SyntaxException("Unsupported \\N escape");
                default:
                    if (e >= '0' && e <= '7') {
                        int value = e - '0';
                        for (int j = 0; j < 2 && i < s.length(); j++) {
                            char o = s.charAt(i);
                            if (o < '0' || o > '7') {
                                break;
                            }
                            value = value * 8 + (o - '0');
                            i++;
                        }
                        sb.append((char) value);
                    } else {
                        // Python keeps unrecognized escapes verbatim.
                        sb.append('\\').append(e);
                    }
            }
        }
        return sb.toString();
    }

    private static int parseHex(String hex) {
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new SyntaxException("Invalid hex escape: " + hex);
        }
    }
}
