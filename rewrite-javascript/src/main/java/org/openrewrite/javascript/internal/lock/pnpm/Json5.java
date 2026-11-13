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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.EcmaScript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Port of json5 2.2.3 lib/parse.js and lib/util.js: {@code JSON5.parse} without a reviver.
 */
final class Json5 {

    private static final Map<String, int[]> UNICODE = unicode();
    private static final int[] SPACE_SEPARATOR = requireNonNull(UNICODE.get("Space_Separator"));
    private static final int[] ID_START = requireNonNull(UNICODE.get("ID_Start"));
    private static final int[] ID_CONTINUE = requireNonNull(UNICODE.get("ID_Continue"));

    /**
     * json5's SyntaxError, with the line and column it reports.
     */
    @Getter
    static final class SyntaxError extends RuntimeException {
        private final int lineNumber;
        private final int columnNumber;

        SyntaxError(String message, int lineNumber, int columnNumber) {
            super(message);
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }
    }

    private static final class Token {
        final String type;
        final @Nullable Object value;

        Token(String type, @Nullable Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private final String source;
    private String parseState = "start";
    private final List<Object> stack = new ArrayList<>();
    private int pos;
    private int line = 1;
    private int column;
    private @Nullable Token token;
    private @Nullable String key;
    private boolean rootSet;
    private @Nullable Object root;

    private String lexState = "default";
    private StringBuilder buffer = new StringBuilder();
    private boolean doubleQuote;
    private int sign = 1;
    private @Nullable String c;

    private Json5(String source) {
        this.source = source;
    }

    /**
     * {@code JSON5.parse(text)}: objects in {@code Object.keys} order, numbers as doubles.
     */
    static @Nullable Object parse(String text) {
        Json5 parser = new Json5(text);
        do {
            parser.token = parser.lex();
            parser.parseState();
        } while (!"eof".equals(parser.token.type));
        return EcmaScript.jsonValue(parser.root);
    }

    private Token lex() {
        lexState = "default";
        buffer = new StringBuilder();
        doubleQuote = false;
        sign = 1;
        for (;;) {
            c = peek();
            Token lexed = lexState(lexState);
            if (lexed != null) {
                return lexed;
            }
        }
    }

    private @Nullable String peek() {
        if (pos < source.length()) {
            return new String(Character.toChars(source.codePointAt(pos)));
        }
        return null;
    }

    private @Nullable String read() {
        String read = peek();
        if ("\n".equals(read)) {
            line++;
            column = 0;
        } else if (read != null) {
            column += read.length();
        } else {
            column++;
        }
        if (read != null) {
            pos += read.length();
        }
        return read;
    }

    private @Nullable Token lexState(String state) {
        switch (state) {
            case "default":
                if (c == null) {
                    read();
                    return newToken("eof", null);
                }
                switch (c) {
                    case "\t":
                    case "\u000B":
                    case "\f":
                    case " ":
                    case "\u00A0":
                    case "\uFEFF":
                    case "\n":
                    case "\r":
                    case "\u2028":
                    case "\u2029":
                        read();
                        return null;
                    case "/":
                        read();
                        lexState = "comment";
                        return null;
                }
                if (isSpaceSeparator(c)) {
                    read();
                    return null;
                }
                return lexState(parseState);
            case "comment":
                if ("*".equals(c)) {
                    read();
                    lexState = "multiLineComment";
                    return null;
                }
                if ("/".equals(c)) {
                    read();
                    lexState = "singleLineComment";
                    return null;
                }
                throw invalidChar(read());
            case "multiLineComment":
                if ("*".equals(c)) {
                    read();
                    lexState = "multiLineCommentAsterisk";
                    return null;
                }
                if (c == null) {
                    throw invalidChar(read());
                }
                read();
                return null;
            case "multiLineCommentAsterisk":
                if ("*".equals(c)) {
                    read();
                    return null;
                }
                if ("/".equals(c)) {
                    read();
                    lexState = "default";
                    return null;
                }
                if (c == null) {
                    throw invalidChar(read());
                }
                read();
                lexState = "multiLineComment";
                return null;
            case "singleLineComment":
                if (c == null) {
                    read();
                    return newToken("eof", null);
                }
                switch (c) {
                    case "\n":
                    case "\r":
                    case "\u2028":
                    case "\u2029":
                        read();
                        lexState = "default";
                        return null;
                }
                read();
                return null;
            case "value":
                return value();
            case "identifierNameStartEscape":
                if (!"u".equals(c)) {
                    throw invalidChar(read());
                }
                read();
                String start = unicodeEscape();
                if (!"$".equals(start) && !"_".equals(start) && !isIdStartChar(start)) {
                    throw invalidIdentifier();
                }
                buffer.append(start);
                lexState = "identifierName";
                return null;
            case "identifierName":
                if ("$".equals(c) || "_".equals(c) || "\u200C".equals(c) || "\u200D".equals(c)) {
                    buffer.append(read());
                    return null;
                }
                if ("\\".equals(c)) {
                    read();
                    lexState = "identifierNameEscape";
                    return null;
                }
                if (isIdContinueChar(c)) {
                    buffer.append(read());
                    return null;
                }
                return newToken("identifier", buffer.toString());
            case "identifierNameEscape":
                if (!"u".equals(c)) {
                    throw invalidChar(read());
                }
                read();
                String u = unicodeEscape();
                if (!"$".equals(u) && !"_".equals(u) && !"\u200C".equals(u) && !"\u200D".equals(u) &&
                    !isIdContinueChar(u)) {
                    throw invalidIdentifier();
                }
                buffer.append(u);
                lexState = "identifierName";
                return null;
            case "sign":
                return sign();
            case "zero":
                if (".".equals(c)) {
                    buffer.append(read());
                    lexState = "decimalPoint";
                    return null;
                }
                if ("e".equals(c) || "E".equals(c)) {
                    buffer.append(read());
                    lexState = "decimalExponent";
                    return null;
                }
                if ("x".equals(c) || "X".equals(c)) {
                    buffer.append(read());
                    lexState = "hexadecimal";
                    return null;
                }
                return newToken("numeric", sign * 0.0);
            case "decimalInteger":
                if (".".equals(c)) {
                    buffer.append(read());
                    lexState = "decimalPoint";
                    return null;
                }
                if ("e".equals(c) || "E".equals(c)) {
                    buffer.append(read());
                    lexState = "decimalExponent";
                    return null;
                }
                if (isDigit(c)) {
                    buffer.append(read());
                    return null;
                }
                return newToken("numeric", sign * number(buffer.toString()));
            case "decimalPointLeading":
                if (isDigit(c)) {
                    buffer.append(read());
                    lexState = "decimalFraction";
                    return null;
                }
                throw invalidChar(read());
            case "decimalPoint":
                if ("e".equals(c) || "E".equals(c)) {
                    buffer.append(read());
                    lexState = "decimalExponent";
                    return null;
                }
                if (isDigit(c)) {
                    buffer.append(read());
                    lexState = "decimalFraction";
                    return null;
                }
                return newToken("numeric", sign * number(buffer.toString()));
            case "decimalFraction":
                if ("e".equals(c) || "E".equals(c)) {
                    buffer.append(read());
                    lexState = "decimalExponent";
                    return null;
                }
                if (isDigit(c)) {
                    buffer.append(read());
                    return null;
                }
                return newToken("numeric", sign * number(buffer.toString()));
            case "decimalExponent":
                if ("+".equals(c) || "-".equals(c)) {
                    buffer.append(read());
                    lexState = "decimalExponentSign";
                    return null;
                }
                if (isDigit(c)) {
                    buffer.append(read());
                    lexState = "decimalExponentInteger";
                    return null;
                }
                throw invalidChar(read());
            case "decimalExponentSign":
                if (isDigit(c)) {
                    buffer.append(read());
                    lexState = "decimalExponentInteger";
                    return null;
                }
                throw invalidChar(read());
            case "decimalExponentInteger":
                if (isDigit(c)) {
                    buffer.append(read());
                    return null;
                }
                return newToken("numeric", sign * number(buffer.toString()));
            case "hexadecimal":
                if (isHexDigit(c)) {
                    buffer.append(read());
                    lexState = "hexadecimalInteger";
                    return null;
                }
                throw invalidChar(read());
            case "hexadecimalInteger":
                if (isHexDigit(c)) {
                    buffer.append(read());
                    return null;
                }
                return newToken("numeric", sign * number(buffer.toString()));
            case "string":
                return string();
            case "start":
                if ("{".equals(c) || "[".equals(c)) {
                    return newToken("punctuator", read());
                }
                lexState = "value";
                return null;
            case "beforePropertyName":
                return beforePropertyName();
            case "afterPropertyName":
                if (":".equals(c)) {
                    return newToken("punctuator", read());
                }
                throw invalidChar(read());
            case "beforePropertyValue":
                lexState = "value";
                return null;
            case "afterPropertyValue":
                if (",".equals(c) || "}".equals(c)) {
                    return newToken("punctuator", read());
                }
                throw invalidChar(read());
            case "beforeArrayValue":
                if ("]".equals(c)) {
                    return newToken("punctuator", read());
                }
                lexState = "value";
                return null;
            case "afterArrayValue":
                if (",".equals(c) || "]".equals(c)) {
                    return newToken("punctuator", read());
                }
                throw invalidChar(read());
            default:
                throw invalidChar(read());
        }
    }

    private @Nullable Token value() {
        if (c == null) {
            throw invalidChar(read());
        }
        switch (c) {
            case "{":
            case "[":
                return newToken("punctuator", read());
            case "n":
                read();
                literal("ull");
                return newToken("null", null);
            case "t":
                read();
                literal("rue");
                return newToken("boolean", true);
            case "f":
                read();
                literal("alse");
                return newToken("boolean", false);
            case "-":
            case "+":
                if ("-".equals(read())) {
                    sign = -1;
                }
                lexState = "sign";
                return null;
            case ".":
                buffer = new StringBuilder(requireNonNull(read()));
                lexState = "decimalPointLeading";
                return null;
            case "0":
                buffer = new StringBuilder(requireNonNull(read()));
                lexState = "zero";
                return null;
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
                buffer = new StringBuilder(requireNonNull(read()));
                lexState = "decimalInteger";
                return null;
            case "I":
                read();
                literal("nfinity");
                return newToken("numeric", Double.POSITIVE_INFINITY);
            case "N":
                read();
                literal("aN");
                return newToken("numeric", Double.NaN);
            case "\"":
            case "'":
                doubleQuote = "\"".equals(read());
                buffer = new StringBuilder();
                lexState = "string";
                return null;
        }
        throw invalidChar(read());
    }

    private @Nullable Token sign() {
        if (c != null) {
            switch (c) {
                case ".":
                    buffer = new StringBuilder(requireNonNull(read()));
                    lexState = "decimalPointLeading";
                    return null;
                case "0":
                    buffer = new StringBuilder(requireNonNull(read()));
                    lexState = "zero";
                    return null;
                case "1":
                case "2":
                case "3":
                case "4":
                case "5":
                case "6":
                case "7":
                case "8":
                case "9":
                    buffer = new StringBuilder(requireNonNull(read()));
                    lexState = "decimalInteger";
                    return null;
                case "I":
                    read();
                    literal("nfinity");
                    return newToken("numeric", sign * Double.POSITIVE_INFINITY);
                case "N":
                    read();
                    literal("aN");
                    return newToken("numeric", Double.NaN);
            }
        }
        throw invalidChar(read());
    }

    private @Nullable Token string() {
        if (c == null) {
            throw invalidChar(read());
        }
        switch (c) {
            case "\\":
                read();
                buffer.append(escape());
                return null;
            case "\"":
                if (doubleQuote) {
                    read();
                    return newToken("string", buffer.toString());
                }
                buffer.append(read());
                return null;
            case "'":
                if (!doubleQuote) {
                    read();
                    return newToken("string", buffer.toString());
                }
                buffer.append(read());
                return null;
            case "\n":
            case "\r":
                throw invalidChar(read());
        }
        // a line or paragraph separator only warns
        buffer.append(read());
        return null;
    }

    private @Nullable Token beforePropertyName() {
        if (c != null) {
            switch (c) {
                case "$":
                case "_":
                    buffer = new StringBuilder(requireNonNull(read()));
                    lexState = "identifierName";
                    return null;
                case "\\":
                    read();
                    lexState = "identifierNameStartEscape";
                    return null;
                case "}":
                    return newToken("punctuator", read());
                case "\"":
                case "'":
                    doubleQuote = "\"".equals(read());
                    lexState = "string";
                    return null;
            }
        }
        if (isIdStartChar(c)) {
            buffer.append(read());
            lexState = "identifierName";
            return null;
        }
        throw invalidChar(read());
    }

    private Token newToken(String type, @Nullable Object value) {
        return new Token(type, value);
    }

    private void literal(String s) {
        for (int i = 0; i < s.length(); i++) {
            String p = peek();
            if (!String.valueOf(s.charAt(i)).equals(p)) {
                throw invalidChar(read());
            }
            read();
        }
    }

    private String escape() {
        String e = peek();
        if (e == null) {
            throw invalidChar(read());
        }
        switch (e) {
            case "b":
                read();
                return "\b";
            case "f":
                read();
                return "\f";
            case "n":
                read();
                return "\n";
            case "r":
                read();
                return "\r";
            case "t":
                read();
                return "\t";
            case "v":
                read();
                return "\u000B";
            case "0":
                read();
                if (isDigit(peek())) {
                    throw invalidChar(read());
                }
                return "\0";
            case "x":
                read();
                return hexEscape();
            case "u":
                read();
                return unicodeEscape();
            case "\n":
            case "\u2028":
            case "\u2029":
                read();
                return "";
            case "\r":
                read();
                if ("\n".equals(peek())) {
                    read();
                }
                return "";
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
                throw invalidChar(read());
        }
        return requireNonNull(read());
    }

    private String hexEscape() {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            if (!isHexDigit(peek())) {
                throw invalidChar(read());
            }
            hex.append(read());
        }
        return new String(Character.toChars(Integer.parseInt(hex.toString(), 16)));
    }

