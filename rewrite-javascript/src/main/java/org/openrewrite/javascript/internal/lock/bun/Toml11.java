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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Port of bun's src/parsers/toml.rs {@code TOML} (bun 1.4.0, {@link BunRelease#parsesTomlStrictly}): a TOML v1.1.0
 * scanner and parser, every error fatal, with bunfig's redaction (a key's or a character's text is left out of a
 * message). A date or time value is its text, as bun's string tagged {@code toml_datetime} is; the document is taken as
 * valid UTF-8, as a Java string's encoding is.
 */
final class Toml11 {

    private static final long MAX_SAFE_INTEGER = (1L << 53) - 1;
    private static final String BARE_CR = "Bare carriage return is not allowed; use \\r\\n or \\n";
    private static final String UNDERSCORE_IN_NUMBER = "Underscores in numbers must be surrounded by digits";

    /**
     * How a table or array came to exist, which decides whether later syntax may extend it.
     */
    private enum Kind {
        HEADER, HEADER_IMPLICIT, DOTTED, ARRAY_ELEM, INLINE, AOT_ARRAY, STATIC_ARRAY
    }

    private static final class Meta {
        final Kind kind;
        final int block;

        Meta(Kind kind, int block) {
            this.kind = kind;
            this.block = block;
        }
    }

    private static final class KeySeg {
        final String text;
        final int pos;

        KeySeg(String text, int pos) {
            this.text = text;
            this.pos = pos;
        }
    }

    private final byte[] src;
    private final Json.Log log;
    private int pos;
    // keyed by the table's property list or the array's item list
    private final Map<Object, Meta> meta = new IdentityHashMap<>();
    private int block;

    private Toml11(byte[] src, Json.Log log) {
        this.src = src;
        this.log = log;
    }

    static Json.Expr parse(String source, Json.Log log) {
        return new Toml11(source.getBytes(UTF_8), log).parseRoot();
    }

    // ── error helpers (redacted) ──

    private Json.Failure err(int at, String msg) {
        log.add(msg, at, 0);
        return new Json.Failure(Json.SYNTAX_ERROR);
    }

    // {before} '{key}'{after}, the key left out when redacting
    private Json.Failure errKeyed(int at, String before, String after) {
        return err(at, before + after);
    }

    private Json.Failure errChar(int at, String what) {
        return err(at, at >= src.length ? what + " end of file" : what + " (redacted)");
    }

    private Json.Failure errUnquotedString(int at) {
        return err(at, "Strings must be quoted");
    }

    // ── byte cursor ──

    private int peek() {
        return peekAt(pos);
    }

    private int peekAt(int at) {
        return at >= 0 && at < src.length ? src[at] & 0xFF : 0;
    }

    private boolean atEof() {
        return pos >= src.length;
    }

    private static boolean isBareKeyChar(int c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_';
    }

    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private void skipWs() {
        while (peek() == ' ' || peek() == '\t') {
            pos++;
        }
    }

    private void expectNewline() {
        int c = peek();
        if (c == '\n') {
            pos++;
        } else if (c == '\r') {
            if (peekAt(pos + 1) != '\n') {
                throw err(pos, BARE_CR);
            }
            pos += 2;
        } else {
            throw errChar(pos, "Expected a newline but found");
        }
    }

    private void skipComment() {
        pos++;
        while (true) {
            int c = peek();
            if (c == 0 && atEof() || c == '\n') {
                return;
            }
            if (c == '\r') {
                if (peekAt(pos + 1) == '\n') {
                    return;
                }
                throw err(pos, BARE_CR);
            }
            if (c != '\t' && (c < 0x20 || c == 0x7F)) {
                throw errChar(pos, "Control character is not allowed in a comment:");
            }
            pos++;
        }
    }

    private void skipWsCommentNewline() {
        while (true) {
            int c = peek();
            if (c == ' ' || c == '\t') {
                pos++;
            } else if (c == '\n' || c == '\r') {
                expectNewline();
            } else if (c == '#') {
                skipComment();
            } else {
                return;
            }
        }
    }

