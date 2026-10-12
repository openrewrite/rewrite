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
 * Port of bun's src/js_lexer.zig {@code NewLexer} in JSON mode (bun 1.3.10), comments allowed (package.json) or only
 * logged (strict JSON): the JavaScript lexer, one token at a time, as src/interchange/json.zig reads JSON through it.
 * Its {@code expect} only logs; what it returns an error for stops the parse. Over UTF-16 code points, which keep every
 * token's text; Unicode identifiers are told by Java's tables.
 */
final class JsonLexer {

    enum T {
        END_OF_FILE, SYNTAX_ERROR, OPEN_BRACE, CLOSE_BRACE, OPEN_BRACKET, CLOSE_BRACKET, OPEN_PAREN, CLOSE_PAREN,
        COMMA, COLON, MINUS, QUESTION, QUESTION_DOT, QUESTION_QUESTION, QUESTION_QUESTION_EQUALS, ASTERISK,
        ASTERISK_EQUALS, ASTERISK_ASTERISK, ASTERISK_ASTERISK_EQUALS, SLASH, SLASH_EQUALS, DOT, DOT_DOT_DOT,
        STRING_LITERAL, NO_SUBSTITUTION_TEMPLATE_LITERAL, TEMPLATE_HEAD, NUMERIC_LITERAL, BIG_INTEGER_LITERAL,
        IDENTIFIER, ESCAPED_KEYWORD, TRUE, FALSE, NULL, KEYWORD;

        /**
         * {@code tokenToString} of the tokens the JSON parser expects; js_lexer_tables.zig's {@code TColon} is
         * {@code " ="}.
         */
        String text() {
            switch (this) {
                case COMMA:
                    return "\",\"";
                case COLON:
                    return "\" =\"";
                case CLOSE_BRACE:
                    return "\"}\"";
                case CLOSE_BRACKET:
                    return "\"]\"";
                case STRING_LITERAL:
                    return "string";
                case NUMERIC_LITERAL:
                    return "number";
                default:
                    throw new IllegalStateException(name());
            }
        }
    }

    private static final String[] KEYWORDS = {"break", "case", "catch", "class", "const", "continue", "debugger",
            "default", "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for", "function", "if",
            "import", "in", "instanceof", "new", "null", "return", "super", "switch", "this", "throw", "true", "try",
            "typeof", "var", "void", "while", "with"};

    private final String source;
    private final int len;
    private final Json.Log log;
    private final boolean allowComments;

    private int current;
    int start;
    int end;
    private int codePoint = -1;
    T token = T.END_OF_FILE;
    double number;
    private int prevErrorLoc = -1;

    // the last string literal: its raw content, whether it holds escapes, and where its token starts
    private int stringRawStart;
    private int stringRawEnd;
    private boolean stringNeedsDecode;
    private int stringLiteralStart;

    private JsonLexer(String source, Json.Log log, boolean allowComments) {
        this.source = source;
        this.len = source.length();
        this.log = log;
        this.allowComments = allowComments;
    }

    /**
     * {@code init}: the lexer at its first token; without {@code allowComments}, a comment is logged and lexed as
     * the end of the file.
     */
    static JsonLexer init(String source, Json.Log log, boolean allowComments) {
        JsonLexer lexer = new JsonLexer(source, log, allowComments);
        lexer.step();
        lexer.next();
        return lexer;
    }

    String raw() {
        return source.substring(start, end);
    }

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

    Json.Failure syntaxError() {
        if (!log.hasErrors()) {
            addError(start, "Syntax Error");
        }
        return new Json.Failure(Json.SYNTAX_ERROR);
    }

    private Json.Failure addDefaultError(String msg) {
        addError(start, msg);
        return new Json.Failure(Json.SYNTAX_ERROR);
    }

    private Json.Failure addSyntaxError(int loc, String msg) {
        addError(loc, msg);
        return new Json.Failure(Json.SYNTAX_ERROR);
    }

    private Json.Failure addUnsupportedSyntaxError(String msg) {
        addError(end, "Unsupported syntax: " + msg);
        return new Json.Failure(Json.SYNTAX_ERROR);
    }

    /**
     * {@code expect}: a token other than {@code token} is only logged ({@code expected}); the lexer moves on either
     * way.
     */
    void expect(T expected) {
        if (token != expected) {
            expectedString(expected.text());
        }
        next();
    }

    private void expectedString(String text) {
        if (len != start) {
            addRangeError(start, end - start, "Expected " + text + " but found \"" + raw() + "\"");
        } else {
            addRangeError(start, end - start, "Expected " + text + " but found end of file");
        }
    }