    private String unicodeEscape() {
        StringBuilder hex = new StringBuilder();
        for (int count = 4; count-- > 0; ) {
            if (!isHexDigit(peek())) {
                throw invalidChar(read());
            }
            hex.append(read());
        }
        return new String(Character.toChars(Integer.parseInt(hex.toString(), 16)));
    }

    private void parseState() {
        Token t = requireNonNull(token);
        switch (parseState) {
            case "start":
                if ("eof".equals(t.type)) {
                    throw invalidEOF();
                }
                push();
                return;
            case "beforePropertyName":
                switch (t.type) {
                    case "identifier":
                    case "string":
                        key = (String) t.value;
                        parseState = "afterPropertyName";
                        return;
                    case "punctuator":
                        pop();
                        return;
                    case "eof":
                        throw invalidEOF();
                }
                return;
            case "afterPropertyName":
                if ("eof".equals(t.type)) {
                    throw invalidEOF();
                }
                parseState = "beforePropertyValue";
                return;
            case "beforePropertyValue":
                if ("eof".equals(t.type)) {
                    throw invalidEOF();
                }
                push();
                return;
            case "beforeArrayValue":
                if ("eof".equals(t.type)) {
                    throw invalidEOF();
                }
                if ("punctuator".equals(t.type) && "]".equals(t.value)) {
                    pop();
                    return;
                }
                push();
                return;
            case "afterPropertyValue":
                if ("eof".equals(t.type)) {
                    throw invalidEOF();
                }
                if (",".equals(t.value)) {
                    parseState = "beforePropertyName";
                } else if ("}".equals(t.value)) {
                    pop();
                }
                return;
            case "afterArrayValue":
                if ("eof".equals(t.type)) {
                    throw invalidEOF();
                }
                if (",".equals(t.value)) {
                    parseState = "beforeArrayValue";
                } else if ("]".equals(t.value)) {
                    pop();
                }
        }
    }

