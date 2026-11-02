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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/parsers sources/resolution.ts and sources/grammars/resolution.pegjs (Yarn 4.10.3).
 */
@Value
class Resolution {

    /**
     * The error parseResolution raises for a resolution key, which Yarn reports and drops the entry for; null for one
     * it parses.
     */
    static @Nullable String rejects(String key) {
        try {
            parseResolution(key);
            return null;
        } catch (JsError e) {
            return e.getMessage();
        }
    }

    private static final Pattern LEGACY_RESOLUTION = Pattern.compile("^\\*{1,2}/(.*)");

    @Value
    static class Specifier {
        String fullName;

        @Nullable
        String description;
    }

    @Nullable
    Specifier from;

    Specifier descriptor;

    /**
     * @throws JsError for a glob pattern, or a key the grammar does not match.
     */
    static Resolution parseResolution(String source) {
        Matcher legacy = LEGACY_RESOLUTION.matcher(source);
        if (legacy.find()) {
            throw new JsError(JsError.ERROR, "The override for '" + source + "' includes a glob pattern. Glob " +
                                             "patterns have been removed since their behaviours don't match what " +
                                             "you'd expect. Set the override to '" + legacy.group(1) + "' instead.");
        }
        int slash = specifierEnd(source, 0);
        if (slash < source.length() && source.charAt(slash) == '/') {
            int end = specifierEnd(source, slash + 1);
            if (slash > 0 && end == source.length() && end > slash + 1) {
                return new Resolution(specifier(source.substring(0, slash)),
                        specifier(source.substring(slash + 1)));
            }
        }
        int end = specifierEnd(source, 0);
        if (end == source.length() && end > 0) {
            return new Resolution(null, specifier(source));
        }
        throw new JsError(JsError.ERROR, "Expected a resolution, got \"" + source + "\"");
    }

    // where `specifier` stops matching from `start`: fullName ("@" ident "/" ident, or ident) then "@" [^/]+
    private static int specifierEnd(String source, int start) {
        int pos = start;
        int nameEnd = -1;
        if (pos < source.length() && source.charAt(pos) == '@') {
            int scopeEnd = identEnd(source, pos + 1);
            if (scopeEnd > pos + 1 && scopeEnd < source.length() && source.charAt(scopeEnd) == '/') {
                int end = identEnd(source, scopeEnd + 1);
                if (end > scopeEnd + 1) {
                    nameEnd = end;
                }
            }
        }
        if (nameEnd == -1) {
            int end = identEnd(source, pos);
            if (end == pos) {
                return start;
            }
            nameEnd = end;
        }
        pos = nameEnd;
        if (pos < source.length() && source.charAt(pos) == '@') {
            int end = pos + 1;
            while (end < source.length() && source.charAt(end) != '/') {
                end++;
            }
            if (end > pos + 1) {
                return end;
            }
        }
        return nameEnd;
    }

    private static int identEnd(String source, int start) {
        int end = start;
        while (end < source.length() && source.charAt(end) != '/' && source.charAt(end) != '@') {
            end++;
        }
        return end;
    }

    private static Specifier specifier(String text) {
        int nameEnd = text.startsWith("@") ? text.indexOf('@', text.indexOf('/')) : text.indexOf('@');
        if (nameEnd == -1) {
            return new Specifier(text, null);
        }
        return new Specifier(text.substring(0, nameEnd), text.substring(nameEnd + 1));
    }

    static String stringifyResolution(Resolution resolution) {
        StringBuilder str = new StringBuilder();
        if (resolution.from != null) {
            str.append(resolution.from.fullName);
            if (resolution.from.description != null && !resolution.from.description.isEmpty()) {
                str.append('@').append(resolution.from.description);
            }
            str.append('/');
        }
        str.append(resolution.descriptor.fullName);
        if (resolution.descriptor.description != null && !resolution.descriptor.description.isEmpty()) {
            str.append('@').append(resolution.descriptor.description);
        }
        return str.toString();
    }
}
