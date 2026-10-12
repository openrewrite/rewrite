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

import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.ArrayList;
import java.util.List;

/**
 * bunfig.toml read into bun's expression shape: port of src/interchange/toml.zig {@code TOML} over its lexer
 * ({@link TomlLexer}) (bun 1.3.10), and from 1.4.0 of src/parsers/toml.rs, a strict TOML 1.1 parser ({@link Toml11}).
 */
final class Toml {

    /**
     * A bunfig.toml bun does not parse: {@code error} names the parser's error, the message is the first it logged.
     */
    static final class SyntaxError extends Json.SyntaxError {
        SyntaxError(String error, String message) {
            super(error, message);
        }
    }

    private final TomlLexer lexer;
    private final BunRelease release;

    private Toml(TomlLexer lexer, BunRelease release) {
        this.lexer = lexer;
        this.release = release;
    }

    static Json.Expr parse(String source) {
        return parse(source, BunRelease.PORTED);
    }

    /**
     * bunfig.toml as {@code release} parses it (with its logs redacted).
     */
    static Json.Expr parse(String source, BunRelease release) {
        if (source.isEmpty()) {
            return object();
        }
        Json.Log log = new Json.Log();
        if (release.parsesTomlStrictly()) {
            try {
                return Toml11.parse(source, log);
            } catch (Json.Failure f) {
                throw new SyntaxError(f.error, log.first(f.error));
            }
        }
        Json.Expr root;
        try {
            root = new Toml(TomlLexer.init(source, log), release).runParser();
        } catch (Json.Failure f) {
            throw new SyntaxError(f.error, log.first(f.error));
        }
        if (log.hasErrors()) {
            // the parse returned what it read past an error it only logged, and the install goes on with it
            throw new CannotKnow("what bun " + release + " makes of bunfig.toml it reads past its own error (" +
                                 log.first("") + ")");
        }
        return root;
    }

    private static Json.Expr object() {
        return new Json.Expr(Json.Kind.OBJECT, new ArrayList<Json.Property>());
    }

    private static Json.Expr array() {
        return new Json.Expr(Json.Kind.ARRAY, new ArrayList<Json.Expr>());
    }

    // a dotted key's segments, each the string expression of its key
    private static final class Rope {
        final Json.Expr head;
        final int headStart;
        Rope next;

        Rope(Json.Expr head, int headStart) {
            this.head = head;
            this.headStart = headStart;
        }
    }

    private static final class Clobber extends RuntimeException {
        Clobber() {
            super(null, null, false, false);
        }
    }

    private Json.Expr runParser() {
        Json.Expr root = object();
        Json.Expr head = root;
        while (true) {
            switch (lexer.token) {
                case END_OF_FILE:
                    return root;
                case OPEN_BRACKET: {
                    lexer.next();
                    Rope key = parseKey();
                    lexer.expect(TomlLexer.T.CLOSE_BRACKET);
                    if (!lexer.hasNewlineBefore) {
                        lexer.expectedString("line break");
                    }
                    try {
                        head = getOrPutObject(root, key);
                    } catch (Clobber c) {
                        throw lexer.addDefaultError("Table already defined");
                    }
                    break;
                }
                case OPEN_BRACKET_DOUBLE: {
                    lexer.next();
                    Rope key = parseKey();
                    lexer.expect(TomlLexer.T.CLOSE_BRACKET_DOUBLE);
                    if (!lexer.hasNewlineBefore) {
                        lexer.expectedString("line break");
                    }
                    Json.Expr array;
                    try {
                        array = getOrPutArray(root, key);
                    } catch (Clobber c) {
                        throw lexer.addDefaultError("Cannot overwrite table array");
                    }
                    Json.Expr newHead = object();
                    array.items().add(newHead);
                    head = newHead;
                    break;
                }
                default:
                    parseAssignment(head);
                    break;
            }
        }
    }

