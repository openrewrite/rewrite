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
    private final Log log;
    private final boolean allowTrailingCommas;

    private Json(JsonLexer lexer, Log log, boolean allowTrailingCommas) {
        this.lexer = lexer;
        this.log = log;
        this.allowTrailingCommas = allowTrailingCommas;
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
                return parseImpl(source.getBytes(UTF_8), log, true, true, false);
            } catch (Failure f) {
                throw new SyntaxError(f.error, log.first(f.error));
            }
        }
        Expr root;
        try {
            root = new Json(JsonLexer.init(source, log, true), log, true).parseExpr();
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

    /**
     * Strict JSON read to its end, as an .npmrc's quoted value is ({@code parseUTF8Impl} with {@code check_len},
     * 1.3.10; {@code parse_utf8_impl::<true>} from 1.4.0): neither comments nor trailing commas, and before 1.4.0
     * what the parse returns though it logged an error, its log being dropped.
     */
    static Expr parseStrict(String source, BunRelease release) {
        if (source.isEmpty()) {
            return new Expr(Kind.OBJECT, new ArrayList<Property>());
        }
        Log log = new Log();
        try {
            if (release.indexesJsonBeforeParsingIt()) {
                return parseImpl(source.getBytes(UTF_8), log, false, false, true);
            }
            JsonLexer lexer = JsonLexer.init(source, log, false);
            Expr root = new Json(lexer, log, false).parseExpr();
            if (lexer.end >= source.length()) {
                return root;
            }
            lexer.unexpected();
            throw new Failure(PARSER_ERROR);
        } catch (Failure f) {
            throw new SyntaxError(f.error, log.first(f.error));
        }
    }

    // json.rs parse_impl: what stage 1 found wrong is the error unless stage 2 logged an earlier one, a comment where
    // none is allowed is, and any error logged fails the parse
    private static Expr parseImpl(byte[] contents, Log log, boolean allowComments, boolean allowTrailingCommas,
                                  boolean checkLen) {
        JsonIndex sidx = new JsonIndex(contents);
        Expr result = null;
        Failure failure = null;
        try {
            JsonStage2 parser = new JsonStage2(contents, log, sidx, allowTrailingCommas);
            result = parser.parseValue();
            if (checkLen && !parser.atTrailingEnd()) {
                throw parser.unexpectedHere();
            }
        } catch (Failure f) {
            failure = f;
        }
        if (sidx.indexError != null) {
            int pos = sidx.indexErrorPos;
            boolean rejectedComment = !allowComments && sidx.firstComment >= 0 && sidx.firstComment < pos;
            if (!rejectedComment) {
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
        }
        if (!allowComments && sidx.firstComment >= 0) {
            int firstError = Integer.MAX_VALUE;
            for (int i = 0; i < log.size(); i++) {
                firstError = Math.min(firstError, log.offset(i));
            }
            if (firstError >= sidx.firstComment) {
                log.truncate(0);
                log.add("JSON does not support comments", sidx.firstComment, 0);
                throw new Failure(SYNTAX_ERROR);
            }
        }
        if (sidx.indexError != null) {
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
        int comma = lexer.start;
        int commaLength = lexer.end - lexer.start;
        lexer.expect(JsonLexer.T.COMMA);
        if (lexer.token == closer) {
            if (!allowTrailingCommas) {
                log.add("JSON does not support trailing commas", comma, commaLength);
            }
            return false;
        }
        return true;
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
