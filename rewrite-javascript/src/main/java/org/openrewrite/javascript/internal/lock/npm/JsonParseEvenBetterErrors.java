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
package org.openrewrite.javascript.internal.lock.npm;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.EcmaScript.JsonSyntaxError;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of json-parse-even-better-errors 5.0.0: {@code JSON.parse} that also notes the indentation and
 * newline of the text, so that a file can be written back the way it was.
 */
public final class JsonParseEvenBetterErrors {

    /**
     * Thrown as {@code JSONParseError} is, with its {@code EJSONPARSE} code.
     */
    public static final class JsonParseException extends IllegalArgumentException implements NpmError {
        JsonParseException(String message, @Nullable Throwable cause) {
            super(message, cause);
        }

        @Override
        public String getCode() {
            return "EJSONPARSE";
        }
    }

    /**
     * The parsed value with the formatting the text had; {@code indent} and {@code newline} are null
     * for a value that is not an object or array.
     */
    @Value
    static class Parsed {
        @Nullable
        Object value;

        @Nullable
        String indent;

        @Nullable
        String newline;
    }

    private static final String DEFAULT_NEWLINE = "\n";
    private static final String DEFAULT_INDENT = "  ";
    private static final Pattern FORMAT = Pattern.compile("^[" + Js.WHITESPACE_CLASS + "]*[{\\[]((?:\\r?\\n)+)([" +
                                                          Js.WHITESPACE_CLASS + "\\t]*)");
    private static final Pattern EMPTY = Pattern.compile("^(?:\\{}|\\[])((?:\\r?\\n)+)?$");

    private static final Pattern JSON_WHITESPACE = Pattern.compile("[ \\t\\n\\r]*");

    private JsonParseEvenBetterErrors() {
    }

    /**
     * {@code JSON.parse(text)}.
     */
    public static @Nullable Object jsonParse(String text) {
        try {
            return Js.jsonParse(text);
        } catch (JsonSyntaxError e) {
            throw new JsonParseException(e.getCause().getMessage(), e.getCause());
        }
    }

    static Parsed parse(String raw) {
        String txt = raw.startsWith("\uFEFF") ? raw.substring(1) : raw;
        Object result;
        String parsing = txt.isEmpty() ? "empty string" : Js.quote(txt.length() > 40 ? txt.substring(0, 40) : txt);
        if (JSON_WHITESPACE.matcher(txt).matches()) {
            throw new JsonParseException("Unexpected end of JSON input while parsing " + parsing, null);
        }
        try {
            result = Js.jsonParse(txt);
        } catch (JsonSyntaxError e) {
            throw new JsonParseException(e.getCause().getMessage() + " while parsing " + parsing, e.getCause());
        }
        if (result instanceof Map || result instanceof List) {
            Matcher empty = EMPTY.matcher(txt);
            if (empty.find()) {
                return new Parsed(result, DEFAULT_INDENT, empty.group(1) != null ? empty.group(1) : DEFAULT_NEWLINE);
            }
            Matcher format = FORMAT.matcher(txt);
            if (format.find()) {
                return new Parsed(result, format.group(2), format.group(1));
            }
            return new Parsed(result, "", "");
        }
        return new Parsed(result, null, null);
    }
}