    private Json.Expr parseKeySegment() {
        switch (lexer.token) {
            case STRING_LITERAL: {
                Json.Expr str = Json.Expr.string(lexer.stringLiteral);
                lexer.next();
                return str;
            }
            case IDENTIFIER: {
                Json.Expr str = Json.Expr.string(lexer.identifier);
                lexer.next();
                return str;
            }
            case FALSE:
                lexer.next();
                return Json.Expr.string("false");
            case TRUE:
                lexer.next();
                return Json.Expr.string("true");
            case NUMERIC_LITERAL: {
                Json.Expr str = Json.Expr.string(lexer.raw());
                lexer.next();
                return str;
            }
            default:
                return null;
        }
    }

    private Rope parseKey() {
        int start = lexer.start;
        Json.Expr first = parseKeySegment();
        if (first == null) {
            lexer.expectedString("key");
            throw new Json.Failure(Json.SYNTAX_ERROR);
        }
        Rope head = new Rope(first, start);
        Rope rope = head;
        while (lexer.token == TomlLexer.T.DOT) {
            lexer.next();
            int segmentStart = lexer.start;
            Json.Expr segment = parseKeySegment();
            if (segment == null) {
                break;
            }
            rope.next = new Rope(segment, segmentStart);
            rope = rope.next;
        }
        return head;
    }

    private void parseAssignment(Json.Expr obj) {
        lexer.allowDoubleBracket = false;
        Rope rope = parseKey();
        int ropeEnd = lexer.start;
        boolean isArray = lexer.token == TomlLexer.T.EMPTY_ARRAY;
        if (isArray) {
            lexer.next();
        }
        lexer.expectAssignment();
        if (!isArray) {
            Json.Expr value = parseValue();
            try {
                setRope(obj, rope, value);
            } catch (Clobber c) {
                String keyName = Strings.trimRight(lexer.source().substring(rope.headStart, ropeEnd), " \t\n\r\u000B\f");
                lexer.addError(rope.headStart, "Cannot redefine key '" + keyName + "'");
                throw new Json.Failure(Json.SYNTAX_ERROR);
            }
        }
        lexer.allowDoubleBracket = true;
    }

    private Json.Expr parseValue() {
        lexer.allowDoubleBracket = true;
        switch (lexer.token) {
            case FALSE:
                lexer.next();
                return new Json.Expr(Json.Kind.BOOLEAN, false);
            case TRUE:
                lexer.next();
                return new Json.Expr(Json.Kind.BOOLEAN, true);
            case STRING_LITERAL: {
                Json.Expr str = Json.Expr.string(lexer.stringLiteral);
                lexer.next();
                return str;
            }
            case IDENTIFIER: {
                Json.Expr str = Json.Expr.string(lexer.identifier);
                lexer.next();
                return str;
            }
            case NUMERIC_LITERAL: {
                double value = lexer.number;
                lexer.next();
                return new Json.Expr(Json.Kind.NUMBER, value);
            }
            case MINUS: {
                lexer.next();
                double value = lexer.number;
                lexer.expect(TomlLexer.T.NUMERIC_LITERAL);
                return new Json.Expr(Json.Kind.NUMBER, -value);
            }
            case PLUS: {
                lexer.next();
                double value = lexer.number;
                lexer.expect(TomlLexer.T.NUMERIC_LITERAL);
                return new Json.Expr(Json.Kind.NUMBER, value);
            }
            case OPEN_BRACE: {
                lexer.next();
                Json.Expr expr = object();
                while (lexer.token != TomlLexer.T.CLOSE_BRACE) {
                    if (!expr.properties().isEmpty() && !parseMaybeTrailingComma(TomlLexer.T.CLOSE_BRACE)) {
                        break;
                    }
                    parseAssignment(expr);
                    lexer.allowDoubleBracket = false;
                }
                // before 1.2.16 the close is consumed as the assignments left the lexer
                if (release.lexesADoubleBracketAfterAValue()) {
                    lexer.allowDoubleBracket = true;
                }
                lexer.expect(TomlLexer.T.CLOSE_BRACE);
                return expr;
            }
            case EMPTY_ARRAY:
                lexer.next();
                lexer.allowDoubleBracket = true;
                return array();
            case OPEN_BRACKET: {
                lexer.next();
                Json.Expr array = array();
                lexer.allowDoubleBracket = false;
                while (lexer.token != TomlLexer.T.CLOSE_BRACKET) {
                    if (!array.items().isEmpty() && !parseMaybeTrailingComma(TomlLexer.T.CLOSE_BRACKET)) {
                        break;
                    }
                    array.items().add(parseValue());
                }
                // before 1.2.16 the close is consumed as the elements left the lexer, and [[ allowed after
                if (release.lexesADoubleBracketAfterAValue()) {
                    lexer.allowDoubleBracket = true;
                    lexer.expect(TomlLexer.T.CLOSE_BRACKET);
                } else {
                    lexer.expect(TomlLexer.T.CLOSE_BRACKET);
                    lexer.allowDoubleBracket = true;
                }
                return array;
            }
            default:
                lexer.unexpected();
                throw new Json.Failure(Json.SYNTAX_ERROR);
        }
    }

