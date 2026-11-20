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

/**
 * Port of bun's src/interchange/toml/lexer.zig {@code Lexer} (bun 1.3.10), over UTF-16 code points: what it returns
 * an error for stops the parse, and {@code expect} and {@code unexpected} only log. Its quirks are kept: a bare word
 * (with {@code @}, {@code $}, {@code -} and {@code :}) is an identifier, a {@code #} or a {@code ;} after a newline
 * starts a comment whose first character is never looked at, and {@code \f} and {@code \t} escapes are swapped.
 */
final class TomlLexer {

    enum T {
        END_OF_FILE, OPEN_BRACKET, OPEN_BRACKET_DOUBLE, CLOSE_BRACKET, CLOSE_BRACKET_DOUBLE, OPEN_BRACE,
        CLOSE_BRACE, NUMERIC_LITERAL, COMMA, STRING_LITERAL, DOT, EQUAL, TRUE, FALSE, COLON, IDENTIFIER, PLUS, MINUS,
        EMPTY_ARRAY;

        // @tagName, as expected() prints it
        String tagName() {
            return "t_" + name().toLowerCase();
        }
    }

    private final String source;
    private final int len;
    private final Json.Log log;

    int start;
    int end;
    private int current;
    private int codePoint = -1;
    T token = T.END_OF_FILE;
    String identifier = "";
    double number;
    String stringLiteral = "";
    private int prevErrorLoc = -1;
    boolean allowDoubleBracket = true;
    boolean hasNewlineBefore;

    private TomlLexer(String source, Json.Log log) {
        this.source = source;
        this.len = source.length();
        this.log = log;
    }

    static TomlLexer init(String source, Json.Log log) {
        TomlLexer lexer = new TomlLexer(source, log);
        lexer.step();
        lexer.next();
        return lexer;
    }

    String raw() {
        return source.substring(start, end);
    }

    String source() {
        return source;
    }

