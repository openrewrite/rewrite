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

import java.util.ArrayList;
import java.util.List;

/**
 * bunfig.toml read into bun's expression shape, as bun's TOML parser (src/toml) gives it to {@code bunfig.zig}:
 * tables and dotted keys become nested objects, with strings, numbers, booleans, arrays and inline tables.
 */
final class Toml {

    private final String source;
    private int pos;

    private Toml(String source) {
        this.source = source;
    }

    static Json.Expr parse(String source) {
        Toml p = new Toml(source);
        List<Json.Property> root = new ArrayList<>();
        List<Json.Property> current = root;
        while (true) {
            p.skipWhitespaceAndNewlines();
            if (p.pos >= source.length()) {
                break;
            }
            char c = source.charAt(p.pos);
            if (c == '[') {
                boolean arrayTable = source.startsWith("[[", p.pos);
                p.pos += arrayTable ? 2 : 1;
                List<String> keys = p.keyPath();
                p.expect(']');
                if (arrayTable) {
                    p.expect(']');
                }
                List<Json.Property> table = root;
                for (int i = 0; i < keys.size(); i++) {
                    boolean last = i == keys.size() - 1;
                    if (last && arrayTable) {
                        Json.Expr array = get(table, keys.get(i));
                        if (array == null) {
                            array = new Json.Expr(Json.Kind.ARRAY, new ArrayList<Json.Expr>());
                            table.add(new Json.Property(Json.Expr.string(keys.get(i)), array));
                        }
                        List<Json.Property> element = new ArrayList<>();
                        array.items().add(new Json.Expr(Json.Kind.OBJECT, element));
                        table = element;
                    } else {
                        table = object(table, keys.get(i));
                    }
                }
                current = table;
            } else {
                List<String> keys = p.keyPath();
                p.skipSpaces();
                p.expect('=');
                p.skipSpaces();
                Json.Expr value = p.value();
                List<Json.Property> table = current;
                for (int i = 0; i < keys.size() - 1; i++) {
                    table = object(table, keys.get(i));
                }
                table.add(new Json.Property(Json.Expr.string(keys.get(keys.size() - 1)), value));
            }
            p.skipSpaces();
            p.skipComment();
        }
        return new Json.Expr(Json.Kind.OBJECT, root);
    }

    private static Json.Expr get(List<Json.Property> table, String key) {
        for (Json.Property p : table) {
            if (key.equals(p.key.asString())) {
                return p.value;
            }
        }
        return null;
    }

    private static List<Json.Property> object(List<Json.Property> table, String key) {
        Json.Expr existing = get(table, key);
        if (existing != null && existing.isObject()) {
            return existing.properties();
        }
        List<Json.Property> props = new ArrayList<>();
        table.add(new Json.Property(Json.Expr.string(key), new Json.Expr(Json.Kind.OBJECT, props)));
        return props;
    }

    private Json.SyntaxError error(String message) {
        return new Json.SyntaxError(message + " at offset " + pos);
    }

    private void expect(char c) {
        skipSpaces();
        if (pos >= source.length() || source.charAt(pos) != c) {
            throw error("Expected \"" + c + "\"");
        }
        pos++;
    }

    private void skipSpaces() {
        while (pos < source.length() && (source.charAt(pos) == ' ' || source.charAt(pos) == '\t')) {
            pos++;
        }
    }

    private void skipComment() {
        if (pos < source.length() && source.charAt(pos) == '#') {
            while (pos < source.length() && source.charAt(pos) != '\n') {
                pos++;
            }
        }
    }

