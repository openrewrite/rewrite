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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Port of bun's src/parsers/json_stage2.rs {@code Parser} (bun 1.4.0, {@link BunRelease#indexesJsonBeforeParsingIt}):
 * stage 2 of the JSON parser, recursive descent over the {@link JsonIndex} with package.json's options (comments and
 * trailing commas allowed), into {@link Json.Expr}. A token's text in a message is its run, from its index to the next,
 * without trailing ASCII whitespace. Duplicate keys only warn, and a document's nesting depth, which bun bounds by its
 * native stack, is not bounded here.
 */
final class JsonStage2 {

    private static final int EOF = -1;

    private final byte[] contents;
    private final int len;
    private final Json.Log log;
    private final JsonIndex idx;
    private final boolean allowTrailingCommas;
    private int cursor;
    private int tokenStart;
    private int prevErrorLoc = -1;
    private int peekPos;

    JsonStage2(byte[] contents, Json.Log log, JsonIndex idx, boolean allowTrailingCommas) {
        this.contents = contents;
        this.len = contents.length;
        this.log = log;
        this.idx = idx;
        this.allowTrailingCommas = allowTrailingCommas;
    }

    /**
     * {@code at_trailing_end}: nothing but exotic whitespace follows the cursor.
     */
    boolean atTrailingEnd() {
        while (true) {
            int p = posAt(cursor);
            if (p >= len) {
                return true;
            }
            int b = byteAt(p);
            if ((b >= 0x80 || b == 0x0B || b == 0x0C) && restIsWsCold(p, posAt(cursor + 1))) {
                cursor++;
                continue;
            }
            return false;
        }
    }

    Json.Failure unexpectedHere() {
        return unexpected(cursor);
    }

    // LexerLog (src/ast/lexer_log.rs): an error at the location of the previous one is not logged again

    private void addError(int loc, String text) {
        if (loc == prevErrorLoc) {
            return;
        }
        log.add(text, loc, 0);
        prevErrorLoc = loc;
    }

    private void addRangeError(int loc, int length, String text) {
        if (loc == prevErrorLoc) {
            return;
        }
        log.add(text, loc, length);
        prevErrorLoc = loc;
    }

    private Json.Failure syntaxError() {
        if (!log.hasErrors()) {
            addError(tokenStart, "Syntax Error");
        }
        return new Json.Failure(Json.SYNTAX_ERROR);
    }

    private Json.Failure addDefaultError(String msg) {
        addError(tokenStart, msg);
        return new Json.Failure(Json.SYNTAX_ERROR);
    }

    private int byteAt(int p) {
        return contents[p] & 0xFF;
    }

    private int posAt(int c) {
        return idx.at(c);
    }

    private String text(int from, int to) {
        return new String(contents, from, to - from, UTF_8);
    }

    // the token's range: one structural byte, a string with its quotes, or the run without trailing ASCII whitespace
    private int tokenLength(int c) {
        int p = posAt(c);
        if (p >= len) {
            return 0;
        }
        int b = byteAt(p);
        switch (b) {
            case '{':
            case '}':
            case '[':
            case ']':
            case ':':
            case ',':
                return 1;
            case '"':
            case '\'': {
                int close = posAt(c + 1);
                return close < len && contents[close] == contents[p] ? close + 1 - p : 1;
            }
            default: {
                int end = posAt(c + 1);
                int e = end - p;
                while (e > 0 && isAsciiWhitespace(byteAt(p + e - 1))) {
                    e--;
                }
                return Math.max(e, 1);
            }
        }
    }

    private Json.Failure unexpected(int c) {
        int length = tokenLength(c);
        int p = posAt(c);
        if (p >= len) {
            addRangeError(len, 0, "Unexpected end of file");
        } else {
            addRangeError(p, length, "Unexpected " + text(p, p + Math.max(length, 1)));
        }
        return new Json.Failure(Json.PARSER_ERROR);
    }