    void addError(int loc, String text) {
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
            addError(start, "Syntax Error");
        }
        return new Json.Failure(Json.SYNTAX_ERROR);
    }

    Json.Failure addDefaultError(String msg) {
        addError(start, msg);
        return new Json.Failure(Json.SYNTAX_ERROR);
    }

    private Json.Failure addSyntaxError(int loc, String msg) {
        addError(loc, msg);
        return new Json.Failure(Json.SYNTAX_ERROR);
    }

    void expect(T expected) {
        if (token != expected) {
            expectedString(expected.tagName());
        }
        next();
    }

    void expectAssignment() {
        if (token != T.EQUAL && token != T.COLON) {
            expectedString(T.EQUAL.tagName());
        }
        next();
    }

    void unexpected() {
        start = Math.min(start, end);
        addRangeError(start, end - start, "Unexpected " + (start == len ? "end of file" : raw()));
    }

    void expectedString(String text) {
        addRangeError(start, end - start, "Expected " + text + " but found " + (len != start ? raw() : "end of file"));
    }

    private void step() {
        if (current >= len) {
            end = len;
            codePoint = -1;
            return;
        }
        int cp = source.codePointAt(current);
        end = current;
        current += Character.charCount(cp);
        codePoint = cp;
    }

    void next() {
        hasNewlineBefore = end == 0;
        while (true) {
            start = end;
            token = T.END_OF_FILE;
            switch (codePoint) {
                case -1:
                    return;
                case '\r':
                case '\n':
                case 0x2028:
                case 0x2029:
                    step();
                    hasNewlineBefore = true;
                    continue;
                case '\t':
                case ' ':
                    step();
                    continue;
                case '[':
                    step();
                    token = T.OPEN_BRACKET;
                    if (codePoint == '[' && allowDoubleBracket) {
                        step();
                        token = T.OPEN_BRACKET_DOUBLE;
                        return;
                    }
                    if (codePoint == ']') {
                        step();
                        token = T.EMPTY_ARRAY;
                    }
                    return;
                case ']':
                    step();
                    token = T.CLOSE_BRACKET;
                    if (codePoint == ']' && allowDoubleBracket) {
                        step();
                        token = T.CLOSE_BRACKET_DOUBLE;
                    }
                    return;
                case '+':
                    step();
                    token = T.PLUS;
                    return;
                case '-':
                    step();
                    token = T.MINUS;
                    return;
                case '{':
                    step();
                    token = T.OPEN_BRACE;
                    return;
                case '}':
                    step();
                    token = T.CLOSE_BRACE;
                    return;
                case '=':
                    step();
                    token = T.EQUAL;
                    return;
                case ':':
                    step();
                    token = T.COLON;
                    return;
                case ',':
                    step();
                    token = T.COMMA;
                    return;
                case ';':
                    if (hasNewlineBefore) {
                        step();
                        skipCommentRest();
                        continue;
                    }
                    throw addDefaultError("Unexpected semicolon");
                case '#':
                    step();
                    skipCommentRest();
                    continue;
                case '\'':
                    literalString();
                    return;
                case '"':
                    basicString();
                    return;
                default:
                    if (codePoint == '.' || codePoint >= '0' && codePoint <= '9') {
                        parseNumericLiteralOrDot();
                        return;
                    }
                    if (codePoint == '@' || codePoint >= 'a' && codePoint <= 'z' ||
                        codePoint >= 'A' && codePoint <= 'Z' || codePoint == '$' || codePoint == '_') {
                        step();
                        while (isIdentifierPart(codePoint)) {
                            step();
                        }
                        identifier = raw();
                        token = "true".equals(identifier) ? T.TRUE : "false".equals(identifier) ? T.FALSE :
                                T.IDENTIFIER;
                        return;
                    }
                    // the empty token: logged as "Unexpected ", and the parse reads it as the end of the file
                    unexpected();
                    return;
            }
        }
    }

    // the comment loop steps before it looks, so the character after the # is never a line's end
    private void skipCommentRest() {
        while (true) {
            step();
            if (codePoint == '\r' || codePoint == '\n' || codePoint == 0x2028 || codePoint == 0x2029 ||
                codePoint == -1) {
                return;
            }
        }
    }

    private void literalString() {
        step();
        int contentStart = end;
        boolean multiline = false;
        if (codePoint == '\'') {
            step();
            if (codePoint == '\'') {
                step();
                multiline = true;
            } else {
                token = T.STRING_LITERAL;
                stringLiteral = "";
                return;
            }
        }
        if (multiline) {
            while (true) {
                if (codePoint == -1) {
                    throw addDefaultError("Unterminated string literal");
                }
                if (codePoint == '\'') {
                    int quote = end;
                    step();
                    if (codePoint != '\'') {
                        continue;
                    }
                    step();
                    if (codePoint != '\'') {
                        continue;
                    }
                    step();
                    token = T.STRING_LITERAL;
                    stringLiteral = source.substring(contentStart + 2, quote);
                    return;
                }
                step();
            }
        }
        while (true) {
            switch (codePoint) {
                case '\r':
                case '\n':
                case 0x2028:
                case 0x2029:
                    throw addDefaultError("Unterminated string literal (single-line)");
                case -1:
                    throw addDefaultError("Unterminated string literal");
                case '\'':
                    step();
                    token = T.STRING_LITERAL;
                    stringLiteral = source.substring(contentStart, end - 1);
                    return;
                default:
                    step();
                    break;
            }
        }
    }

    private void basicString() {
        step();
        boolean needsSlowPass = false;
        int contentStart = end;
        boolean multiline = false;
        if (codePoint == '"') {
            step();
            if (codePoint == '"') {
                step();
                multiline = true;
            } else {
                token = T.STRING_LITERAL;
                stringLiteral = "";
                return;
            }
        }
        String text;
        if (multiline) {
            while (true) {
                if (codePoint == -1) {
                    throw addDefaultError("Unterminated basic string");
                }
                if (codePoint == '\\') {
                    step();
                    needsSlowPass = true;
                    if (codePoint == '"') {
                        step();
                        continue;
                    }
                } else if (codePoint == '"') {
                    int quote = end;
                    step();
                    if (codePoint != '"') {
                        continue;
                    }
                    step();
                    if (codePoint != '"') {
                        continue;
                    }
                    step();
                    token = T.STRING_LITERAL;
                    text = source.substring(contentStart + 2, quote);
                    break;
                }
                step();
            }
        } else {
            while (true) {
                if (codePoint == '\r' || codePoint == '\n' || codePoint == 0x2028 || codePoint == 0x2029) {
                    throw addDefaultError("Unterminated basic string (single-line)");
                }
                if (codePoint == -1) {
                    throw addDefaultError("Unterminated basic string");
                }
                if (codePoint == '\\') {
                    step();
                    needsSlowPass = true;
                    if (codePoint == '"') {
                        step();
                        continue;
                    }
                } else if (codePoint == '"') {
                    step();
                    token = T.STRING_LITERAL;
                    text = source.substring(contentStart, end - 1);
                    break;
                }
                step();
            }
        }
        if (!needsSlowPass) {
            stringLiteral = text;
            return;
        }
        start = contentStart;
        StringBuilder buf = new StringBuilder(text.length());
        decodeEscapeSequences(contentStart, text, multiline, buf);
        stringLiteral = buf.toString();
        token = T.STRING_LITERAL;
    }

    // decodeEscapeSequences: \f is a tab and \t a form feed, an escape it does not know is its character, and a line
    // continuation drops the newline alone
    private void decodeEscapeSequences(int textStart, String text, boolean allowMultiline, StringBuilder buf) {
        int i = 0;
        while (i < text.length()) {
            int c = text.codePointAt(i);
            i += Character.charCount(c);
            if (c == '\r') {
                if (i < text.length() && text.charAt(i) == '\n') {
                    i++;
                }
                buf.append('\n');
                continue;
            }
            if (c != '\\') {
                buf.appendCodePoint(c);
                continue;
            }
            if (i >= text.length()) {
                return;
            }
            int c2 = text.codePointAt(i);
            int width2 = Character.charCount(c2);
            i += width2;
            switch (c2) {
                case 'b':
                    buf.append((char) 8);
                    continue;
                case 'f':
                    buf.append((char) 9);
                    continue;
                case 'n':
                    buf.append((char) 10);
                    continue;
                case 'v':
                    buf.append((char) 11);
                    continue;
                case 't':
                    buf.append((char) 12);
                    continue;
                case 'r':
                    buf.append((char) 13);
                    continue;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7': {
                    int value = c2 - '0';
                    if (i >= text.length()) {
                        if (value == 0) {
                            buf.append((char) 0);
                            return;
                        }
                        throw syntaxError();
                    }
                    int c3 = text.charAt(i);
                    if (c3 >= '0' && c3 <= '7') {
                        value = value * 8 + c3 - '0';
                        i++;
                        if (i >= text.length()) {
                            throw syntaxError();
                        }
                        int c4 = text.charAt(i);
                        if (c4 >= '0' && c4 <= '7') {
                            int temp = value * 8 + c4 - '0';
                            if (temp < 256) {
                                value = temp;
                                i++;
                            }
                        } else if (c4 == '8' || c4 == '9') {
                            i++;
                            addRangeError(textStart + i, 0, "Invalid legacy octal literal");
                        }
                    } else if (c3 == '8' || c3 == '9') {
                        i++;
                        addRangeError(textStart + i, 0, "Invalid legacy octal literal");
                    }
                    buf.appendCodePoint(value);
                    continue;
                }
                case '8':
                case '9':
                    buf.appendCodePoint(c2);
                    continue;
                case 'x': {
                    if (allowMultiline) {
                        throw syntaxError();
                    }
                    int value = 0;
                    for (int k = 0; k < 2; k++) {
                        int d = i < text.length() ? hexDigit(text.charAt(i)) : -1;
                        if (d < 0) {
                            throw syntaxError();
                        }
                        i++;
                        value = value * 16 | d;
                    }
                    buf.appendCodePoint(value);
                    continue;
                }
                case 'u': {
                    if (i >= text.length()) {
                        throw syntaxError();
                    }
                    long value = 0;
                    if (text.charAt(i) == '{') {
                        i++;
                        boolean isFirst = true;
                        boolean outOfRange = false;
                        while (i < text.length()) {
                            char c3 = text.charAt(i++);
                            if (c3 == '}') {
                                if (isFirst) {
                                    throw syntaxError();
                                }
                                break;
                            }
                            int d = hexDigit(c3);
                            if (d < 0) {
                                throw syntaxError();
                            }
                            value = value * 16 | d;
                            if (value > 1114111) {
                                outOfRange = true;
                            }
                            isFirst = false;
                        }
                        if (outOfRange) {
                            addRangeError(textStart + i, 0, "Unicode escape sequence is out of range");
                            return;
                        }
                    } else {
                        for (int k = 0; k < 4; k++) {
                            int d = i < text.length() ? hexDigit(text.charAt(i)) : -1;
                            if (d < 0) {
                                throw syntaxError();
                            }
                            i++;
                            value = value * 16 | d;
                        }
                    }
                    int cp = (int) value;
                    if (cp <= 0xFFFF) {
                        buf.append((char) cp);
                    } else {
                        buf.appendCodePoint(cp);
                    }
                    continue;
                }
                case '\r':
                    if (!allowMultiline) {
                        throw addDefaultError("Unexpected end of line");
                    }
                    if (i < text.length() && text.charAt(i) == '\n') {
                        i++;
                    }
                    continue;
                case '\n':
                case 0x2028:
                case 0x2029:
                    if (!allowMultiline) {
                        throw addDefaultError("Unexpected end of line");
                    }
                    continue;
                default:
                    buf.appendCodePoint(c2);
                    break;
            }
        }
    }

    private void parseNumericLiteralOrDot() {
        int first = codePoint;
        step();
        if (first == '.' && (codePoint < '0' || codePoint > '9')) {
            token = T.DOT;
            return;
        }
        int underscoreCount = 0;
        int lastUnderscoreEnd = 0;
        boolean hasDotOrExponent = first == '.';
        double base = 0;
        boolean isLegacyOctalLiteral = false;
        token = T.NUMERIC_LITERAL;
        if (first == '0') {
            if (codePoint == 'b' || codePoint == 'B') {
                base = 2;
            } else if (codePoint == 'o' || codePoint == 'O') {
                base = 8;
            } else if (codePoint == 'x' || codePoint == 'X') {
                base = 16;
            } else if (codePoint >= '0' && codePoint <= '7' || codePoint == '_') {
                base = 8;
                isLegacyOctalLiteral = true;
            }
        }
        if (base != 0) {
            boolean isFirst = true;
            boolean isInvalidLegacyOctalLiteral = false;
            number = 0;
            if (!isLegacyOctalLiteral) {
                step();
            }
            integerLiteral:
            while (true) {
                switch (codePoint) {
                    case '_':
                        if (lastUnderscoreEnd > 0 && end == lastUnderscoreEnd + 1) {
                            throw syntaxError();
                        }
                        if (isFirst || isLegacyOctalLiteral) {
                            throw syntaxError();
                        }
                        lastUnderscoreEnd = end;
                        underscoreCount++;
                        break;
                    case '0':
                    case '1':
                        number = number * base + (codePoint - '0');
                        break;
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                        if (base == 2) {
                            throw syntaxError();
                        }
                        number = number * base + (codePoint - '0');
                        break;
                    case '8':
                    case '9':
                        if (isLegacyOctalLiteral) {
                            isInvalidLegacyOctalLiteral = true;
                        } else if (base < 10) {
                            throw syntaxError();
                        }
                        number = number * base + (codePoint - '0');
                        break;
                    case 'A':
                    case 'B':
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                        if (base != 16) {
                            throw syntaxError();
                        }
                        number = number * base + (codePoint + 10 - 'A');
                        break;
                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                        if (base != 16) {
                            throw syntaxError();
                        }
                        number = number * base + (codePoint + 10 - 'a');
                        break;
                    default:
                        if (isFirst) {
                            throw syntaxError();
                        }
                        break integerLiteral;
                }
                step();
                isFirst = false;
            }
            boolean isBigIntegerLiteral = codePoint == 'n' && !hasDotOrExponent;
            if (isBigIntegerLiteral || isInvalidLegacyOctalLiteral) {
                String text = raw();
                if (isBigIntegerLiteral && isLegacyOctalLiteral) {
                    throw syntaxError();
                }
                if (!isBigIntegerLiteral) {
                    try {
                        number = Double.parseDouble(text);
                    } catch (NumberFormatException e) {
                        throw addSyntaxError(start, "Invalid number " + text);
                    }
                }
            }
            return;
        }
        boolean isInvalidLegacyOctalLiteral = first == '0' && (codePoint == '8' || codePoint == '9');
        while (true) {
            if (codePoint < '0' || codePoint > '9') {
                if (codePoint != '_') {
                    break;
                }
                if (lastUnderscoreEnd > 0 && end == lastUnderscoreEnd + 1) {
                    throw syntaxError();
                }
                if (isInvalidLegacyOctalLiteral) {
                    throw syntaxError();
                }
                lastUnderscoreEnd = end;
                underscoreCount++;
            }
            step();
        }
        if (first != '.' && codePoint == '.') {
            if (lastUnderscoreEnd > 0 && end == lastUnderscoreEnd + 1) {
                end--;
                throw syntaxError();
            }
            hasDotOrExponent = true;
            step();
            if (codePoint == '_') {
                throw syntaxError();
            }
            while (true) {
                if (codePoint < '0' || codePoint > '9') {
                    if (codePoint != '_') {
                        break;
                    }
                    if (lastUnderscoreEnd > 0 && end == lastUnderscoreEnd + 1) {
                        throw syntaxError();
                    }
                    lastUnderscoreEnd = end;
                    underscoreCount++;
                }
                step();
            }
        }
        if (codePoint == 'e' || codePoint == 'E') {
            if (lastUnderscoreEnd > 0 && end == lastUnderscoreEnd + 1) {
                end--;
                throw syntaxError();
            }
            hasDotOrExponent = true;
            step();
            if (codePoint == '+' || codePoint == '-') {
                step();
            }
            if (codePoint < '0' || codePoint > '9') {
                throw syntaxError();
            }
            while (true) {
                if (codePoint < '0' || codePoint > '9') {
                    if (codePoint != '_') {
                        break;
                    }
                    if (lastUnderscoreEnd > 0 && end == lastUnderscoreEnd + 1) {
                        throw syntaxError();
                    }
                    lastUnderscoreEnd = end;
                    underscoreCount++;
                }
                step();
            }
        }
        String text = raw();
        if (underscoreCount > 0) {
            text = text.replace("_", "");
        }
        if (!hasDotOrExponent && end - start < 10) {
            long value = 0;
            for (int k = 0; k < text.length(); k++) {
                value = value * 10 + (text.charAt(k) - '0');
            }
            number = value;
        } else {
            try {
                number = Double.parseDouble(text);
            } catch (NumberFormatException e) {
                throw addSyntaxError(start, "Invalid number");
            }
        }
    }

    // isIdentifierPart: letters, digits, $ _ - and :
    private static boolean isIdentifierPart(int c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '$' || c == '_' ||
               c == '-' || c == ':';
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
}
