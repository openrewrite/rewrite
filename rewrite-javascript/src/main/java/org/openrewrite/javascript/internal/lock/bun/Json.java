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
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JSON as bun's src/interchange/json.zig {@code parsePackageJSONUTF8} reads it (bun 1.3.10): comments and trailing
 * commas allowed, every property kept in source order, a repeated key too, {@link Expr#get} answering the first.
 */
final class Json {

    private final String source;
    private int pos;

    private Json(String source) {
        this.source = source;
    }

    static final class SyntaxError extends IllegalArgumentException {
        SyntaxError(String message) {
            super(message);
        }
    }

    static Expr parse(String source) {
        if (source.isEmpty()) {
            return new Expr(Kind.OBJECT, new ArrayList<Property>());
        }
        Json p = new Json(source);
        if (source.charAt(0) == '\uFEFF') {
            p.pos = 1;
        }
        p.skipWhitespace();
        Expr value = p.value();
        p.skipWhitespace();
        if (p.pos < source.length()) {
            throw p.error("Expected end of file");
        }
        return value;
    }

    enum Kind {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
    }

    static final class Property {
        final Expr key;
        final Expr value;

        Property(Expr key, Expr value) {
            this.key = key;
            this.value = value;
        }
    }

    static final class Expr {
        final Kind kind;
        private final @Nullable Object data;

        Expr(Kind kind, @Nullable Object data) {
            this.kind = kind;
            this.data = data;
        }

        static Expr string(String s) {
            return new Expr(Kind.STRING, s);
        }

        boolean isObject() {
            return kind == Kind.OBJECT;
        }

        boolean isArray() {
            return kind == Kind.ARRAY;
        }

        boolean isString() {
            return kind == Kind.STRING;
        }

        @SuppressWarnings("unchecked")
        List<Property> properties() {
            return kind == Kind.OBJECT ? (List<Property>) data : Collections.<Property>emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Expr> items() {
            return kind == Kind.ARRAY ? (List<Expr>) data : Collections.<Expr>emptyList();
        }

        @Nullable
        Expr get(String name) {
            for (Property p : properties()) {
                if (p.key.isString() && name.equals(p.key.data)) {
                    return p.value;
                }
            }
            return null;
        }

        @Nullable
        Expr getObject(String name) {
            Expr e = get(name);
            return e != null && e.isObject() ? e : null;
        }

        @Nullable
        String asString() {
            return kind == Kind.STRING ? (String) data : null;
        }

        @Nullable
        Boolean asBool() {
            return kind == Kind.BOOLEAN ? (Boolean) data : null;
        }

        double asNumber() {
            return kind == Kind.NUMBER ? (Double) data : Double.NaN;
        }
    }

    private SyntaxError error(String message) {
        return new SyntaxError(message);
    }

    private void skipWhitespace() {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000B' || c == '\u000C' ||
                c == '\u00A0' || c == '\uFEFF' || c == '\u2028' || c == '\u2029') {
                pos++;
            } else if (c == '/' && pos + 1 < source.length() && source.charAt(pos + 1) == '/') {
                while (pos < source.length() && source.charAt(pos) != '\n' && source.charAt(pos) != '\r') {
                    pos++;
                }
            } else if (c == '/' && pos + 1 < source.length() && source.charAt(pos + 1) == '*') {
                int end = source.indexOf("*/", pos + 2);
                if (end < 0) {
                    throw error("Expected \"*/\" to terminate multi-line comment");
                }
                pos = end + 2;
            } else {
                break;
            }
        }
    }

    private Expr value() {
        if (pos >= source.length()) {
            throw error("Unexpected end of file");
        }
        char c = source.charAt(pos);
        switch (c) {
            case '{':
                return object();
            case '[':
                return array();
            case '"':
                return Expr.string(string());
            case 't':
                keyword("true");
                return new Expr(Kind.BOOLEAN, true);
            case 'f':
                keyword("false");
                return new Expr(Kind.BOOLEAN, false);
            case 'n':
                keyword("null");
                return new Expr(Kind.NULL, null);
            default:
                if (c == '-' || c >= '0' && c <= '9') {
                    return number();
                }
                throw error("Unexpected " + c);
        }
    }

    private void keyword(String word) {
        if (!source.startsWith(word, pos)) {
            throw error("Unexpected " + source.charAt(pos));
        }
        pos += word.length();
    }

    private Expr object() {
        pos++;
        List<Property> properties = new ArrayList<>();
        skipWhitespace();
        while (!at('}')) {
            if (!properties.isEmpty()) {
                trailingComma();
                if (at('}')) {
                    break;
                }
            }
            if (!at('"')) {
                throw error("Expected string but found " + found());
            }
            Expr key = Expr.string(string());
            skipWhitespace();
            expect(':');
            skipWhitespace();
            properties.add(new Property(key, value()));
            skipWhitespace();
        }
        expect('}');
        return new Expr(Kind.OBJECT, properties);
    }

    private Expr array() {
        pos++;
        List<Expr> items = new ArrayList<>();
        skipWhitespace();
        while (!at(']')) {
            if (!items.isEmpty()) {
                trailingComma();
                if (at(']')) {
                    break;
                }
            }
            items.add(value());
            skipWhitespace();
        }
        expect(']');
        return new Expr(Kind.ARRAY, items);
    }

    // parseMaybeTrailingComma: a comma is required between members and allowed before the close
    private void trailingComma() {
        if (!at(',')) {
            throw error("Expected \",\" but found " + found());
        }
        pos++;
        skipWhitespace();
    }

    private boolean at(char c) {
        return pos < source.length() && source.charAt(pos) == c;
    }

    private String found() {
        return pos >= source.length() ? "end of file" : "\"" + source.charAt(pos) + "\"";
    }

    private void expect(char c) {
        if (!at(c)) {
            throw error("Expected \"" + c + "\" but found " + found());
        }
        pos++;
    }

    private String string() {
        pos++;
        StringBuilder out = new StringBuilder();
        while (true) {
            if (pos >= source.length()) {
                throw error("Unterminated string literal");
            }
            char c = source.charAt(pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c == '\n' || c == '\r') {
                throw error("Unterminated string literal");
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (pos >= source.length()) {
                throw error("Unterminated string literal");
            }
            char e = source.charAt(pos++);
            switch (e) {
                case 'b':
                    out.append('\b');
                    break;
                case 'f':
                    out.append('\f');
                    break;
                case 'n':
                    out.append('\n');
                    break;
                case 'r':
                    out.append('\r');
                    break;
                case 't':
                    out.append('\t');
                    break;
                case 'u':
                    if (pos + 4 > source.length()) {
                        throw error("Invalid unicode escape");
                    }
                    try {
                        out.append((char) Integer.parseInt(source.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException ex) {
                        throw error("Invalid unicode escape");
                    }
                    pos += 4;
                    break;
                case '"':
                case '\\':
                case '/':
                    out.append(e);
                    break;
                default:
                    throw error("Invalid escape sequence");
            }
        }
    }

    private Expr number() {
        int start = pos;
        if (source.charAt(pos) == '-') {
            pos++;
        }
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c >= '0' && c <= '9' || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                pos++;
            } else {
                break;
            }
        }
        try {
            return new Expr(Kind.NUMBER, Double.parseDouble(source.substring(start, pos)));
        } catch (NumberFormatException e) {
            throw error("Invalid number");
        }
    }
}