    void unexpected() {
        start = Math.min(start, end);
        String found = start == len ? "end of file" : raw();
        addRangeError(start, end - start, "Unexpected " + found);
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
        while (true) {
            start = end;
            token = T.END_OF_FILE;
            switch (codePoint) {
                case -1:
                    return;
                case '#':
                    throw addUnsupportedSyntaxError("Private identifiers are not allowed in JSON");
                case '\r':
                case '\n':
                case 0x2028:
                case 0x2029:
                case '\t':
                case ' ':
                    step();
                    continue;
                case '(':
                    step();
                    token = T.OPEN_PAREN;
                    return;
                case ')':
                    step();
                    token = T.CLOSE_PAREN;
                    return;
                case '[':
                    step();
                    token = T.OPEN_BRACKET;
                    return;
                case ']':
                    step();
                    token = T.CLOSE_BRACKET;
                    return;
                case '{':
                    step();
                    token = T.OPEN_BRACE;
                    return;
                case '}':
                    step();
                    token = T.CLOSE_BRACE;
                    return;
                case ',':
                    step();
                    token = T.COMMA;
                    return;
                case ':':
                    step();
                    token = T.COLON;
                    return;
                case ';':
                    throw addUnsupportedSyntaxError("Semicolons are not allowed in JSON");
                case '@':
                    throw addUnsupportedSyntaxError("Decorators are not allowed in JSON");
                case '~':
                    throw addUnsupportedSyntaxError("~ is not allowed in JSON");
                case '?':
                    step();
                    if (codePoint == '?') {
                        step();
                        if (codePoint == '=') {
                            step();
                            token = T.QUESTION_QUESTION_EQUALS;
                        } else {
                            token = T.QUESTION_QUESTION;
                        }
                    } else if (codePoint == '.') {
                        token = T.QUESTION;
                        if (current < len) {
                            char c = source.charAt(current);
                            if (c < '0' || c > '9') {
                                step();
                                token = T.QUESTION_DOT;
                            }
                        }
                    } else {
                        token = T.QUESTION;
                    }
                    return;
                case '%':
                case '&':
                case '|':
                case '^':
                case '+':
                case '=':
                case '<':
                case '>':
                case '!':
                    throw addUnsupportedSyntaxError("Operators are not allowed in JSON");
                case '-':
                    step();
                    if (codePoint == '=' || codePoint == '-') {
                        throw addUnsupportedSyntaxError("Operators are not allowed in JSON");
                    }
                    token = T.MINUS;
                    return;
                case '*':
                    step();
                    if (codePoint == '=') {
                        step();
                        token = T.ASTERISK_EQUALS;
                    } else if (codePoint == '*') {
                        step();
                        if (codePoint == '=') {
                            step();
                            token = T.ASTERISK_ASTERISK_EQUALS;
                        } else {
                            token = T.ASTERISK_ASTERISK;
                        }
                    } else {
                        token = T.ASTERISK;
                    }
                    return;
                case '/':
                    step();
                    if (codePoint == '=') {
                        step();
                        token = T.SLASH_EQUALS;
                        return;
                    }
                    if (codePoint == '/') {
                        scanSingleLineComment();
                        if (!allowComments) {
                            addRangeError(start, end - start, "JSON does not support comments");
                            return;
                        }
                        continue;
                    }
                    if (codePoint == '*') {
                        step();
                        multiLineComment:
                        while (true) {
                            switch (codePoint) {
                                case '*':
                                    step();
                                    if (codePoint == '/') {
                                        step();
                                        break multiLineComment;
                                    }
                                    break;
                                case -1:
                                    start = end;
                                    throw addSyntaxError(start, "Expected \"*/\" to terminate multi-line comment");
                                default:
                                    step();
                                    break;
                            }
                        }
                        if (!allowComments) {
                            addRangeError(start, end - start, "JSON does not support comments");
                            return;
                        }
                        continue;
                    }
                    token = T.SLASH;
                    return;
                case '\'':
                case '"':
                case '`':
                    parseStringLiteral(codePoint);
                    return;
                case '\\':
                    scanIdentifierWithEscapes();
                    return;
                case '.':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    parseNumericLiteralOrDot();
                    return;
                default:
                    if (codePoint == '_' || codePoint == '$' || codePoint >= 'a' && codePoint <= 'z' ||
                        codePoint >= 'A' && codePoint <= 'Z') {
                        int advance = current;
                        while (advance < len && isLatin1IdentifierContinue(source.charAt(advance))) {
                            advance++;
                        }
                        end = advance;
                        current = end;
                        step();
                        if (codePoint >= 0x80) {
                            while (isIdentifierContinue(codePoint)) {
                                step();
                            }
                        }
                        if (codePoint != '\\') {
                            token = keyword(start, end);
                        } else {
                            scanIdentifierWithEscapes();
                        }
                        return;
                    }
                    if (isWhitespace(codePoint)) {
                        step();
                        continue;
                    }
                    if (isIdentifierStart(codePoint)) {
                        step();
                        while (isIdentifierContinue(codePoint)) {
                            step();
                        }
                        if (codePoint == '\\') {
                            scanIdentifierWithEscapes();
                        } else {
                            token = T.IDENTIFIER;
                        }
                        return;
                    }
                    end = current;
                    token = T.SYNTAX_ERROR;
                    return;
            }
        }
    }