    // ── scan modes ──

    private KeySeg scanKeySegment() {
        int at = pos;
        int c = peek();
        if (c == '"') {
            return new KeySeg(scanBasicString(false), at);
        }
        if (c == '\'') {
            return new KeySeg(scanLiteralString(false), at);
        }
        if (isBareKeyChar(c)) {
            while (isBareKeyChar(peek())) {
                pos++;
            }
            return new KeySeg(new String(src, at, pos - at, UTF_8), at);
        }
        throw errChar(at, "Expected a key but found");
    }

    private KeySeg scanKeyAfterSep() {
        skipWs();
        return scanKeySegment();
    }

    // after a key segment of a key/value pair: true for a dot, false for =
    private boolean scanKeyvalSepIsDot() {
        skipWs();
        if (peek() == '.') {
            pos++;
            return true;
        }
        if (peek() == '=') {
            pos++;
            return false;
        }
        throw errChar(pos, "Expected '=' after a key but found");
    }

    // after a key segment of a header: true for a dot, false for the close
    private boolean scanHeaderSepIsDot(boolean aot) {
        skipWs();
        if (peek() == '.') {
            pos++;
            return true;
        }
        if (peek() == ']') {
            pos++;
            if (aot) {
                if (peek() != ']') {
                    throw errChar(pos, "Expected ']]' to close an array-of-tables header but found");
                }
                pos++;
            }
            return false;
        }
        throw aot ? errChar(pos, "Expected ']]' to close an array-of-tables header but found") :
                errChar(pos, "Expected ']' to close a table header but found");
    }

    private void scanLineEnd(String after) {
        skipWs();
        if (peek() == '#') {
            skipComment();
        }
        if (atEof()) {
            return;
        }
        if (peek() == '\n' || peek() == '\r') {
            expectNewline();
            return;
        }
        throw err(pos, "Expected a newline or end of file after " + after);
    }

    private void expectValueTerminator() {
        int c = peek();
        if (c == 0 && atEof() || c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == ',' || c == ']' ||
            c == '}' || c == '#') {
            return;
        }
        throw errChar(pos, "Unexpected character after a value:");
    }

    // ── values ──

