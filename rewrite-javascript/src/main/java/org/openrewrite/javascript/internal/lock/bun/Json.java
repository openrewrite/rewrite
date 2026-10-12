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
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * JSON as bun reads package.json and bun.lock, comments and trailing commas allowed, every property kept in source
 * order, a repeated key too, {@link Expr#get} answering the first: port of src/interchange/json.zig
 * {@code parsePackageJSONUTF8} over the JavaScript lexer ({@link JsonLexer}) (bun 1.3.10), and from 1.4.0 of
 * src/parsers/json.rs {@code ParsedJson::parse_package_json} over a structural index ({@link JsonIndex},
 * {@link JsonStage2}).
 */
final class Json {

    static final String PARSER_ERROR = "ParserError";
    static final String SYNTAX_ERROR = "SyntaxError";

    private final JsonLexer lexer;

    private Json(JsonLexer lexer) {
        this.lexer = lexer;
    }

    /**
     * A document bun does not parse: {@link #error} names the parser's error ({@code ParserError} or
     * {@code SyntaxError}), the message is the first error it logged.
     */
    static class SyntaxError extends IllegalArgumentException {
        final String error;

        SyntaxError(String error, String message) {
            super(message);
            this.error = error;
        }
    }

    /**
     * An error a parser returns once it has logged it, which ends the parse.
     */
    static final class Failure extends RuntimeException {
        final String error;

        Failure(String error) {
            super(error, null, false, false);
            this.error = error;
        }
    }

    /**
     * The errors a parse logs, in order, each with where it is.
     */
    static final class Log {
        private final List<String> texts = new ArrayList<>();
        private final List<int[]> locations = new ArrayList<>();

        void add(String text, int offset, int length) {
            texts.add(text);
            locations.add(new int[]{offset, length});
        }

        boolean hasErrors() {
            return !texts.isEmpty();
        }

        int size() {
            return texts.size();
        }

        int offset(int i) {
            return locations.get(i)[0];
        }

        int length(int i) {
            return locations.get(i)[1];
        }

        void truncate(int size) {
            while (texts.size() > size) {
                texts.remove(texts.size() - 1);
                locations.remove(locations.size() - 1);
            }
        }

        String first(String otherwise) {
            return texts.isEmpty() ? otherwise : texts.get(0);
        }
    }

    static Expr parse(String source) {
        return parse(source, BunRelease.PORTED);
    }

    /**
     * The document as {@code release} reads it.
     */
    static Expr parse(String source, BunRelease release) {
        if (source.isEmpty()) {
            return new Expr(Kind.OBJECT, new ArrayList<Property>());
        }
        Log log = new Log();
        if (release.indexesJsonBeforeParsingIt()) {
            try {
                return parseImpl(source.getBytes(UTF_8), log);
            } catch (Failure f) {
                throw new SyntaxError(f.error, log.first(f.error));
            }
        }
        Expr root;
        try {
            root = new Json(JsonLexer.init(source, log)).parseExpr();
        } catch (Failure f) {
            throw new SyntaxError(f.error, log.first(f.error));
        }
        if (log.hasErrors()) {
            // the parse returned what it read past an error it only logged, and an install goes on with it
            throw new CannotKnow("what bun " + release + " makes of JSON it reads past its own error (" +
                                 log.first("") + ")");
        }
        return root;
    }

    // json.rs parse_impl with PACKAGE_JSON_OPTS: what stage 1 found wrong is the error unless stage 2 logged an earlier
    // one, and any error logged fails the parse
    private static Expr parseImpl(byte[] contents, Log log) {
        JsonIndex sidx = new JsonIndex(contents);
        Expr result = null;
        Failure failure = null;
        try {
            result = new JsonStage2(contents, log, sidx).parseValue();
        } catch (Failure f) {
            failure = f;
        }
        if (sidx.indexError != null) {
            int pos = sidx.indexErrorPos;
            boolean earlierStage2Err = false;
            for (int i = 0; i < log.size(); i++) {
                if (log.offset(i) + Math.max(log.length(i), 1) <= pos) {
                    earlierStage2Err = true;
                    break;
                }
            }
            if (!earlierStage2Err) {
                log.truncate(0);
                if (sidx.indexError == JsonIndex.Error.UNTERMINATED_BLOCK_COMMENT) {
                    log.add("Expected \"*/\" to terminate multi-line comment", contents.length, 0);
                } else {
                    log.add("Unsupported syntax: Operators are not allowed in JSON", pos + 1, 0);
                }
            }
            throw new Failure(SYNTAX_ERROR);
        }
        if (failure != null) {
            throw failure;
        }
        if (log.hasErrors()) {
            throw new Failure(SYNTAX_ERROR);
        }
        return result;
    }

    // json.zig JSONLikeParser.parseExpr (force_utf8)
    private Expr parseExpr() {
        switch (lexer.token) {
            case FALSE:
                lexer.next();
                return new Expr(Kind.BOOLEAN, false);
            case TRUE:
                lexer.next();
                return new Expr(Kind.BOOLEAN, true);
            case NULL:
                lexer.next();
                return new Expr(Kind.NULL, null);
            case STRING_LITERAL: {
                String str = lexer.toEString();
                lexer.next();
                return Expr.string(str);
            }
            case NUMERIC_LITERAL: {
                double value = lexer.number;
                lexer.next();
                return new Expr(Kind.NUMBER, value);
            }
            case MINUS: {
                lexer.next();
                double value = lexer.number;
                lexer.expect(JsonLexer.T.NUMERIC_LITERAL);
                return new Expr(Kind.NUMBER, -value);
            }
            case OPEN_BRACKET: {
                lexer.next();
                List<Expr> exprs = new ArrayList<>();
                while (lexer.token != JsonLexer.T.CLOSE_BRACKET) {
                    if (!exprs.isEmpty() && !parseMaybeTrailingComma(JsonLexer.T.CLOSE_BRACKET)) {
                        break;
                    }
                    exprs.add(parseExpr());
                }
                lexer.expect(JsonLexer.T.CLOSE_BRACKET);
                return new Expr(Kind.ARRAY, exprs);
            }
            case OPEN_BRACE: {
                lexer.next();
                List<Property> properties = new ArrayList<>();
                while (lexer.token != JsonLexer.T.CLOSE_BRACE) {
                    if (!properties.isEmpty() && !parseMaybeTrailingComma(JsonLexer.T.CLOSE_BRACE)) {
                        break;
                    }
                    Expr key = Expr.string(lexer.toEString());
                    lexer.expect(JsonLexer.T.STRING_LITERAL);
                    lexer.expect(JsonLexer.T.COLON);
                    properties.add(new Property(key, parseExpr()));
                }
                lexer.expect(JsonLexer.T.CLOSE_BRACE);
                return new Expr(Kind.OBJECT, properties);
            }
            default:
                lexer.unexpected();
                throw new Failure(PARSER_ERROR);
        }
    }

    private boolean parseMaybeTrailingComma(JsonLexer.T closer) {
        lexer.expect(JsonLexer.T.COMMA);
        return lexer.token != closer;
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
}