    private T keyword(int from, int to) {
        String word = source.substring(from, to);
        if ("true".equals(word)) {
            return T.TRUE;
        }
        if ("false".equals(word)) {
            return T.FALSE;
        }
        if ("null".equals(word)) {
            return T.NULL;
        }
        for (String keyword : KEYWORDS) {
            if (keyword.equals(word)) {
                return T.KEYWORD;
            }
        }
        return T.IDENTIFIER;
    }

    private void scanSingleLineComment() {
        while (true) {
            step();
            switch (codePoint) {
                case '\r':
                case '\n':
                case 0x2028:
                case 0x2029:
                case -1:
                    return;
                default:
                    break;
            }
        }
    }

    private void parseStringLiteral(int quote) {
        token = quote == '`' ? T.NO_SUBSTITUTION_TEMPLATE_LITERAL : T.STRING_LITERAL;
        step();
        int suffixLen = 1;
        boolean needsDecode = false;
        stringLiteral:
        while (true) {
            switch (codePoint) {
                case '\\':
                    needsDecode = true;
                    step();
                    if (codePoint == '`' || codePoint == '\'' || codePoint == '"' || codePoint == '\\') {
                        step();
                        continue;
                    }
                    break;
                case -1:
                    throw addDefaultError("Unterminated string literal");
                case '\r':
                    if (quote != '`') {
                        throw addDefaultError("Unterminated string literal");
                    }
                    needsDecode = true;
                    break;
                case '\n':
                    if (quote != '`') {
                        throw addDefaultError("Unterminated string literal");
                    }
                    break;
                case '$':
                    if (quote == '`') {
                        step();
                        if (codePoint == '{') {
                            suffixLen = 2;
                            step();
                            token = T.TEMPLATE_HEAD;
                            break stringLiteral;
                        }
                        continue;
                    }
                    break;
                default:
                    if (codePoint == quote) {
                        step();
                        break stringLiteral;
                    }
                    if (codePoint >= 0x80) {
                        needsDecode = true;
                    } else if (codePoint < 0x20) {
                        throw syntaxError();
                    }
                    break;
            }
            step();
        }
        int base = start + 1;
        int endPos = end >= suffixLen ? end - suffixLen : end;
        stringRawStart = base;
        stringRawEnd = Math.min(len, Math.max(base, endPos));
        stringNeedsDecode = needsDecode;
        stringLiteralStart = start;
    }

    /**
     * {@code toEString}: the last string literal's value, its escapes decoded.
     */
    String toEString() {
        if (!stringNeedsDecode) {
            return source.substring(stringRawStart, stringRawEnd);
        }
        StringBuilder buf = new StringBuilder(stringRawEnd - stringRawStart);
        decodeEscapeSequences(stringRawStart, stringRawEnd, buf);
        return buf.toString();
    }

    // decodeEscapeSequences in JSON mode: a legacy octal, \\u{...} or line continuation escape is an error, and so is any
    // escape but JSON's and \v \xHH \8 \9
    private void decodeEscapeSequences(int from, int to, StringBuilder buf) {
        int i = from;
        while (i < to) {
            int c = source.codePointAt(i);
            i += Character.charCount(c);
            if (c == '\r') {
                if (i < to && source.charAt(i) == '\n') {
                    i++;
                }
                buf.append('\n');
                continue;
            }
            if (c != '\\') {
                buf.appendCodePoint(c);
                continue;
            }
            if (i >= to) {
                return;
            }
            int c2 = source.codePointAt(i);
            i += Character.charCount(c2);
            switch (c2) {
                case 'b':
                    buf.append('\b');
                    break;
                case 'f':
                    buf.append('\f');
                    break;
                case 'n':
                    buf.append('\n');
                    break;
                case 'v':
                    buf.append('\u000B');
                    break;
                case 't':
                    buf.append('\t');
                    break;
                case 'r':
                    buf.append('\r');
                    break;
                case '8':
                case '9':
                    buf.appendCodePoint(c2);
                    break;
                case 'x': {
                    int value = 0;
                    for (int k = 0; k < 2; k++) {
                        int d = i < to ? hexDigit(source.charAt(i)) : -1;
                        if (d < 0) {
                            throw syntaxError();
                        }
                        i++;
                        value = value * 16 | d;
                    }
                    buf.appendCodePoint(value);
                    break;
                }
                case 'u': {
                    if (i < to && source.charAt(i) == '{') {
                        throw syntaxError();
                    }
                    int value = 0;
                    for (int k = 0; k < 4; k++) {
                        int d = i < to ? hexDigit(source.charAt(i)) : -1;
                        if (d < 0) {
                            throw syntaxError();
                        }
                        i++;
                        value = value * 16 | d;
                    }
                    buf.append((char) value);
                    break;
                }
                case '"':
                case '\\':
                case '/':
                    buf.appendCodePoint(c2);
                    break;
                default:
                    // the legacy octal digits, \r, \n, U+2028, U+2029 and every other escape
                    throw syntaxError();
            }
        }
    }

