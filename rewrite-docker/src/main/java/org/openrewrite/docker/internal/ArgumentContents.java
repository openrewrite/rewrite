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
package org.openrewrite.docker.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * Builds the contents of a {@link Docker.Argument} from text, and reads them back out. Both the
 * parser and recipes that synthesize values go through here, so a value a recipe writes is modelled
 * the same way as one read back from the printed Dockerfile.
 */
public class ArgumentContents {
    private ArgumentContents() {
    }

    /// The contents of a value whose text is {@code text} and whose quote style, if the value is a
    /// single quoted string, is {@code quoteStyle}. Single quotes never expand, so such a value is
    /// always one literal. Double quotes do, and a value holding a reference is no longer one
    /// literal, which puts its quotes into the text.
    public static List<Docker.ArgumentContent> of(String text, Docker.Literal.@Nullable QuoteStyle quoteStyle) {
        if (quoteStyle == Docker.Literal.QuoteStyle.SINGLE ||
                (quoteStyle == Docker.Literal.QuoteStyle.DOUBLE && !containsVariable(text))) {
            return singletonList(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, text, quoteStyle));
        }
        if (quoteStyle == Docker.Literal.QuoteStyle.DOUBLE) {
            return splitVariables('"' + text + '"', Space.EMPTY);
        }
        return splitVariables(text, Space.EMPTY);
    }

    /// Splits environment variable references out of a value's text. Token boundaries are no guide here:
    /// the lexer emits `--name=value` as a single token, so a reference can sit inside one. What counts
    /// as a reference mirrors the lexer's `ENV_VAR` and `SPECIAL_VAR` rules, so `$$` and `$1` stay text.
    public static List<Docker.ArgumentContent> splitVariables(String text, Space prefix) {
        List<Docker.ArgumentContent> contents = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuoted = false;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : 0;
            if (c == '\'') {
                singleQuoted = !singleQuoted;
                current.append(c);
                i++;
                continue;
            }
            if (c == '\\' && next != 0 && !singleQuoted) {
                current.append(c).append(next);
                i += 2;
                continue;
            }
            if (singleQuoted) {
                current.append(c);
                i++;
                continue;
            }
            String name = null;
            boolean braced = false;
            if (c == '$' && next == '{') {
                int close = text.indexOf('}', i + 2);
                if (close > i + 2 && isVarStart(text.charAt(i + 2))) {
                    name = text.substring(i + 2, close);
                    braced = true;
                    i = close + 1;
                }
            } else if (c == '$' && isVarStart(next)) {
                int end = i + 1;
                while (end < text.length() && isVarPart(text.charAt(end))) {
                    end++;
                }
                name = text.substring(i + 1, end);
                i = end;
            } else if (c == '$' && isSpecialVar(next)) {
                current.append(c).append(next);
                i += 2;
                continue;
            }
            if (name == null) {
                current.append(c);
                i++;
                continue;
            }
            if (current.length() > 0) {
                contents.add(new Docker.Literal(randomId(), contents.isEmpty() ? prefix : Space.EMPTY,
                        Markers.EMPTY, current.toString(), null));
                current.setLength(0);
            }
            contents.add(new Docker.EnvironmentVariable(randomId(), contents.isEmpty() ? prefix : Space.EMPTY,
                    Markers.EMPTY, name, braced));
        }
        if (current.length() > 0 || contents.isEmpty()) {
            contents.add(new Docker.Literal(randomId(), contents.isEmpty() ? prefix : Space.EMPTY,
                    Markers.EMPTY, current.toString(), null));
        }
        return contents;
    }

    /// @return The text of every content of `argument`, or `null` if an environment variable
    /// reference makes it impossible to resolve statically.
    public static @Nullable String text(Docker.Argument argument) {
        StringBuilder text = new StringBuilder();
        for (Docker.ArgumentContent content : argument.getContents()) {
            if (content instanceof Docker.EnvironmentVariable) {
                return null;
            }
            if (content instanceof Docker.Literal) {
                text.append(((Docker.Literal) content).getText());
            }
        }
        return text.toString();
    }

    /// @return As [#text], but rendering environment variable references in their original
    /// `$VAR` or `${VAR}` form rather than giving up.
    public static String textWithVariables(Docker.Argument argument) {
        StringBuilder text = new StringBuilder();
        for (Docker.ArgumentContent content : argument.getContents()) {
            if (content instanceof Docker.Literal) {
                text.append(((Docker.Literal) content).getText());
            } else if (content instanceof Docker.EnvironmentVariable) {
                Docker.EnvironmentVariable env = (Docker.EnvironmentVariable) content;
                text.append(env.isBraced() ? "${" + env.getName() + "}" : "$" + env.getName());
            }
        }
        return text.toString();
    }

    /// @return The quote style of the first quoted literal in `argument`, or `null` if none is quoted.
    public static Docker.Literal.@Nullable QuoteStyle quoteStyle(Docker.Argument argument) {
        for (Docker.ArgumentContent content : argument.getContents()) {
            if (content instanceof Docker.Literal) {
                Docker.Literal.QuoteStyle style = ((Docker.Literal) content).getQuoteStyle();
                if (style != null) {
                    return style;
                }
            }
        }
        return null;
    }

    public static boolean containsVariable(Docker.Argument argument) {
        for (Docker.ArgumentContent content : argument.getContents()) {
            if (content instanceof Docker.EnvironmentVariable) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsVariable(String text) {
        for (Docker.ArgumentContent content : splitVariables(text, Space.EMPTY)) {
            if (content instanceof Docker.EnvironmentVariable) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVarStart(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
    }

    private static boolean isVarPart(char c) {
        return isVarStart(c) || (c >= '0' && c <= '9');
    }

    private static boolean isSpecialVar(char c) {
        return c == '!' || c == '$' || c == '?' || c == '#' || c == '@' || c == '*' || (c >= '0' && c <= '9');
    }
}
