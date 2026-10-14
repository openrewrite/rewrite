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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.StripBom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of yarn src/lockfile/parse.js (yarn 1.22.22): yarn's tokenizer and parser for the lockfile syntax (which
 * .yarnrc shares), the merge of a lock holding git conflict markers, and the js-yaml fallback.
 */
final class LockfileParser {

    private static final JsYaml.Options YAML = new JsYaml.Options(JsYaml.Release.V3_13_1, false, PlainObject::new);

    static final int LOCKFILE_VERSION = 1;

    static final String MERGE = "merge";
    static final String SUCCESS = "success";
    static final String CONFLICT = "conflict";

    private static final Pattern VERSION_REGEX = Pattern.compile("^yarn lockfile v(\\d+)\\z");
    private static final Pattern DIGITS = Pattern.compile("^[0-9]+");

    private static final String MERGE_CONFLICT_ANCESTOR = "|||||||";
    private static final String MERGE_CONFLICT_END = ">>>>>>>";
    private static final String MERGE_CONFLICT_SEP = "=======";
    private static final String MERGE_CONFLICT_START = "<<<<<<<";

    private LockfileParser() {
    }

    @Value
    static class ParseResult {
        String type;

        @Nullable
        Object object;
    }

    enum TokenType {
        BOOLEAN, STRING, IDENTIFIER, EOF, COLON, NEWLINE, COMMENT, INDENT, INVALID, NUMBER, COMMA
    }

    @Value
    static class Token {
        int line;
        int col;
        TokenType type;

        @Nullable
        Object value;

        boolean isValidPropValue() {
            return type == TokenType.BOOLEAN || type == TokenType.STRING || type == TokenType.NUMBER;
        }

        String inspect() {
            String v = value == null ? "undefined" : value instanceof String ? "'" + value + "'" :
                    value instanceof Double ? Js.numberToString((Double) value) : String.valueOf(value);
            return "{ line: " + line + ", col: " + col + ", type: '" + type + "', value: " + v + " }";
        }
    }

    /**
     * {@code tokenise}, a generator: each call runs the loop until it yields.
     */
    static final class Tokenizer {
        private String input;
        private final YarnClassicRelease release;
        private boolean lastNewline;
        private int line = 1;
        private int col;
        private final Deque<Token> pending = new ArrayDeque<>();
        private boolean done;

        Tokenizer(String input, YarnClassicRelease release) {
            this.input = input;
            this.release = release;
        }

        // 1.12.0's loops past the end of the input, which add `undefined` to the token until V8 refuses its length
        private void readPastTheEnd() {
            if (!release.endsTokensAtTheEndOfInput()) {
                throw new JsError(JsError.RANGE_ERROR, "Invalid string length");
            }
        }

        @Nullable
        Token next() {
            while (pending.isEmpty()) {
                if (done) {
                    return null;
                }
                step();
            }
            return pending.poll();
        }

        private void step() {
            if (input.isEmpty()) {
                pending.add(buildToken(TokenType.EOF, null));
                done = true;
                return;
            }
            int chop = 0;
            char c0 = input.charAt(0);
            boolean carriageReturns = release.readsCarriageReturnsAsLineEnds();
            if (c0 == '\n' || c0 == '\r' && carriageReturns) {
                chop++;
                if (carriageReturns && input.length() > 1 && input.charAt(1) == '\n') {
                    chop++;
                }
                line++;
                col = 0;
                pending.add(buildToken(TokenType.NEWLINE, null));
            } else if (c0 == '#') {
                chop++;
                int nextNewline = input.indexOf('\n', chop);
                if (nextNewline == -1) {
                    readPastTheEnd();
                    nextNewline = input.length();
                }
                String val = input.substring(chop, nextNewline);
                chop = nextNewline;
                pending.add(buildToken(TokenType.COMMENT, val));
            } else if (c0 == ' ') {
                if (lastNewline) {
                    int indentSize = 1;
                    for (int i = 1; i < input.length() && input.charAt(i) == ' '; i++) {
                        indentSize++;
                    }
                    if (indentSize % 2 != 0) {
                        throw new JsError(JsError.TYPE_ERROR, "Invalid number of spaces");
                    }
                    chop = indentSize;
                    pending.add(buildToken(TokenType.INDENT, (double) (indentSize / 2)));
                } else {
                    chop++;
                }
            } else if (c0 == '"') {
                int i = 1;
                boolean closed = false;
                for (; i < input.length(); i++) {
                    if (input.charAt(i) == '"') {
                        boolean isEscaped = input.charAt(i - 1) == '\\' && (i < 2 || input.charAt(i - 2) != '\\');
                        if (!isEscaped) {
                            i++;
                            closed = true;
                            break;
                        }
                    }
                }
                if (!closed) {
                    readPastTheEnd();
                }
                String val = input.substring(0, i);
                chop = i;
                Object parsed;
                try {
                    parsed = Js.jsonParse(val);
                } catch (JsError e) {
                    parsed = e;
                }
                pending.add(parsed instanceof JsError ? buildToken(TokenType.INVALID, null) :
                        buildToken(TokenType.STRING, parsed));
            } else if (c0 >= '0' && c0 <= '9') {
                Matcher m = DIGITS.matcher(input);
                String val = m.find() ? m.group() : "";
                chop = val.length();
                pending.add(buildToken(TokenType.NUMBER, Double.parseDouble(val)));
            } else if (input.startsWith("true")) {
                pending.add(buildToken(TokenType.BOOLEAN, true));
                chop = 4;
            } else if (input.startsWith("false")) {
                pending.add(buildToken(TokenType.BOOLEAN, false));
                chop = 5;
            } else if (c0 == ':') {
                pending.add(buildToken(TokenType.COLON, null));
                chop++;
            } else if (c0 == ',') {
                pending.add(buildToken(TokenType.COMMA, null));
                chop++;
            } else if ((c0 >= 'a' && c0 <= 'z') || (c0 >= 'A' && c0 <= 'Z') || c0 == '/' || c0 == '-' ||
                       c0 == '.' && release.readsAValueAfterAColon()) {
                int i = 0;
                for (; i < input.length(); i++) {
                    char c = input.charAt(i);
                    if (c == ':' || c == ' ' || c == '\n' || c == '\r' && carriageReturns || c == ',') {
                        break;
                    }
                }
                String name = input.substring(0, i);
                chop = i;
                pending.add(buildToken(TokenType.STRING, name));
            } else {
                pending.add(buildToken(TokenType.INVALID, null));
            }

            if (chop == 0) {
                pending.add(buildToken(TokenType.INVALID, null));
            }

            col += chop;
            lastNewline = c0 == '\n' || (carriageReturns && c0 == '\r' && input.length() > 1 && input.charAt(1) == '\n');
            input = input.substring(chop);
        }

