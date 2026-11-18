/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.toml;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.SourceFile;
import org.openrewrite.toml.internal.grammar.TomlLexer;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the TOML a recipe is given as an option, accepting only what parses without error.
 */
final class TomlSnippets {
    private TomlSnippets() {
    }

    /**
     * @return the document, or null if the text is not TOML, including when error recovery read past what is not
     */
    static Toml.@Nullable Document parse(String toml) {
        SourceFile parsed = new TomlParser().parse(new InMemoryExecutionContext(t -> {
        }), toml).findFirst().orElse(null);
        if (!(parsed instanceof Toml.Document) || parsed.getMarkers().findFirst(ParseExceptionResult.class).isPresent()) {
            return null;
        }
        return (Toml.Document) parsed;
    }

    /**
     * @return the value {@code value} is written as, such as an array for {@code ["a", "b"]}, or null if it is no
     * TOML value
     */
    static @Nullable Toml value(String value) {
        Toml.Document doc = parse("value = " + value);
        if (doc == null || doc.getValues().size() != 1) {
            return null;
        }
        TomlValue kv = doc.getValues().get(0);
        return kv instanceof Toml.KeyValue ? ((Toml.KeyValue) kv).getValue() : null;
    }

    /**
     * @return the key-values of a table row, or null if the row is not TOML
     */
    static @Nullable List<Toml.KeyValue> keyValues(String row) {
        Toml.Document doc = parse(row);
        if (doc == null) {
            return null;
        }
        List<Toml.KeyValue> keyValues = new ArrayList<>();
        for (TomlValue value : doc.getValues()) {
            if (value instanceof Toml.KeyValue) {
                keyValues.add((Toml.KeyValue) value);
            }
        }
        return keyValues;
    }

    /**
     * What a {@code null} value is read as. A string can hold it, but no row means it.
     */
    private static final String NULL = "\u0000null";

    /**
     * Like {@link #keyValues(String)}, but also reads a key-value such as {@code enabled = null}, which is no TOML
     * but how a row says to remove a property. {@link #isNull(Toml.KeyValue)} tells it apart. A {@code null} is
     * found where the lexer fails on it right after an {@code =}, so one inside a string or a comment stays text.
     *
     * @return the key-values, or null if the row is not TOML, or has a {@code null} other than as a key's value
     */
    static @Nullable List<Toml.KeyValue> keyValuesWithNulls(String row) {
        String toml = row;
        int nulls = 0;
        for (int at = firstLexerError(toml); at >= 0; at = firstLexerError(toml)) {
            if (!isNullValue(toml, at)) {
                return null;
            }
            toml = toml.substring(0, at) + '"' + NULL.replace("\u0000", "\\u0000") + '"' + toml.substring(at + 4);
            nulls++;
        }
        List<Toml.KeyValue> keyValues = keyValues(toml);
        if (keyValues == null || keyValues.stream().filter(TomlSnippets::isNull).count() != nulls) {
            return null; // a null inside an array or an inline table
        }
        return keyValues;
    }

    /**
     * @return whether {@code kv} is a key-value {@link #keyValuesWithNulls(String)} read from {@code key = null}
     */
    static boolean isNull(Toml.KeyValue kv) {
        return kv.getValue() instanceof Toml.Literal && NULL.equals(((Toml.Literal) kv.getValue()).getValue());
    }

    private static boolean isNullValue(String toml, int at) {
        if (!toml.startsWith("null", at) ||
            (at + 4 < toml.length() && !Character.isWhitespace(toml.charAt(at + 4)) && toml.charAt(at + 4) != '#')) {
            return false;
        }
        int before = at - 1;
        while (before >= 0 && (toml.charAt(before) == ' ' || toml.charAt(before) == '\t')) {
            before--;
        }
        return before >= 0 && toml.charAt(before) == '=';
    }

    /**
     * @return the index into {@code toml} of the first character the lexer fails on, or -1 if it fails on none
     */
    private static int firstLexerError(String toml) {
        int[] at = {-1};
        TomlLexer lexer = new TomlLexer(CharStreams.fromString(toml));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                    int charPositionInLine, String msg, RecognitionException e) {
                if (at[0] < 0) {
                    // in code points, as the char stream counts them
                    at[0] = toml.offsetByCodePoints(0, ((Lexer) recognizer)._tokenStartCharIndex);
                }
            }
        });
        for (Token t = lexer.nextToken(); t.getType() != Token.EOF && at[0] < 0; t = lexer.nextToken()) {
            // lex until the first error
        }
        return at[0];
    }
}