    // the value at the cursor: a string, number, date/time text or boolean, or an array or inline table opened
    private Json.Expr scanValue() {
        int at = pos;
        int c = peek();
        if (c == '"') {
            return Json.Expr.string(scanBasicString(startsWith(pos, "\"\"\"")));
        }
        if (c == '\'') {
            return Json.Expr.string(scanLiteralString(startsWith(pos, "'''")));
        }
        if (c == 't') {
            expectKeyword("true");
            return new Json.Expr(Json.Kind.BOOLEAN, true);
        }
        if (c == 'f') {
            expectKeyword("false");
            return new Json.Expr(Json.Kind.BOOLEAN, false);
        }
        if (c == '[') {
            pos++;
            return parseArray(at);
        }
        if (c == '{') {
            pos++;
            return parseInlineTable(at);
        }
        if (c == 'i' || c == 'n' || c == '+' || c == '-' || isDigit(c)) {
            return scanNumberOrDatetime();
        }
        if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
            throw errUnquotedString(at);
        }
        throw errChar(at, "Expected a value but found");
    }

    private boolean startsWith(int at, String text) {
        if (at + text.length() > src.length) {
            return false;
        }
        for (int k = 0; k < text.length(); k++) {
            if (src[at + k] != text.charAt(k)) {
                return false;
            }
        }
        return true;
    }

    private void expectKeyword(String word) {
        int at = pos;
        if (startsWith(pos, word) && !isBareKeyChar(peekAt(pos + word.length()))) {
            pos += word.length();
            return;
        }
        throw errUnquotedString(at);
    }

    private Json.Expr scanNumberOrDatetime() {
        if (isDigit(peek())) {
            int d1 = digitRunLen(pos);
            if (d1 == 4 && peekAt(pos + 4) == '-') {
                String text = scanDatetimeFromDate();
                expectValueTerminator();
                return Json.Expr.string(truncateFractionalSeconds(text));
            }
            if (d1 == 2 && peekAt(pos + 2) == ':') {
                int start = pos;
                scanTimeDigits();
                expectValueTerminator();
                return Json.Expr.string(truncateFractionalSeconds(new String(src, start, pos - start, UTF_8)));
            }
        }
        return scanNumber();
    }

    private int digitRunLen(int start) {
        int i = start;
        while (isDigit(peekAt(i))) {
            i++;
        }
        return i - start;
    }

    private int readDigits(int n, String what) {
        int value = 0;
        for (int k = 0; k < n; k++) {
            int c = peek();
            if (!isDigit(c)) {
                throw err(pos, what);
            }
            value = value * 10 + c - '0';
            pos++;
        }
        return value;
    }

    private String scanDatetimeFromDate() {
        int start = pos;
        int year = readDigits(4, "Invalid date: expected a 4-digit year");
        if (peek() != '-') {
            throw err(pos, "Invalid date: expected '-' after the year");
        }
        pos++;
        int month = readDigits(2, "Invalid date: expected a 2-digit month");
        if (peek() != '-') {
            throw err(pos, "Invalid date: expected '-' after the month");
        }
        pos++;
        int dayPos = pos;
        int day = readDigits(2, "Invalid date: expected a 2-digit day");
        if (month < 1 || month > 12) {
            throw err(start, "Invalid date: month must be between 01 and 12");
        }
        boolean leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
        int maxDay;
        switch (month) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                maxDay = 31;
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                maxDay = 30;
                break;
            default:
                maxDay = leap ? 29 : 28;
                break;
        }
        if (day < 1 || day > maxDay) {
            throw err(dayPos, "Invalid date: day is out of range for the month");
        }
        boolean hasTime;
        if (peek() == 'T' || peek() == 't') {
            pos++;
            hasTime = true;
        } else if (peek() == ' ' && isDigit(peekAt(pos + 1)) && isDigit(peekAt(pos + 2)) && peekAt(pos + 3) == ':') {
            pos++;
            hasTime = true;
        } else {
            hasTime = false;
        }
        if (hasTime) {
            scanTimeDigits();
            if (peek() == 'Z' || peek() == 'z') {
                pos++;
            } else if (peek() == '+' || peek() == '-') {
                pos++;
                int hour = readDigits(2, "Invalid date-time offset: expected 2-digit hours");
                if (peek() != ':') {
                    throw err(pos, "Invalid date-time offset: expected ':' between hours and minutes");
                }
                pos++;
                int minute = readDigits(2, "Invalid date-time offset: expected 2-digit minutes");
                if (hour > 23) {
                    throw err(start, "Invalid date-time offset: hours must be between 00 and 23");
                }
                if (minute > 59) {
                    throw err(start, "Invalid date-time offset: minutes must be between 00 and 59");
                }
            }
        }
        return new String(src, start, pos - start, UTF_8);
    }

    private void scanTimeDigits() {
        int start = pos;
        int hour = readDigits(2, "Invalid time: expected 2-digit hours");
        if (peek() != ':') {
            throw err(pos, "Invalid time: expected ':' after hours");
        }
        pos++;
        int minute = readDigits(2, "Invalid time: expected 2-digit minutes");
        if (hour > 23) {
            throw err(start, "Invalid time: hours must be between 00 and 23");
        }
        if (minute > 59) {
            throw err(start, "Invalid time: minutes must be between 00 and 59");
        }
        if (peek() == ':') {
            pos++;
            int secPos = pos;
            int second = readDigits(2, "Invalid time: expected 2-digit seconds");
            if (second > 60) {
                throw err(secPos, "Invalid time: seconds must be between 00 and 60");
            }
            if (peek() == '.') {
                pos++;
                if (!isDigit(peek())) {
                    throw err(pos, "Invalid time: expected at least one digit of fractional seconds");
                }
                while (isDigit(peek())) {
                    pos++;
                }
            }
        }
    }

    // TOML truncates fractional seconds past nine digits
    private static String truncateFractionalSeconds(String text) {
        int dot = text.indexOf('.');
        if (dot < 0) {
            return text;
        }
        int fracStart = dot + 1;
        int fracEnd = fracStart;
        while (fracEnd < text.length() && isDigit(text.charAt(fracEnd))) {
            fracEnd++;
        }
        if (fracEnd - fracStart <= 9) {
            return text;
        }
        return text.substring(0, fracStart + 9) + text.substring(fracEnd);
    }

    private void checkUnderscore(int radix) {
        if (!isDigitOf(peekAt(pos - 1), radix) || !isDigitOf(peekAt(pos + 1), radix)) {
            throw err(pos, UNDERSCORE_IN_NUMBER);
        }
        pos++;
    }

    private static boolean isDigitOf(int c, int radix) {
        switch (radix) {
            case 16:
                return isDigit(c) || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
            case 8:
                return c >= '0' && c <= '7';
            case 2:
                return c == '0' || c == '1';
            default:
                return isDigit(c);
        }
    }

    private void scanDecimalDigits() {
        while (true) {
            int c = peek();
            if (isDigit(c)) {
                pos++;
            } else if (c == '_') {
                checkUnderscore(10);
            } else {
                return;
            }
        }
    }

    private Json.Expr scanNumber() {
        int start = pos;
        boolean negative = false;
        if (peek() == '-') {
            pos++;
            negative = true;
        } else if (peek() == '+') {
            pos++;
        }
        if (startsWith(pos, "inf") && !isBareKeyChar(peekAt(pos + 3))) {
            pos += 3;
            expectValueTerminator();
            return number(negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        }
        if (startsWith(pos, "nan") && !isBareKeyChar(peekAt(pos + 3))) {
            pos += 3;
            expectValueTerminator();
            return number(Double.NaN);
        }
        if (peek() == '0' && (peekAt(pos + 1) == 'x' || peekAt(pos + 1) == 'o' || peekAt(pos + 1) == 'b')) {
            if (negative || src[start] == '+') {
                throw err(start, "A sign is not allowed on hexadecimal, octal, or binary integers");
            }
            return scanRadixInteger();
        }
        if (!isDigit(peek())) {
            if (start == pos && (peek() >= 'a' && peek() <= 'z' || peek() >= 'A' && peek() <= 'Z')) {
                throw errUnquotedString(start);
            }
            throw errChar(pos, "Expected a number but found");
        }
        int intStart = pos;
        long magnitude = 0;
        boolean intOverflow = false;
        int digits = 0;
        while (true) {
            int c = peek();
            if (isDigit(c)) {
                digits++;
                // the magnitude is unsigned: at most 2^64 - 1
                if (!intOverflow) {
                    long next = magnitude * 10 + (c - '0');
                    if (Long.compareUnsigned(magnitude, Long.divideUnsigned(-1L, 10)) > 0 ||
                        Long.compareUnsigned(next, magnitude * 10) < 0) {
                        intOverflow = true;
                        magnitude = 0;
                    } else {
                        magnitude = next;
                    }
                }
                pos++;
            } else if (c == '_') {
                checkUnderscore(10);
            } else {
                break;
            }
        }
        if (digits > 1 && src[intStart] == '0') {
            throw err(intStart, "Leading zeros are not allowed in numbers");
        }
        boolean isFloat = false;
        if (peek() == '.') {
            isFloat = true;
            pos++;
            if (!isDigit(peek())) {
                throw err(pos, "A decimal point must be followed by at least one digit");
            }
            scanDecimalDigits();
        }
        if (peek() == 'e' || peek() == 'E') {
            isFloat = true;
            pos++;
            if (peek() == '+' || peek() == '-') {
                pos++;
            }
            if (!isDigit(peek())) {
                throw err(pos, "An exponent must contain at least one digit");
            }
            scanDecimalDigits();
        }
        expectValueTerminator();
        if (isFloat) {
            String raw = new String(src, start, pos - start, UTF_8).replace("_", "");
            try {
                return number(Double.parseDouble(raw));
            } catch (NumberFormatException e) {
                throw err(start, "Invalid number");
            }
        }
        // |i64::MIN| or i64::MAX
        long signedLimit = negative ? Long.MIN_VALUE : Long.MAX_VALUE;
        if (intOverflow || Long.compareUnsigned(magnitude, signedLimit) > 0) {
            throw err(start, "Integer is outside the 64-bit signed range");
        }
        if (Long.compareUnsigned(magnitude, MAX_SAFE_INTEGER) > 0) {
            throw err(start, "Integer cannot be losslessly represented as a JavaScript number; it must be within " +
                             "+/-(2^53 - 1)");
        }
        // the integer negated: -0 is 0
        return number(negative ? (double) -magnitude : (double) magnitude);
    }

    private Json.Expr scanRadixInteger() {
        int start = pos;
        int radixChar = peekAt(pos + 1);
        int radix = radixChar == 'x' ? 16 : radixChar == 'o' ? 8 : 2;
        pos += 2;
        if (!isDigitOf(peek(), radix)) {
            throw err(pos, "Expected at least one digit after the radix prefix");
        }
        long value = 0;
        boolean overflow = false;
        while (true) {
            int c = peek();
            if (isDigitOf(c, radix)) {
                int digit = Character.digit(c, 16);
                if (!overflow) {
                    if (Long.compareUnsigned(value, Long.divideUnsigned(-1L, radix)) > 0) {
                        overflow = true;
                    } else {
                        long next = value * radix + digit;
                        if (Long.compareUnsigned(next, value * radix) < 0) {
                            overflow = true;
                        } else {
                            value = next;
                        }
                    }
                }
                pos++;
            } else if (c == '_') {
                checkUnderscore(radix);
            } else if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || isDigit(c)) {
                throw errChar(pos, "Invalid digit in number:");
            } else {
                break;
            }
        }
        expectValueTerminator();
        if (overflow || Long.compareUnsigned(value, Long.MAX_VALUE) > 0) {
            throw err(start, "Integer is outside the 64-bit signed range");
        }
        if (value > MAX_SAFE_INTEGER) {
            throw err(start, "Integer cannot be losslessly represented as a JavaScript number; it must be within " +
                             "+/-(2^53 - 1)");
        }
        return number(value);
    }

    private static Json.Expr number(double value) {
        return new Json.Expr(Json.Kind.NUMBER, value);
    }

    // ── strings ──

    // the quote run at the cursor: its length, which closes a multi-line string from three to five
    private int quoteRun(int quote) {
        int run = 0;
        while (peekAt(pos + run) == quote) {
            run++;
        }
        if (run > 5) {
            throw err(pos, "Too many quotes at the end of a multi-line string");
        }
        return run;
    }

    private String scanBasicString(boolean multiline) {
        int openPos = pos;
        pos += multiline ? 3 : 1;
        if (multiline) {
            skipLeadingNewline();
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        while (true) {
            if (atEof()) {
                throw err(openPos, "Unterminated string");
            }
            int c = peek();
            if (c == '"') {
                if (!multiline) {
                    pos++;
                    return text(buf);
                }
                int run = quoteRun('"');
                if (run >= 3) {
                    for (int k = 0; k < run - 3; k++) {
                        buf.write('"');
                    }
                    pos += run;
                    return text(buf);
                }
                for (int k = 0; k < run; k++) {
                    buf.write('"');
                }
                pos += run;
            } else if (c == '\\') {
                if (multiline) {
                    int i = pos + 1;
                    while (peekAt(i) == ' ' || peekAt(i) == '\t') {
                        i++;
                    }
                    if (peekAt(i) == '\n' || peekAt(i) == '\r' && peekAt(i + 1) == '\n') {
                        // a line-ending backslash trims the whitespace up to the next character
                        pos = i;
                        while (true) {
                            if (peek() == ' ' || peek() == '\t' || peek() == '\n') {
                                pos++;
                            } else if (peek() == '\r' && peekAt(pos + 1) == '\n') {
                                pos += 2;
                            } else {
                                break;
                            }
                        }
                        continue;
                    }
                }
                scanEscape(buf);
            } else if (c == '\n') {
                if (!multiline) {
                    throw err(openPos, "Unterminated string; newlines must be escaped in basic strings");
                }
                buf.write('\n');
                pos++;
            } else if (c == '\r') {
                if (peekAt(pos + 1) != '\n') {
                    throw err(pos, BARE_CR);
                }
                if (!multiline) {
                    throw err(openPos, "Unterminated string; newlines must be escaped in basic strings");
                }
                buf.write('\n');
                pos += 2;
            } else if (c == '\t') {
                buf.write('\t');
                pos++;
            } else if (c < 0x20 || c == 0x7F) {
                throw errChar(pos, "Control character must be escaped in a string:");
            } else {
                buf.write(c);
                pos++;
            }
        }
    }

    private void skipLeadingNewline() {
        if (peek() == '\n') {
            pos++;
        } else if (peek() == '\r' && peekAt(pos + 1) == '\n') {
            pos += 2;
        }
    }

    private static String text(ByteArrayOutputStream buf) {
        return new String(buf.toByteArray(), UTF_8);
    }

    private void scanEscape(ByteArrayOutputStream buf) {
        int escapePos = pos;
        pos++;
        int c = peek();
        pos++;
        switch (c) {
            case 'b':
                buf.write(0x08);
                break;
            case 't':
                buf.write('\t');
                break;
            case 'n':
                buf.write('\n');
                break;
            case 'f':
                buf.write(0x0C);
                break;
            case 'r':
                buf.write('\r');
                break;
            case '"':
                buf.write('"');
                break;
            case '\\':
                buf.write('\\');
                break;
            case 'e':
                buf.write(0x1B);
                break;
            case 'x':
                appendScalar(buf, readHexCodepoint("hex escape", 2, escapePos), escapePos);
                break;
            case 'u':
                appendScalar(buf, readHexCodepoint("Unicode escape", 4, escapePos), escapePos);
                break;
            case 'U':
                appendScalar(buf, readHexCodepoint("Unicode escape", 8, escapePos), escapePos);
                break;
            default:
                if (c == 0 && pos - 1 >= src.length) {
                    throw err(escapePos, "Unterminated escape sequence");
                }
                pos--;
                throw errChar(pos, "Invalid escape sequence:");
        }
    }

    private long readHexCodepoint(String what, int digits, int escapePos) {
        long value = 0;
        for (int k = 0; k < digits; k++) {
            int d = Character.digit(peek(), 16);
            if (d < 0 || peek() >= 0x80) {
                throw err(escapePos, "A " + what + " must be followed by exactly " + digits + " hex digits");
            }
            value = value * 16 + d;
            pos++;
        }
        return value;
    }

    private void appendScalar(ByteArrayOutputStream buf, long cp, int escapePos) {
        if (cp > 0x10FFFF || cp >= 0xD800 && cp <= 0xDFFF) {
            throw err(escapePos, "Escaped code point must be a Unicode scalar value");
        }
        byte[] bytes = new String(Character.toChars((int) cp)).getBytes(UTF_8);
        buf.write(bytes, 0, bytes.length);
    }

    private String scanLiteralString(boolean multiline) {
        int openPos = pos;
        pos += multiline ? 3 : 1;
        if (multiline) {
            skipLeadingNewline();
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        while (true) {
            if (atEof()) {
                throw err(openPos, "Unterminated string");
            }
            int c = peek();
            if (c == '\'') {
                if (!multiline) {
                    pos++;
                    return text(buf);
                }
                int run = quoteRun('\'');
                if (run >= 3) {
                    for (int k = 0; k < run - 3; k++) {
                        buf.write('\'');
                    }
                    pos += run;
                    return text(buf);
                }
                for (int k = 0; k < run; k++) {
                    buf.write('\'');
                }
                pos += run;
            } else if (c == '\n') {
                if (!multiline) {
                    throw err(openPos, "Unterminated string; literal strings cannot contain newlines");
                }
                buf.write('\n');
                pos++;
            } else if (c == '\r') {
                if (peekAt(pos + 1) != '\n') {
                    throw err(pos, BARE_CR);
                }
                if (!multiline) {
                    throw err(openPos, "Unterminated string; literal strings cannot contain newlines");
                }
                buf.write('\n');
                pos += 2;
            } else if (c == '\t') {
                buf.write('\t');
                pos++;
            } else if (c < 0x20 || c == 0x7F) {
                throw errChar(pos, "Control character is not allowed in a literal string:");
            } else {
                buf.write(c);
                pos++;
            }
        }
    }

    // ── parser ──

    private Json.Expr parseRoot() {
        if (src.length >= 3 && (src[0] & 0xFF) == 0xEF && (src[1] & 0xFF) == 0xBB && (src[2] & 0xFF) == 0xBF) {
            pos = 3;
        }
        Json.Expr root = new Json.Expr(Json.Kind.OBJECT, new ArrayList<Json.Property>());
        Json.Expr current = root;
        while (true) {
            skipWsCommentNewline();
            if (atEof()) {
                return root;
            }
            if (peek() == '[') {
                int headerPos = pos;
                pos++;
                boolean aot = peek() == '[';
                if (aot) {
                    pos++;
                }
                current = parseTableHeader(root, aot, headerPos);
                scanLineEnd("a table header");
            } else {
                parseKeyval(current, scanKeySegment());
                scanLineEnd("a key/value pair");
            }
        }
    }

    private Json.Expr parseTableHeader(Json.Expr root, boolean aot, int headerPos) {
        List<KeySeg> path = new ArrayList<>();
        path.add(scanKeyAfterSep());
        while (scanHeaderSepIsDot(aot)) {
            path.add(scanKeyAfterSep());
        }
        block++;
        return navigateHeader(root, path, aot, headerPos);
    }

    private Json.Expr navigateHeader(Json.Expr root, List<KeySeg> path, boolean isAot, int headerPos) {
        Json.Expr cur = root;
        for (int i = 0; i < path.size(); i++) {
            KeySeg seg = path.get(i);
            boolean last = i + 1 == path.size();
            Json.Expr existing = cur.get(seg.text);
            if (existing == null) {
                if (last && isAot) {
                    Json.Expr array = newArray(Kind.AOT_ARRAY);
                    Json.Expr elem = appendAotElem(array);
                    insertKey(cur, seg, array);
                    cur = elem;
                } else {
                    Json.Expr table = newTable(last ? Kind.HEADER : Kind.HEADER_IMPLICIT);
                    insertKey(cur, seg, table);
                    cur = table;
                }
            } else if (existing.isObject()) {
                Meta m = meta.get(existing.properties());
                if (last) {
                    if (isAot) {
                        throw errKeyed(headerPos, "Cannot redefine table", " as an array of tables");
                    }
                    if (m.kind == Kind.HEADER_IMPLICIT) {
                        meta.put(existing.properties(), new Meta(Kind.HEADER, block));
                        cur = existing;
                    } else if (m.kind == Kind.INLINE) {
                        throw errKeyed(headerPos, "Cannot redefine inline table", "");
                    } else {
                        throw errKeyed(headerPos, "Cannot redefine table", "");
                    }
                } else {
                    if (m.kind == Kind.INLINE) {
                        throw errKeyed(headerPos, "Cannot extend inline table", "");
                    }
                    cur = existing;
                }
            } else if (existing.isArray()) {
                Meta m = meta.get(existing.items());
                if (m.kind != Kind.AOT_ARRAY) {
                    throw errKeyed(headerPos, "Cannot extend array", "");
                }
                if (last) {
                    if (!isAot) {
                        throw errKeyed(headerPos, "Cannot redefine array of tables", " as a table");
                    }
                    cur = appendAotElem(existing);
                } else {
                    List<Json.Expr> items = existing.items();
                    cur = items.get(items.size() - 1);
                }
            } else {
                throw errKeyed(headerPos, "Cannot redefine key", last && isAot ? " as an array of tables" :
                        " as a table");
            }
        }
        return cur;
    }

    private void parseKeyval(Json.Expr table, KeySeg first) {
        List<KeySeg> path = new ArrayList<>();
        path.add(first);
        while (scanKeyvalSepIsDot()) {
            path.add(scanKeyAfterSep());
        }
        skipWs();
        if (peek() == '\n' || peek() == '\r') {
            throw err(pos, "Missing value after '='; values must be on the same line");
        }
        if (peek() == 0 && atEof()) {
            throw err(pos, "Missing value after '='");
        }
        assignPath(table, path, scanValue());
    }

    private void assignPath(Json.Expr table, List<KeySeg> path, Json.Expr value) {
        Json.Expr cur = table;
        for (KeySeg seg : path.subList(0, path.size() - 1)) {
            Json.Expr found = cur.get(seg.text);
            if (found == null) {
                Json.Expr dotted = newTable(Kind.DOTTED);
                insertKey(cur, seg, dotted);
                cur = dotted;
            } else {
                if (!found.isObject()) {
                    throw errKeyed(seg.pos, "Cannot redefine key", "");
                }
                Meta m = meta.get(found.properties());
                if (m.kind != Kind.DOTTED || m.block != block) {
                    throw errKeyed(seg.pos, "Cannot extend table", " with a dotted key");
                }
                cur = found;
            }
        }
        insertKey(cur, path.get(path.size() - 1), value);
    }

    private Json.Expr parseArray(int at) {
        Json.Expr array = newArray(Kind.STATIC_ARRAY);
        while (true) {
            skipWsCommentNewline();
            if (peek() == ']') {
                pos++;
                return array;
            }
            if (atEof()) {
                throw err(pos, "Unterminated array; expected ']'");
            }
            array.items().add(scanValue());
            if (!scanListSepIsComma(']', "Expected ',' or ']' in an array but found")) {
                return array;
            }
        }
    }

    private boolean scanListSepIsComma(char close, String what) {
        skipWsCommentNewline();
        if (peek() == ',') {
            pos++;
            return true;
        }
        if (peek() == close) {
            pos++;
            return false;
        }
        throw errChar(pos, what);
    }

    private Json.Expr parseInlineTable(int at) {
        int outerBlock = block;
        block++;
        Json.Expr table = newTable(Kind.DOTTED);
        while (true) {
            skipWsCommentNewline();
            if (peek() == '}') {
                pos++;
                break;
            }
            if (atEof()) {
                throw err(pos, "Unterminated inline table; expected '}'");
            }
            parseKeyval(table, scanKeySegment());
            if (!scanListSepIsComma('}', "Expected ',' or '}' in an inline table but found")) {
                break;
            }
        }
        meta.put(table.properties(), new Meta(Kind.INLINE, block));
        block = outerBlock;
        return table;
    }

    private Json.Expr newTable(Kind kind) {
        Json.Expr table = new Json.Expr(Json.Kind.OBJECT, new ArrayList<Json.Property>());
        meta.put(table.properties(), new Meta(kind, block));
        return table;
    }

    private Json.Expr newArray(Kind kind) {
        Json.Expr array = new Json.Expr(Json.Kind.ARRAY, new ArrayList<Json.Expr>());
        meta.put(array.items(), new Meta(kind, block));
        return array;
    }

    private Json.Expr appendAotElem(Json.Expr array) {
        Json.Expr elem = newTable(Kind.ARRAY_ELEM);
        array.items().add(elem);
        return elem;
    }

    private void insertKey(Json.Expr obj, KeySeg seg, Json.Expr value) {
        if (obj.get(seg.text) != null) {
            throw errKeyed(seg.pos, "Cannot redefine key", "");
        }
        obj.properties().add(new Json.Property(Json.Expr.string(seg.text), value));
    }
}