        private Token buildToken(TokenType type, @Nullable Object value) {
            return new Token(line, col, type, value);
        }
    }

    static final class Parser {
        final List<String> comments = new ArrayList<>();
        private final Tokenizer tokens;
        private final String fileLoc;
        private final YarnClassicRelease release;
        @SuppressWarnings("NotNullFieldNotInitialized")
        private Token token;

        Parser(String input, String fileLoc, YarnClassicRelease release) {
            this.tokens = new Tokenizer(input, release);
            this.fileLoc = fileLoc;
            this.release = release;
        }

        private void onComment(Token token) {
            String comment = Js.trim(String.valueOf(token.getValue()));
            Matcher versionMatch = VERSION_REGEX.matcher(comment);
            if (versionMatch.find()) {
                double version = Double.parseDouble(versionMatch.group(1));
                if (version > LOCKFILE_VERSION) {
                    throw new MessageError("Can't install from a lockfile of version " + Js.numberToString(version) +
                                           " as you're on an old yarn version that only supports versions up to " +
                                           LOCKFILE_VERSION + ". Run `$ yarn self-update` to upgrade to the latest " +
                                           "version.");
                }
            }
            comments.add(comment);
        }

        Token next() {
            Token value = tokens.next();
            if (value == null) {
                throw new JsError(JsError.ERROR, "No more tokens");
            } else if (value.getType() == TokenType.COMMENT) {
                onComment(value);
                return next();
            }
            return token = value;
        }

        private JsError unexpected(String msg) {
            return new JsError(JsError.SYNTAX_ERROR,
                    msg + " " + token.getLine() + ":" + token.getCol() + " in " + fileLoc);
        }

        Map<String, @Nullable Object> parse(int indent) {
            Map<String, @Nullable Object> obj = new LinkedHashMap<>();
            while (true) {
                Token propToken = token;
                if (propToken.getType() == TokenType.NEWLINE) {
                    Token nextToken = next();
                    if (indent == 0) {
                        continue;
                    }
                    if (nextToken.getType() != TokenType.INDENT) {
                        break;
                    }
                    if (Double.valueOf(indent).equals(nextToken.getValue())) {
                        next();
                    } else {
                        break;
                    }
                } else if (propToken.getType() == TokenType.INDENT) {
                    if (Double.valueOf(indent).equals(propToken.getValue())) {
                        next();
                    } else {
                        break;
                    }
                } else if (propToken.getType() == TokenType.EOF) {
                    break;
                } else if (propToken.getType() == TokenType.STRING) {
                    String key = (String) propToken.getValue();
                    invariantKey(key);
                    List<String> keys = new ArrayList<>();
                    keys.add(key);
                    next();
                    while (token.getType() == TokenType.COMMA) {
                        next();
                        Token keyToken = token;
                        if (keyToken.getType() != TokenType.STRING) {
                            throw unexpected("Expected string");
                        }
                        String k = (String) keyToken.getValue();
                        invariantKey(k);
                        keys.add(k);
                        next();
                    }
                    boolean wasColon = token.getType() == TokenType.COLON;
                    if (wasColon) {
                        next();
                    }
                    // before 1.16.0 a colon opens an object whatever follows it
                    if (token.isValidPropValue() && (!wasColon || release.readsAValueAfterAColon())) {
                        for (String k : keys) {
                            obj.put(k, token.getValue());
                        }
                        next();
                    } else if (wasColon) {
                        Map<String, @Nullable Object> val = parse(indent + 1);
                        for (String k : keys) {
                            obj.put(k, val);
                        }
                        if (indent != 0 && token.getType() != TokenType.INDENT) {
                            break;
                        }
                    } else {
                        throw unexpected("Invalid value type");
                    }
                } else {
                    throw unexpected(release.inspectsTheUnknownToken() ? "Unknown token: " + propToken.inspect() :
                            "Unknown token");
                }
            }
            return obj;
        }

