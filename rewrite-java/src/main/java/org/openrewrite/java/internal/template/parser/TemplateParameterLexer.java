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
package org.openrewrite.java.internal.template.parser;

/**
 * Hand-rolled lexer for the TemplateParameter grammar. Replaces the
 * generated ANTLR lexer to avoid the cost of ATN simulation on a tiny
 * grammar (7 punctuation tokens, 2 keywords, identifiers/numbers).
 */
final class TemplateParameterLexer {

    enum TokenKind {
        LPAREN, RPAREN, DOT, COLON, COMMA,
        LBRACK, RBRACK, WILDCARD, LSBRACK, RSBRACK, AND,
        EXTENDS, SUPER,
        FULLY_QUALIFIED_NAME, NUMBER, IDENTIFIER,
        EOF
    }

    private final String input;
    private int cursor;

    /** Start of the most recently produced token (inclusive). */
    private int tokenStart;
    /** End of the most recently produced token (exclusive). */
    private int tokenEnd;
    /** Kind of the most recently produced token. */
    private TokenKind kind = TokenKind.EOF;

    TemplateParameterLexer(String input) {
        this.input = input;
        advance();
    }

    String input() {
        return input;
    }

    TokenKind kind() {
        return kind;
    }

    int tokenStart() {
        return tokenStart;
    }

    int tokenEnd() {
        return tokenEnd;
    }

    String tokenText() {
        return input.substring(tokenStart, tokenEnd);
    }

    void advance() {
        skipWhitespace();
        tokenStart = cursor;
        if (cursor >= input.length()) {
            kind = TokenKind.EOF;
            tokenEnd = cursor;
            return;
        }

        char c = input.charAt(cursor);
        switch (c) {
            case '(':
                kind = TokenKind.LPAREN;
                cursor++;
                tokenEnd = cursor;
                return;
            case ')':
                kind = TokenKind.RPAREN;
                cursor++;
                tokenEnd = cursor;
                return;
            case '.':
                kind = TokenKind.DOT;
                cursor++;
                tokenEnd = cursor;
                return;
            case ':':
                kind = TokenKind.COLON;
                cursor++;
                tokenEnd = cursor;
                return;
            case ',':
                kind = TokenKind.COMMA;
                cursor++;
                tokenEnd = cursor;
                return;
            case '<':
                kind = TokenKind.LBRACK;
                cursor++;
                tokenEnd = cursor;
                return;
            case '>':
                kind = TokenKind.RBRACK;
                cursor++;
                tokenEnd = cursor;
                return;
            case '?':
                kind = TokenKind.WILDCARD;
                cursor++;
                tokenEnd = cursor;
                return;
            case '[':
                kind = TokenKind.LSBRACK;
                cursor++;
                tokenEnd = cursor;
                return;
            case ']':
                kind = TokenKind.RSBRACK;
                cursor++;
                tokenEnd = cursor;
                return;
            case '&':
                kind = TokenKind.AND;
                cursor++;
                tokenEnd = cursor;
                return;
            default:
                break;
        }

        if (c >= '0' && c <= '9') {
            int n = cursor + 1;
            while (n < input.length() && input.charAt(n) >= '0' && input.charAt(n) <= '9') {
                n++;
            }
            cursor = n;
            tokenEnd = cursor;
            kind = TokenKind.NUMBER;
            return;
        }

        if (isJavaIdentifierStart(c, cursor)) {
            int identEnd = consumeJavaIdentifier(cursor);
            cursor = identEnd;
            // Keyword and primitive-shortcut detection by direct char comparison
            // — avoid allocating a String for the identifier just to test it.
            if (tokenMatches(tokenStart, identEnd, "extends")) {
                tokenEnd = cursor;
                kind = TokenKind.EXTENDS;
                return;
            }
            if (tokenMatches(tokenStart, identEnd, "super")) {
                tokenEnd = cursor;
                kind = TokenKind.SUPER;
                return;
            }
            // Greedily consume DOT Identifier sequences for FullyQualifiedName.
            boolean consumedDot = false;
            while (cursor < input.length() && input.charAt(cursor) == '.' &&
                    cursor + 1 < input.length() && isJavaIdentifierStart(input.charAt(cursor + 1), cursor + 1)) {
                cursor++; // consume DOT
                cursor = consumeJavaIdentifier(cursor);
                consumedDot = true;
            }
            tokenEnd = cursor;
            if (consumedDot || isPrimitiveOrJavaLangShortcut(tokenStart, identEnd)) {
                kind = TokenKind.FULLY_QUALIFIED_NAME;
            } else {
                kind = TokenKind.IDENTIFIER;
            }
            return;
        }

        throw new IllegalArgumentException(
                "Unexpected character '" + c + "' at position " + cursor + " in input '" + input + "'");
    }

    private void skipWhitespace() {
        while (cursor < input.length()) {
            char c = input.charAt(cursor);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                cursor++;
            } else {
                break;
            }
        }
    }

    /**
     * Consumes a Java identifier starting at {@code start} and returns the
     * exclusive end index. Handles surrogate pairs for U+10000 and above.
     */
    private int consumeJavaIdentifier(int start) {
        int n = start;
        // Identifier start was already validated by the caller.
        n = advanceCodepoint(n);
        while (n < input.length()) {
            char c = input.charAt(n);
            if (c >= '\uD800' && c <= '\uDBFF' && n + 1 < input.length()) {
                int cp = Character.toCodePoint(c, input.charAt(n + 1));
                if (Character.isJavaIdentifierPart(cp)) {
                    n += 2;
                    continue;
                }
                break;
            }
            if (Character.isJavaIdentifierPart(c)) {
                n++;
            } else {
                break;
            }
        }
        return n;
    }

    private int advanceCodepoint(int n) {
        char c = input.charAt(n);
        if (c >= '\uD800' && c <= '\uDBFF' && n + 1 < input.length()) {
            return n + 2;
        }
        return n + 1;
    }

    private boolean isJavaIdentifierStart(char c, int at) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '$' || c == '_') {
            return true;
        }
        if (c < 0x0080) {
            return false;
        }
        if (c >= '\uD800' && c <= '\uDBFF' && at + 1 < input.length()) {
            int cp = Character.toCodePoint(c, input.charAt(at + 1));
            return Character.isJavaIdentifierStart(cp);
        }
        return Character.isJavaIdentifierStart(c);
    }

    private boolean tokenMatches(int start, int end, String s) {
        return end - start == s.length() && input.regionMatches(start, s, 0, s.length());
    }

    private boolean isPrimitiveOrJavaLangShortcut(int start, int end) {
        switch (end - start) {
            case 3:
                return tokenMatches(start, end, "int");
            case 4:
                return tokenMatches(start, end, "byte") ||
                        tokenMatches(start, end, "char") ||
                        tokenMatches(start, end, "long");
            case 5:
                return tokenMatches(start, end, "float") ||
                        tokenMatches(start, end, "short");
            case 6:
                return tokenMatches(start, end, "double") ||
                        tokenMatches(start, end, "Object") ||
                        tokenMatches(start, end, "String");
            case 7:
                return tokenMatches(start, end, "boolean");
            default:
                return false;
        }
    }
}