    private boolean parseMaybeTrailingComma(TomlLexer.T closer) {
        lexer.expect(TomlLexer.T.COMMA);
        return lexer.token != closer;
    }

    // E.Object.get: the first property of the name
    private static Json.Expr get(Json.Expr obj, Json.Expr key) {
        return obj.get(key.asString());
    }

    // E.Object.setRope: an array takes the value at its end, an object is descended, anything else is a clobber
    private static void setRope(Json.Expr obj, Rope rope, Json.Expr value) {
        Json.Expr existing = get(obj, rope.head);
        if (existing != null) {
            if (existing.isArray()) {
                List<Json.Expr> items = existing.items();
                if (rope.next == null) {
                    items.add(value);
                    return;
                }
                if (!items.isEmpty()) {
                    Json.Expr last = items.get(items.size() - 1);
                    if (!last.isObject()) {
                        throw new Clobber();
                    }
                    setRope(last, rope.next, value);
                    return;
                }
                items.add(value);
                return;
            }
            if (existing.isObject()) {
                if (rope.next != null) {
                    setRope(existing, rope.next, value);
                    return;
                }
                throw new Clobber();
            }
            throw new Clobber();
        }
        Json.Expr value_ = value;
        if (rope.next != null) {
            value_ = object();
            setRope(value_, rope.next, value);
        }
        obj.properties().add(new Json.Property(rope.head, value_));
    }

    // E.Object.getOrPutObject: a table reopened is the one there (merged), a table under an array of tables is under
    // its last element
    private static Json.Expr getOrPutObject(Json.Expr obj, Rope rope) {
        Json.Expr existing = get(obj, rope.head);
        if (existing != null) {
            if (existing.isArray()) {
                List<Json.Expr> items = existing.items();
                if (rope.next == null || items.isEmpty() || !items.get(items.size() - 1).isObject()) {
                    throw new Clobber();
                }
                return getOrPutObject(items.get(items.size() - 1), rope.next);
            }
            if (existing.isObject()) {
                return rope.next != null ? getOrPutObject(existing, rope.next) : existing;
            }
            throw new Clobber();
        }
        if (rope.next != null) {
            Json.Expr child = object();
            Json.Expr out = getOrPutObject(child, rope.next);
            obj.properties().add(new Json.Property(rope.head, child));
            return out;
        }
        Json.Expr out = object();
        obj.properties().add(new Json.Property(rope.head, out));
        return out;
    }

    // E.Object.getOrPutArray
    private static Json.Expr getOrPutArray(Json.Expr obj, Rope rope) {
        Json.Expr existing = get(obj, rope.head);
        if (existing != null) {
            if (existing.isArray()) {
                if (rope.next == null) {
                    return existing;
                }
                List<Json.Expr> items = existing.items();
                if (items.isEmpty() || !items.get(items.size() - 1).isObject()) {
                    throw new Clobber();
                }
                return getOrPutArray(items.get(items.size() - 1), rope.next);
            }
            if (existing.isObject()) {
                if (rope.next == null) {
                    throw new Clobber();
                }
                return getOrPutArray(existing, rope.next);
            }
            throw new Clobber();
        }
        if (rope.next != null) {
            Json.Expr child = object();
            Json.Expr out = getOrPutArray(child, rope.next);
            obj.properties().add(new Json.Property(rope.head, child));
            return out;
        }
        Json.Expr out = array();
        obj.properties().add(new Json.Property(rope.head, out));
        return out;
    }
}