        private static void invariantKey(@Nullable String key) {
            if (key == null || key.isEmpty()) {
                throw new JsError(Js.INVARIANT_VIOLATION, "Expected a key");
            }
        }
    }

    static String[] extractConflictVariants(String str) {
        List<String> variant0 = new ArrayList<>();
        List<String> variant1 = new ArrayList<>();
        Deque<String> lines = new ArrayDeque<>(Arrays.asList(str.split("\\r?\\n", -1)));
        boolean skip = false;
        while (!lines.isEmpty()) {
            String line = lines.removeFirst();
            if (line.startsWith(MERGE_CONFLICT_START)) {
                while (!lines.isEmpty()) {
                    String conflictLine = lines.removeFirst();
                    if (conflictLine.equals(MERGE_CONFLICT_SEP)) {
                        skip = false;
                        break;
                    } else if (skip || conflictLine.startsWith(MERGE_CONFLICT_ANCESTOR)) {
                        skip = true;
                    } else {
                        variant0.add(conflictLine);
                    }
                }
                while (!lines.isEmpty()) {
                    String conflictLine = lines.removeFirst();
                    if (conflictLine.startsWith(MERGE_CONFLICT_END)) {
                        break;
                    } else {
                        variant1.add(conflictLine);
                    }
                }
            } else {
                variant0.add(line);
                variant1.add(line);
            }
        }
        return new String[]{String.join("\n", variant0), String.join("\n", variant1)};
    }

    static boolean hasMergeConflicts(String str) {
        return str.contains(MERGE_CONFLICT_START) && str.contains(MERGE_CONFLICT_SEP) &&
               str.contains(MERGE_CONFLICT_END);
    }

    static @Nullable Object parse(String str, String fileLoc, YarnClassicRelease release) {
        Parser parser = new Parser(str, fileLoc, release);
        parser.next();
        if (!fileLoc.endsWith(".yml") || !release.parsesYmlAsYaml()) {
            if (!release.readsWithYamlWhatTheLockfileParserCannot()) {
                return parser.parse(0);
            }
            try {
                return parser.parse(0);
            } catch (RuntimeException error1) {
                try {
                    return safeLoadFailsafe(str);
                } catch (RuntimeException error2) {
                    throw error1;
                }
            }
        }
        Object result = safeLoadFailsafe(str);
        if (!release.readsYmlOfNoObjectAsEmpty()) {
            return result;
        }
        // typeof result === 'object': a failsafe document holds no null, and none is undefined
        return result instanceof Map || result instanceof List ? result : new PlainObject();
    }

    // js-yaml's errors as yarn's JavaScript sees them
    static @Nullable Object safeLoadFailsafe(String str) {
        try {
            return JsYaml.safeLoadFailsafe(str, YAML);
        } catch (JsYaml.YamlException e) {
            throw new JsError("YAMLException", String.valueOf(e.getMessage()));
        }
    }

    static ParseResult parseWithConflict(String str, String fileLoc, YarnClassicRelease release) {
        String[] variants = extractConflictVariants(str);
        try {
            Map<String, @Nullable Object> merged = new PlainObject();
            Object first = parse(variants[0], fileLoc, release);
            Object second = parse(variants[1], fileLoc, release);
            Js.assign(merged, first);
            Js.assign(merged, second);
            return new ParseResult(MERGE, merged);
        } catch (JsError err) {
            if (err.isSyntaxError()) {
                return new ParseResult(CONFLICT, new PlainObject());
            }
            throw err;
        }
    }

    /**
     * The module's default export.
     */
    static ParseResult parseLockfile(String str, String fileLoc) {
        return parseLockfile(str, fileLoc, YarnClassicRelease.PORTED);
    }

    /**
     * The module's default export as {@code release} parses.
     */
    static ParseResult parseLockfile(String str, String fileLoc, YarnClassicRelease release) {
        str = StripBom.stripBom(str);
        return hasMergeConflicts(str) ? parseWithConflict(str, fileLoc, release) :
                new ParseResult(SUCCESS, parse(str, fileLoc, release));
    }

    static ParseResult parseLockfile(String str) {
        return parseLockfile(str, "lockfile");
    }
}