    private void expected(int c, String what) {
        int length = tokenLength(c);
        int p = posAt(c);
        if (p >= len) {
            addRangeError(len, 0, "Expected " + what + " but found end of file");
        } else {
            addRangeError(p, length, "Expected " + what + " but found \"" + text(p, p + Math.max(length, 1)) + "\"");
        }
    }

    private static String jsPunctMessage(int c) {
        switch (c) {
            case '#':
                return "Private identifiers are not allowed in JSON";
            case ';':
                return "Semicolons are not allowed in JSON";
            case '@':
                return "Decorators are not allowed in JSON";
            case '~':
                return "~ is not allowed in JSON";
            case '%':
            case '&':
            case '|':
            case '^':
            case '+':
            case '=':
            case '<':
            case '>':
            case '!':
            case '`':
                return "Operators are not allowed in JSON";
            default:
                return null;
        }
    }

    private Json.Failure junkByteError(int c, int pos, int b) {
        String msg = jsPunctMessage(b);
        if (msg != null) {
            addError(pos + 1, "Unsupported syntax: " + msg);
            return new Json.Failure(Json.SYNTAX_ERROR);
        }
        return unexpected(c);
    }

    // past whitespace (exotic too) and comments from the cursor's position: that position, moving the cursor to the
    // index it is in; EOF at the end, moving the cursor to the end
    private int skipUnicodeWs() {
        int p = posAt(cursor);
        outer:
        while (true) {
            while (p < len) {
                int cp = codePointAt(p);
                if (!(cp == 0x09 || cp == 0x0A || cp == 0x0D || cp == 0x20 || isExoticWhitespace(cp))) {
                    break;
                }
                p += width(p);
            }
            if (p >= len || contents[p] != '/') {
                break;
            }
            int next = p + 1 < len ? byteAt(p + 1) : EOF;
            if (next == '/') {
                p += 2;
                while (p < len && contents[p] != '\n' && contents[p] != '\r' && !JsonIndex.isLsPs(contents, p)) {
                    p++;
                }
            } else if (next == '*') {
                int close = indexOf(contents, p + 2, '*', '/');
                if (close < 0) {
                    p = len;
                    break outer;
                }
                p = close + 2;
            } else {
                break;
            }
        }
        if (p >= len) {
            while (posAt(cursor) < len) {
                cursor++;
            }
            return EOF;
        }
        while (posAt(cursor) < p && posAt(cursor + 1) <= p) {
            cursor++;
        }
        return p;
    }

    // the byte at the cursor past exotic whitespace (EOF at the end), and in peekPos the cursor's position
    private int peek() {
        int p = posAt(cursor);
        if (p >= len) {
            peekPos = p;
            return EOF;
        }
        int b = byteAt(p);
        if (b >= 0x80 || b == 0x0B || b == 0x0C) {
            int np = skipUnicodeWs();
            int peeked = np == EOF ? EOF : byteAt(np);
            peekPos = posAt(cursor);
            return peeked;
        }
        peekPos = p;
        return b;
    }