    private void skipWhitespaceAndNewlines() {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else if (c == '#') {
                skipComment();
            } else {
                break;
            }
        }
    }

    private List<String> keyPath() {
        List<String> keys = new ArrayList<>();
        while (true) {
            skipSpaces();
            keys.add(key());
            skipSpaces();
            if (pos < source.length() && source.charAt(pos) == '.') {
                pos++;
                continue;
            }
            return keys;
        }
    }

    private String key() {
        if (pos >= source.length()) {
            throw error("Expected key");
        }
        char c = source.charAt(pos);
        if (c == '"') {
            return basicString();
        }
        if (c == '\'') {
            return literalString();
        }
        int start = pos;
        while (pos < source.length()) {
            char k = source.charAt(pos);
            if (k >= 'a' && k <= 'z' || k >= 'A' && k <= 'Z' || k >= '0' && k <= '9' || k == '_' || k == '-' ||
                k == '@') {
                pos++;
            } else {
                break;
            }
        }
        if (start == pos) {
            throw error("Expected key");
        }
        return source.substring(start, pos);
    }

    private Json.Expr value() {
        if (pos >= source.length()) {
            throw error("Expected value");
        }
        char c = source.charAt(pos);
        if (c == '"') {
            return Json.Expr.string(source.startsWith("\"\"\"", pos) ? multilineString("\"\"\"") : basicString());
        }
        if (c == '\'') {
            return Json.Expr.string(source.startsWith("'''", pos) ? multilineString("'''") : literalString());
        }
        if (c == '[') {
            pos++;
            List<Json.Expr> items = new ArrayList<>();
            while (true) {
                skipWhitespaceAndNewlines();
                if (pos < source.length() && source.charAt(pos) == ']') {
                    pos++;
                    break;
                }
                items.add(value());
                skipWhitespaceAndNewlines();
                if (pos < source.length() && source.charAt(pos) == ',') {
                    pos++;
                }
            }
            return new Json.Expr(Json.Kind.ARRAY, items);
        }
        if (c == '{') {
            pos++;
            List<Json.Property> props = new ArrayList<>();
            while (true) {
                skipSpaces();
                if (pos < source.length() && source.charAt(pos) == '}') {
                    pos++;
                    break;
                }
                List<String> keys = keyPath();
                expect('=');
                skipSpaces();
                Json.Expr v = value();
                List<Json.Property> table = props;
                for (int i = 0; i < keys.size() - 1; i++) {
                    table = object(table, keys.get(i));
                }
                table.add(new Json.Property(Json.Expr.string(keys.get(keys.size() - 1)), v));
                skipSpaces();
                if (pos < source.length() && source.charAt(pos) == ',') {
                    pos++;
                }
            }
            return new Json.Expr(Json.Kind.OBJECT, props);
        }
        if (source.startsWith("true", pos)) {
            pos += 4;
            return new Json.Expr(Json.Kind.BOOLEAN, true);
        }
        if (source.startsWith("false", pos)) {
            pos += 5;
            return new Json.Expr(Json.Kind.BOOLEAN, false);
        }
        int start = pos;
        while (pos < source.length()) {
            char k = source.charAt(pos);
            if (k >= '0' && k <= '9' || k == '.' || k == '-' || k == '+' || k == '_' || k == 'e' || k == 'E') {
                pos++;
            } else {
                break;
            }
        }
        try {
            return new Json.Expr(Json.Kind.NUMBER, Double.parseDouble(source.substring(start, pos).replace("_", "")));
        } catch (NumberFormatException e) {
            throw error("Invalid value");
        }
    }

    private String basicString() {
        pos++;
        StringBuilder out = new StringBuilder();
        while (pos < source.length()) {
            char c = source.charAt(pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\' && pos < source.length()) {
                char e = source.charAt(pos++);
                switch (e) {
                    case 'n':
                        out.append('\n');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 'b':
                        out.append('\b');
                        break;
                    case 'f':
                        out.append('\f');
                        break;
                    case 'u':
                        out.append((char) Integer.parseInt(source.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default:
                        out.append(e);
                        break;
                }
            } else {
                out.append(c);
            }
        }
        throw error("Unterminated string");
    }

    private String literalString() {
        int end = source.indexOf('\'', pos + 1);
        if (end < 0) {
            throw error("Unterminated string");
        }
        String s = source.substring(pos + 1, end);
        pos = end + 1;
        return s;
    }

    private String multilineString(String delimiter) {
        int start = pos + 3;
        int end = source.indexOf(delimiter, start);
        if (end < 0) {
            throw error("Unterminated string");
        }
        String s = source.substring(start, end);
        if (s.startsWith("\n")) {
            s = s.substring(1);
        } else if (s.startsWith("\r\n")) {
            s = s.substring(2);
        }
        pos = end + 3;
        return s;
    }
}