    @SuppressWarnings("unchecked")
    private void push() {
        Token t = requireNonNull(token);
        Object value = null;
        if ("punctuator".equals(t.type)) {
            if ("{".equals(t.value)) {
                value = new LinkedHashMap<String, @Nullable Object>();
            } else if ("[".equals(t.value)) {
                value = new ArrayList<@Nullable Object>();
            }
        } else {
            value = t.value;
        }
        if (!rootSet) {
            root = value;
            rootSet = true;
        } else {
            Object parent = stack.get(stack.size() - 1);
            if (parent instanceof List) {
                ((List<@Nullable Object>) parent).add(value);
            } else {
                ((Map<String, @Nullable Object>) parent).put(requireNonNull(key), value);
            }
        }
        if (value instanceof Map || value instanceof List) {
            stack.add(value);
            parseState = value instanceof List ? "beforeArrayValue" : "beforePropertyName";
        } else {
            afterValue();
        }
    }

    private void pop() {
        stack.remove(stack.size() - 1);
        afterValue();
    }

    private void afterValue() {
        Object current = stack.isEmpty() ? null : stack.get(stack.size() - 1);
        parseState = current == null ? "end" : current instanceof List ? "afterArrayValue" : "afterPropertyValue";
    }

    private SyntaxError invalidChar(@Nullable String ch) {
        if (ch == null) {
            return syntaxError("JSON5: invalid end of input at " + line + ":" + column);
        }
        return syntaxError("JSON5: invalid character '" + formatChar(ch) + "' at " + line + ":" + column);
    }