    Json.Expr parseValue() {
        int c = cursor;
        int start = posAt(c);
        if (start >= len) {
            tokenStart = len;
            throw unexpected(c);
        }
        tokenStart = start;
        switch (contents[start]) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
            case '\'':
                return Json.Expr.string(parseStringAt(start));
            default:
                return parseScalar();
        }
    }

    private String parseStringAt(int open) {
        int i = cursor;
        int close = posAt(i + 1);
        byte quote = contents[open];
        tokenStart = open;
        if (close >= len || contents[close] != quote) {
            throw addDefaultError("Unterminated string literal");
        }
        cursor = i + 2;
        // the dirty-block check (FLAG_HAS_BACKSLASH_IN_STRING / FLAG_HAS_CTRL_IN_STRING) sends a body with a backslash
        // or a control character to the slow path; one without either is the same either way
        for (int k = open + 1; k < close; k++) {
            int b = byteAt(k);
            if (b == '\\' || b < 0x20) {
                if (b != '\\') {
                    throw stringControlCharError(b);
                }
                return decodeEscapes(open + 1, close);
            }
        }
        return text(open + 1, close);
    }

    private Json.Failure stringControlCharError(int c) {
        if (c == '\r' || c == '\n') {
            return addDefaultError("Unterminated string literal");
        }
        return syntaxError();
    }

    private Json.Expr parseScalar() {
        int c = cursor;
        int start = posAt(c);
        int end = posAt(c + 1);
        int first = byteAt(start);
        if (first == 't' && startsWith(start, end, "true") && runRestIsWs(start + 4, end) ||
            first == 'f' && startsWith(start, end, "false") && runRestIsWs(start + 5, end) ||
            first == 'n' && startsWith(start, end, "null") && runRestIsWs(start + 4, end)) {
            cursor++;
            return keyword(first);
        }
        if (first >= '0' && first <= '9' || first == '.' || first == '-') {
            return parseNumber();
        }
        return parseScalarCold();
    }

    private static Json.Expr keyword(int first) {
        return first == 'n' ? new Json.Expr(Json.Kind.NULL, null) : new Json.Expr(Json.Kind.BOOLEAN, first == 't');
    }

    private boolean runRestIsWs(int from, int to) {
        return from == to || restIsWsCold(from, to);
    }

    private boolean restIsWsCold(int from, int to) {
        int i = from;
        while (i < to) {
            int b = byteAt(i);
            if (b == ' ' || b == '\t' || b == '\n' || b == '\r') {
                i++;
            } else if (b == '/') {
                int next = i + 1 < to ? byteAt(i + 1) : EOF;
                if (next == '/') {
                    i += 2;
                    while (i < to && contents[i] != '\n' && contents[i] != '\r') {
                        i++;
                    }
                } else if (next == '*') {
                    int close = indexOf(contents, i + 2, to, '*', '/');
                    if (close < 0) {
                        return false;
                    }
                    i = close + 2;
                } else {
                    return false;
                }
            } else {
                if (!isExoticWhitespace(codePointAt(i))) {
                    return false;
                }
                i += width(i);
            }
        }
        return true;
    }

    private Json.Expr parseArray() {
        cursor++;
        List<Json.Expr> items = new ArrayList<>();
        while (true) {
            int b = peek();
            int p = peekPos;
            int c = cursor;
            if (p >= len) {
                tokenStart = len;
                expected(c, "\"]\"");
                throw new Json.Failure(Json.PARSER_ERROR);
            }
            if (b == ']') {
                cursor++;
                break;
            }
            if (!items.isEmpty()) {
                if (b != ',') {
                    String msg = jsPunctMessage(b);
                    if (msg != null) {
                        addError(p + 1, "Unsupported syntax: " + msg);
                        throw new Json.Failure(Json.SYNTAX_ERROR);
                    }
                    expected(c, "\",\"");
                    throw new Json.Failure(Json.PARSER_ERROR);
                }
                cursor++;
                if (peek() == ']') {
                    if (!allowTrailingCommas) {
                        addRangeError(p, 1, "JSON does not support trailing commas");
                    }
                    cursor++;
                    break;
                }
            }
            items.add(parseValue());
        }
        return new Json.Expr(Json.Kind.ARRAY, items);
    }

    private Json.Expr parseObject() {
        cursor++;
        List<Json.Property> properties = new ArrayList<>();
        while (true) {
            int b = peek();
            int p = peekPos;
            int c = cursor;
            if (p >= len) {
                tokenStart = len;
                expected(c, "\"}\"");
                throw new Json.Failure(Json.PARSER_ERROR);
            }
            if (b == '}') {
                cursor++;
                break;
            }
            if (!properties.isEmpty()) {
                if (b != ',') {
                    String msg = jsPunctMessage(b);
                    if (msg != null) {
                        addError(p + 1, "Unsupported syntax: " + msg);
                        throw new Json.Failure(Json.SYNTAX_ERROR);
                    }
                    expected(c, "\",\"");
                    throw new Json.Failure(Json.PARSER_ERROR);
                }
                cursor++;
                int comma = p;
                b = peek();
                p = peekPos;
                if (b == '}') {
                    if (!allowTrailingCommas) {
                        addRangeError(comma, 1, "JSON does not support trailing commas");
                    }
                    cursor++;
                    break;
                }
            }
            int keyCursor = cursor;
            tokenStart = p;
            if (b != '"' && b != '\'') {
                expected(keyCursor, "string");
                throw unexpected(keyCursor);
            }
            String key = parseStringAt(p);
            if (peek() == ':') {
                cursor++;
            } else {
                expected(cursor, "\":\"");
                throw new Json.Failure(Json.PARSER_ERROR);
            }
            properties.add(new Json.Property(Json.Expr.string(key), parseValue()));
        }
        return new Json.Expr(Json.Kind.OBJECT, properties);
    }

    private Json.Expr parseNumber() {
        int c = cursor;
        int start = posAt(c);
        int end = posAt(c + 1);
        if (contents[start] == '-') {
            return parseNegativeNumberAt(start);
        }
        double[] value = new double[1];
        int used = parseNumberText(start, end, value);
        if (!restIsWsCold(start + used, end)) {
            throw numberTrailingJunk(start + used);
        }
        cursor++;
        return number(value[0]);
    }

    private Json.Expr parseNegativeNumberAt(int minusPos) {
        tokenStart = minusPos;
        int q = skipWsAndComments(minusPos + 1);
        if (q < 0) {
            while (posAt(cursor) < len) {
                cursor++;
            }
            expected(cursor, "number");
            throw unexpected(cursor);
        }
        while (posAt(cursor) < q && posAt(cursor + 1) <= q) {
            cursor++;
        }
        if (!(contents[q] >= '0' && contents[q] <= '9' || contents[q] == '.')) {
            expected(cursor, "number");
            throw unexpected(cursor);
        }
        tokenStart = q;
        int end = posAt(cursor + 1);
        double[] value = new double[1];
        int used = parseNumberText(q, end, value);
        if (!restIsWsCold(q + used, end)) {
            throw numberTrailingJunk(q + used);
        }
        cursor++;
        return number(-value[0]);
    }

    private static Json.Expr number(double value) {
        return new Json.Expr(Json.Kind.NUMBER, value);
    }

    private Json.Failure numberTrailingJunk(int pos) {
        int b = byteAt(pos);
        if (isIdentifierStart(b) || b == '\\') {
            return syntaxErrAt(pos);
        }
        return junkByteError(cursor, pos, b);
    }

    private Json.Failure syntaxErrAt(int pos) {
        tokenStart = pos;
        return syntaxError();
    }

    // the number at [pos, end): its value in value[0] and the bytes it takes
    private int parseNumberText(int pos, int end, double[] value) {
        tokenStart = pos;
        int n = end - pos;
        int first = byteAt(pos);
        int i = 1;
        if (first == '.' && (n < 2 || !isDigit(byteAt(pos + 1)))) {
            throw syntaxErrAt(pos);
        }
        if (first == '0' && n > 1) {
            int t1 = byteAt(pos + 1);
            int radix = 0;
            int prefixLen = 0;
            boolean legacyOctal = false;
            if (t1 == 'b' || t1 == 'B') {
                radix = 2;
                prefixLen = 2;
            } else if (t1 == 'o' || t1 == 'O') {
                radix = 8;
                prefixLen = 2;
            } else if (t1 == 'x' || t1 == 'X') {
                radix = 16;
                prefixLen = 2;
            } else if (t1 >= '0' && t1 <= '7' || t1 == '_') {
                radix = 8;
                prefixLen = 1;
                legacyOctal = true;
            } else if (t1 == '8' || t1 == '9') {
                radix = 10;
                prefixLen = 1;
                legacyOctal = true;
            }
            if (radix != 0) {
                return parseRadixNumber(pos, end, radix, prefixLen, legacyOctal, value);
            }
        }
        boolean hasDotOrExp = first == '.';
        boolean underscores = false;
        int lastUnderscoreEnd = -1;
        if (first != '.') {
            i = digits(pos, n, i, lastUnderscoreEnd);
            lastUnderscoreEnd = lastUnderscore;
            underscores = sawUnderscore;
        }
        if (i < n && byteAt(pos + i) == '.' && first != '.') {
            if (lastUnderscoreEnd != -1 && i == lastUnderscoreEnd + 1) {
                throw syntaxErrAt(pos);
            }
            hasDotOrExp = true;
            i++;
            if (i < n && byteAt(pos + i) == '_') {
                throw syntaxErrAt(pos);
            }
            i = digits(pos, n, i, lastUnderscoreEnd);
            lastUnderscoreEnd = lastUnderscore;
            underscores |= sawUnderscore;
        } else if (first == '.') {
            i = digits(pos, n, i, lastUnderscoreEnd);
            lastUnderscoreEnd = lastUnderscore;
            underscores |= sawUnderscore;
        }
        if (i < n && (byteAt(pos + i) == 'e' || byteAt(pos + i) == 'E')) {
            if (lastUnderscoreEnd != -1 && i == lastUnderscoreEnd + 1) {
                throw syntaxErrAt(pos);
            }
            hasDotOrExp = true;
            i++;
            if (i < n && (byteAt(pos + i) == '+' || byteAt(pos + i) == '-')) {
                i++;
            }
            if (i >= n || !isDigit(byteAt(pos + i))) {
                throw syntaxErrAt(pos);
            }
            i = digits(pos, n, i, lastUnderscoreEnd);
            lastUnderscoreEnd = lastUnderscore;
            underscores |= sawUnderscore;
        }
        if (lastUnderscoreEnd != -1 && i == lastUnderscoreEnd + 1) {
            throw syntaxErrAt(pos);
        }
        if (!hasDotOrExp && !underscores && i < 10) {
            long v = 0;
            for (int k = 0; k < i; k++) {
                v = v * 10 + (byteAt(pos + k) - '0');
            }
            value[0] = v;
        } else {
            StringBuilder digits = new StringBuilder(i);
            for (int k = 0; k < i; k++) {
                if (contents[pos + k] != '_') {
                    digits.append((char) byteAt(pos + k));
                }
            }
            try {
                value[0] = Double.parseDouble(digits.toString());
            } catch (NumberFormatException e) {
                addError(pos, "Invalid number");
                throw new Json.Failure(Json.SYNTAX_ERROR);
            }
        }
        return i;
    }

    // the digits! macro's state, which it shares with parse_number_text
    private int lastUnderscore;
    private boolean sawUnderscore;

    private int digits(int pos, int n, int i, int lastUnderscoreEnd) {
        lastUnderscore = lastUnderscoreEnd;
        sawUnderscore = false;
        while (i < n) {
            int b = byteAt(pos + i);
            if (isDigit(b)) {
                i++;
            } else if (b == '_') {
                if (lastUnderscore != -1 && i == lastUnderscore + 1) {
                    throw syntaxErrAt(pos);
                }
                if (i == 0) {
                    throw syntaxErrAt(pos);
                }
                lastUnderscore = i;
                sawUnderscore = true;
                i++;
            } else {
                break;
            }
        }
        return i;
    }

    private int parseRadixNumber(int pos, int end, int radix, int prefixLen, boolean legacyOctal, double[] out) {
        int n = end - pos;
        int i = prefixLen;
        double value = 0;
        boolean isFirst = true;
        boolean isInvalidLegacyOctal = false;
        int lastUnderscoreEnd = -1;
        while (i < n) {
            int c = byteAt(pos + i);
            int digit;
            if (c == '_') {
                if (lastUnderscoreEnd != -1 && i == lastUnderscoreEnd + 1 || isFirst || legacyOctal) {
                    throw syntaxErrAt(pos);
                }
                lastUnderscoreEnd = i;
                i++;
                continue;
            } else if (c == '0' || c == '1') {
                digit = c - '0';
            } else if (c >= '2' && c <= '7') {
                if (radix == 2) {
                    throw syntaxErrAt(pos);
                }
                digit = c - '0';
            } else if (c == '8' || c == '9') {
                if (legacyOctal) {
                    isInvalidLegacyOctal = true;
                } else if (radix < 10) {
                    throw syntaxErrAt(pos);
                }
                digit = c - '0';
            } else if (c >= 'A' && c <= 'F') {
                if (radix != 16) {
                    throw syntaxErrAt(pos);
                }
                digit = c - 'A' + 10;
            } else if (c >= 'a' && c <= 'f') {
                if (radix != 16) {
                    throw syntaxErrAt(pos);
                }
                digit = c - 'a' + 10;
            } else {
                break;
            }
            value = value * radix + digit;
            i++;
            isFirst = false;
        }
        if (isFirst) {
            throw syntaxErrAt(pos);
        }
        if (lastUnderscoreEnd != -1 && i == lastUnderscoreEnd + 1) {
            throw syntaxErrAt(pos);
        }
        if (isInvalidLegacyOctal) {
            String t = text(pos, pos + i);
            try {
                value = Double.parseDouble(t);
            } catch (NumberFormatException e) {
                addError(pos, "Invalid number " + t);
                throw new Json.Failure(Json.SYNTAX_ERROR);
            }
        }
        out[0] = value;
        return i;
    }

    private Json.Expr parseScalarCold() {
        int c = cursor;
        int start = posAt(c);
        int first = byteAt(start);
        if (first >= 0x80 || first == 0x0B || first == 0x0C) {
            int p = skipUnicodeWs();
            if (p == EOF) {
                tokenStart = len;
                throw unexpected(cursor);
            }
            if (p != start) {
                if (p == posAt(cursor)) {
                    return parseValue();
                }
                return parseScalarTail(p);
            }
        }
        if (first == '\\') {
            return parseEscapedIdentifier(start);
        }
        if (isIdentifierStart(first) || first >= 0x80) {
            throw unexpected(c);
        }
        throw junkByteError(c, start, first);
    }

    private Json.Expr parseScalarTail(int pos) {
        int end = posAt(cursor + 1);
        tokenStart = pos;
        int first = byteAt(pos);
        if (first == 't' && startsWith(pos, end, "true") && restIsWsCold(pos + 4, end) ||
            first == 'f' && startsWith(pos, end, "false") && restIsWsCold(pos + 5, end) ||
            first == 'n' && startsWith(pos, end, "null") && restIsWsCold(pos + 4, end)) {
            cursor++;
            return keyword(first);
        }
        if (first == '-') {
            return parseNegativeNumberAt(pos);
        }
        if (isDigit(first) || first == '.') {
            double[] value = new double[1];
            int used = parseNumberText(pos, end, value);
            if (!restIsWsCold(pos + used, end)) {
                throw numberTrailingJunk(pos + used);
            }
            cursor++;
            return number(value[0]);
        }
        if (isIdentifierStart(first)) {
            int identLen = identLen(pos, end);
            addRangeError(pos, identLen, "Unexpected " + text(pos, pos + identLen));
            throw new Json.Failure(Json.PARSER_ERROR);
        }
        if (first == '\\') {
            return parseEscapedIdentifier(pos);
        }
        throw junkByteError(cursor, pos, first);
    }

    private Json.Expr parseEscapedIdentifier(int start) {
        int end = posAt(cursor + 1);
        tokenStart = start;
        int i = start;
        while (i < end) {
            int c = byteAt(i);
            if (c == '\\') {
                if (i + 1 >= end || contents[i + 1] != 'u') {
                    throw syntaxError();
                }
                i += 2;
                for (int k = 0; k < 4; k++) {
                    if (i >= end || hexDigit(byteAt(i)) < 0) {
                        throw syntaxError();
                    }
                    i++;
                }
                continue;
            }
            if (!isIdentifierContinue(c)) {
                break;
            }
            i++;
        }
        if (!restIsWsCold(i, end)) {
            throw junkByteError(cursor, i, byteAt(i));
        }
        String decoded = decodeEscapes(start, i);
        if ("true".equals(decoded) || "false".equals(decoded) || "null".equals(decoded)) {
            cursor++;
            return keyword(decoded.charAt(0));
        }
        throw unexpected(cursor);
    }

    // decode_string_escapes::<false>: a raw control character is an error, every escape JSON's or one of \v \xHH \8 \9
    private String decodeEscapes(int from, int to) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(to - from);
        int i = from;
        while (i < to) {
            int c = codePointAt(i);
            i += width(i);
            if (c != '\\') {
                if (c < 0x20) {
                    if (c == 0x0A || c == 0x0D) {
                        throw addDefaultError("Unterminated string literal");
                    }
                    throw syntaxError();
                }
                pushCodePoint(buf, c);
                continue;
            }
            if (i >= to) {
                break;
            }
            int c2 = codePointAt(i);
            i += width(i);
            switch (c2) {
                case 'b':
                    buf.write(0x08);
                    break;
                case 'f':
                    buf.write(0x0C);
                    break;
                case 'n':
                    buf.write(0x0A);
                    break;
                case 'r':
                    buf.write(0x0D);
                    break;
                case 't':
                    buf.write(0x09);
                    break;
                case 'v':
                    buf.write(0x0B);
                    break;
                case '8':
                case '9':
                    buf.write(c2);
                    break;
                case 'x': {
                    int value = 0;
                    for (int k = 0; k < 2; k++) {
                        int d = i < to ? hexDigit(codePointAt(i)) : -1;
                        if (d < 0) {
                            throw syntaxError();
                        }
                        i += width(i);
                        value = value * 16 | d;
                    }
                    pushCodePoint(buf, value);
                    break;
                }
                case '"':
                case '\\':
                case '/':
                    buf.write(c2);
                    break;
                case 'u': {
                    int value = 0;
                    for (int k = 0; k < 4; k++) {
                        int d = i < to ? hexDigit(codePointAt(i)) : -1;
                        if (d < 0) {
                            throw syntaxError();
                        }
                        i += width(i);
                        value = value * 16 + d;
                    }
                    if (Character.isHighSurrogate((char) value)) {
                        int trail = trailSurrogateEscape(i, to);
                        if (trail >= 0) {
                            value = Character.toCodePoint((char) value, (char) trail);
                            i += 6;
                        }
                    }
                    pushCodePoint(buf, value);
                    break;
                }
                default:
                    throw syntaxError();
            }
        }
        return new String(buf.toByteArray(), UTF_8);
    }

    // read_trail_surrogate_escape: the trail surrogate a \\uXXXX at i escapes, or -1
    private int trailSurrogateEscape(int i, int to) {
        if (i + 6 > to || contents[i] != '\\' || contents[i + 1] != 'u') {
            return -1;
        }
        int value = 0;
        for (int k = 2; k < 6; k++) {
            int d = hexDigit(byteAt(i + k));
            if (d < 0) {
                return -1;
            }
            value = value * 16 + d;
        }
        return Character.isLowSurrogate((char) value) ? value : -1;
    }

    // encode_wtf8_rune: a lone surrogate as its three bytes
    private static void pushCodePoint(ByteArrayOutputStream buf, int cp) {
        if (cp < 0x80) {
            buf.write(cp);
        } else if (cp < 0x800) {
            buf.write(0xC0 | cp >> 6);
            buf.write(0x80 | cp & 0x3F);
        } else if (cp < 0x10000) {
            buf.write(0xE0 | cp >> 12);
            buf.write(0x80 | cp >> 6 & 0x3F);
            buf.write(0x80 | cp & 0x3F);
        } else {
            buf.write(0xF0 | cp >> 18);
            buf.write(0x80 | cp >> 12 & 0x3F);
            buf.write(0x80 | cp >> 6 & 0x3F);
            buf.write(0x80 | cp & 0x3F);
        }
    }

    // json.rs skip_ws_and_comments: the position after whitespace and comments, or -1 at the end
    private int skipWsAndComments(int p) {
        while (p < len) {
            int b = byteAt(p);
            if (b == ' ' || b == '\t' || b == '\n' || b == '\r' || b == 0x0B || b == 0x0C) {
                p++;
            } else if (b == '/') {
                int next = p + 1 < len ? byteAt(p + 1) : EOF;
                if (next == '/') {
                    p += 2;
                    while (p < len && contents[p] != '\n' && contents[p] != '\r' && !JsonIndex.isLsPs(contents, p)) {
                        p++;
                    }
                } else if (next == '*') {
                    int close = indexOf(contents, p + 2, '*', '/');
                    if (close < 0) {
                        return -1;
                    }
                    p = close + 2;
                } else {
                    return p;
                }
            } else if (b >= 0x80) {
                if (!isExoticWhitespace(codePointAt(p))) {
                    return p;
                }
                p += width(p);
            } else {
                return p;
            }
        }
        return -1;
    }

    private int identLen(int from, int to) {
        int i = from;
        while (i < to && isIdentifierContinue(byteAt(i))) {
            i++;
        }
        return Math.max(i - from, 1);
    }

    private boolean startsWith(int from, int to, String word) {
        if (to - from < word.length()) {
            return false;
        }
        for (int k = 0; k < word.length(); k++) {
            if (contents[from + k] != word.charAt(k)) {
                return false;
            }
        }
        return true;
    }

    private int codePointAt(int p) {
        int b = byteAt(p);
        int w = width(p);
        if (w == 1) {
            return b;
        }
        int cp = b & (0xFF >> (w + 1));
        for (int k = 1; k < w && p + k < len; k++) {
            cp = cp << 6 | contents[p + k] & 0x3F;
        }
        return cp;
    }

    private int width(int p) {
        int b = byteAt(p);
        int w = b < 0x80 ? 1 : b >= 0xF0 ? 4 : b >= 0xE0 ? 3 : b >= 0xC0 ? 2 : 1;
        return Math.min(w, len - p);
    }

    private static int indexOf(byte[] s, int from, byte a, byte b) {
        return indexOf(s, from, s.length, a, b);
    }

    private static int indexOf(byte[] s, int from, int to, byte a, byte b) {
        for (int i = from; i + 1 < to; i++) {
            if (s[i] == a && s[i + 1] == b) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOf(byte[] s, int from, char a, char b) {
        return indexOf(s, from, s.length, (byte) a, (byte) b);
    }

    private static int indexOf(byte[] s, int from, int to, char a, char b) {
        return indexOf(s, from, to, (byte) a, (byte) b);
    }

    private static boolean isDigit(int b) {
        return b >= '0' && b <= '9';
    }

    private static int hexDigit(int c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private static boolean isIdentifierStart(int c) {
        return c == '$' || c == '_' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private static boolean isIdentifierContinue(int c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    // u8::is_ascii_whitespace: space, \t, \n, \x0C, \r
    private static boolean isAsciiWhitespace(int b) {
        return b == ' ' || b == '\t' || b == '\n' || b == 0x0C || b == '\r';
    }

    /**
     * {@code is_exotic_whitespace}: \v, \f, U+2028, U+2029, U+FEFF and the space separators.
     */
    static boolean isExoticWhitespace(int cp) {
        switch (cp) {
            case 0x0B:
            case 0x0C:
            case 0x2028:
            case 0x2029:
            case 0xFEFF:
            case 0x20:
            case 0xA0:
            case 0x1680:
            case 0x202F:
            case 0x205F:
            case 0x3000:
                return true;
            default:
                return cp >= 0x2000 && cp <= 0x200A;
        }
    }
}