    // a unicode-escaped identifier, from the backslash or partway into one
    private void scanIdentifierWithEscapes() {
        while (true) {
            if (codePoint == '\\') {
                step();
                if (codePoint != 'u') {
                    throw addSyntaxError(start, invalidEscapeSequence(codePoint));
                }
                step();
                if (codePoint == '{') {
                    step();
                    while (codePoint != '}') {
                        if (hexDigit(codePoint) < 0) {
                            throw syntaxError();
                        }
                        step();
                    }
                    step();
                } else {
                    for (int k = 0; k < 4; k++) {
                        if (hexDigit(codePoint) < 0) {
                            throw syntaxError();
                        }
                        step();
                    }
                }
                continue;
            }
            if (!isIdentifierContinue(codePoint)) {
                break;
            }
            step();
        }
        StringBuilder buf = new StringBuilder();
        decodeEscapeSequences(start, end, buf);
        String contents = buf.toString();
        if (!isIdentifier(contents)) {
            addRangeError(start, end - start, "Invalid identifier: \"" + contents + "\"");
        }
        token = isKeyword(contents) ? T.ESCAPED_KEYWORD : T.IDENTIFIER;
    }

    private static String invalidEscapeSequence(int codePoint) {
        switch (codePoint) {
            case '"':
                return "Unexpected escaped double quote '\"'";
            case '\'':
                return "Unexpected escaped single quote \"'\"";
            case '`':
                return "Unexpected escaped backtick '`'";
            case '\\':
                return "Unexpected escaped backslash '\\'";
            default:
                return "Unexpected escape sequence";
        }
    }

    private static boolean isKeyword(String word) {
        for (String keyword : KEYWORDS) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }

    private void parseNumericLiteralOrDot() {
        int first = codePoint;
        step();
        if (first == '.' && (codePoint < '0' || codePoint > '9')) {
            if (codePoint == '.' && current < len && source.charAt(current) == '.') {
                step();
                step();
                token = T.DOT_DOT_DOT;
                return;
            }
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
        } else {
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
            if (codePoint == 'n' && !hasDotOrExponent) {
                if (text.length() > 1 && first == '0') {
                    throw syntaxError();
                }
            } else if (!hasDotOrExponent && end - start < 10) {
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
        if (lastUnderscoreEnd > 0 && end == lastUnderscoreEnd + 1) {
            end--;
            throw syntaxError();
        }
        if (codePoint == 'n' && !hasDotOrExponent) {
            token = T.BIG_INTEGER_LITERAL;
            step();
        }
        if (isIdentifierStart(codePoint)) {
            throw syntaxError();
        }
    }

    private static boolean isLatin1IdentifierContinue(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '$' || c == '_';
    }

    /**
     * {@code isIdentifierStart}: ASCII letters, {@code _} and {@code $}, else Unicode's ID_Start.
     */
    static boolean isIdentifierStart(int c) {
        if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '$') {
            return true;
        }
        return c > 0x7F && c < 0x10FFFF && Character.isUnicodeIdentifierStart(c);
    }

    /**
     * {@code isIdentifierPart}: ASCII letters, digits, {@code _} and {@code $}, else Unicode's ID_Continue.
     */
    static boolean isIdentifierContinue(int c) {
        if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '$') {
            return true;
        }
        if (c <= 0x7F || c >= 0x10FFFF) {
            return false;
        }
        return c == 0x200C || c == 0x200D ||
               Character.isUnicodeIdentifierPart(c) && !Character.isIdentifierIgnorable(c);
    }

    private static boolean isIdentifier(String text) {
        if (text.isEmpty() || !isIdentifierStart(text.codePointAt(0))) {
            return false;
        }
        for (int i = Character.charCount(text.codePointAt(0)); i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (!isIdentifierContinue(cp)) {
                return false;
            }
            i += Character.charCount(cp);
        }
        return true;
    }

    /**
     * {@code isWhitespace}: the unusual whitespace the lexer skips.
     */
    private static boolean isWhitespace(int c) {
        switch (c) {
            case 0x0B:
            case 0x09:
            case 0x0C:
            case 0x20:
            case 0xA0:
            case 0x1680:
            case 0x202F:
            case 0x205F:
            case 0x3000:
            case 0xFEFF:
                return true;
            default:
                return c >= 0x2000 && c <= 0x200A;
        }
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