    private SyntaxError invalidEOF() {
        return syntaxError("JSON5: invalid end of input at " + line + ":" + column);
    }

    private SyntaxError invalidIdentifier() {
        column -= 5;
        return syntaxError("JSON5: invalid identifier character at " + line + ":" + column);
    }

    private static String formatChar(String ch) {
        switch (ch) {
            case "'":
                return "\\'";
            case "\"":
                return "\\\"";
            case "\\":
                return "\\\\";
            case "\b":
                return "\\b";
            case "\f":
                return "\\f";
            case "\n":
                return "\\n";
            case "\r":
                return "\\r";
            case "\t":
                return "\\t";
            case "\u000B":
                return "\\v";
            case "\0":
                return "\\0";
            case "\u2028":
                return "\\u2028";
            case "\u2029":
                return "\\u2029";
        }
        if (ch.charAt(0) < ' ') {
            String hexString = Integer.toHexString(ch.charAt(0));
            return "\\x" + ("00" + hexString).substring(hexString.length());
        }
        return ch;
    }

    private SyntaxError syntaxError(String message) {
        return new SyntaxError(message, line, column);
    }

    private static double number(String text) {
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return new BigInteger(text.substring(2), 16).doubleValue();
        }
        return Double.parseDouble(text);
    }

    private static boolean isSpaceSeparator(@Nullable String ch) {
        return ch != null && in(SPACE_SEPARATOR, ch.codePointAt(0));
    }

    private static boolean isIdStartChar(@Nullable String ch) {
        return ch != null && in(ID_START, ch.codePointAt(0));
    }

    private static boolean isIdContinueChar(@Nullable String ch) {
        return ch != null && in(ID_CONTINUE, ch.codePointAt(0));
    }

    private static boolean isDigit(@Nullable String ch) {
        return ch != null && ch.charAt(0) >= '0' && ch.charAt(0) <= '9';
    }

    private static boolean isHexDigit(@Nullable String ch) {
        return ch != null && (isDigit(ch) || ch.charAt(0) >= 'A' && ch.charAt(0) <= 'F' ||
                              ch.charAt(0) >= 'a' && ch.charAt(0) <= 'f');
    }

    // ranges as start, end pairs in ascending order
    private static boolean in(int[] ranges, int codePoint) {
        int i = Arrays.binarySearch(ranges, codePoint);
        return i >= 0 || (-i - 1) % 2 == 1;
    }

    // json5-unicode.txt, written by json5.js from util.js' predicates
    private static Map<String, int[]> unicode() {
        Map<String, int[]> sets = new LinkedHashMap<>();
        try (InputStream in = Json5.class.getResourceAsStream("json5-unicode.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(requireNonNull(in),
                     StandardCharsets.UTF_8))) {
            String setLine;
            while ((setLine = reader.readLine()) != null) {
                String[] fields = setLine.split(" ");
                int[] ranges = new int[(fields.length - 1) * 2];
                for (int i = 1; i < fields.length; i++) {
                    int dash = fields[i].indexOf('-');
                    ranges[(i - 1) * 2] = Integer.parseInt(dash < 0 ? fields[i] : fields[i].substring(0, dash), 16);
                    ranges[(i - 1) * 2 + 1] = Integer.parseInt(dash < 0 ? fields[i] : fields[i].substring(dash + 1),
                            16);
                }
                sets.put(fields[0], ranges);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sets;
    }
}
