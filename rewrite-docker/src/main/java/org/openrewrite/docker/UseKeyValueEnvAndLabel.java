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
package org.openrewrite.docker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.internal.ParserDirectives;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseKeyValueEnvAndLabel extends Recipe {

    String displayName = "Use the `key=value` form of `ENV` and `LABEL`";

    String description = "BuildKit's `LegacyKeyValueFormat` check reports an `ENV key value` or `LABEL key value`, whose " +
            "value is the whole rest of the line, in favour of `ENV key=value`, whose value ends at the next " +
            "space. A value that spans a space therefore has to be quoted to keep its meaning, and one whose " +
            "quoting cannot be decided from the source, because it already carries quotes or escapes of its " +
            "own, is left in the legacy form rather than silently given a different value. A file whose " +
            "`# escape=` directive names the backtick is left alone, because a backslash escapes nothing there.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                return ParserDirectives.escapeChar(file) == '\\' ? super.visitFile(file, ctx) : file;
            }

            @Override
            public Docker.Env.EnvPair visitEnvPair(Docker.Env.EnvPair pair, ExecutionContext ctx) {
                Docker.Env.EnvPair p = super.visitEnvPair(pair, ctx);
                if (p.isHasEquals()) {
                    return p;
                }
                Docker.Argument value = singleWord(p.getValue());
                return value == null ? p : p.withHasEquals(true).withValue(value.withPrefix(Space.EMPTY));
            }

            @Override
            public Docker.Label.LabelPair visitLabelPair(Docker.Label.LabelPair pair, ExecutionContext ctx) {
                Docker.Label.LabelPair p = super.visitLabelPair(pair, ctx);
                if (p.isHasEquals()) {
                    return p;
                }
                Docker.Argument value = singleWord(p.getValue());
                return value == null ? p : p.withHasEquals(true).withValue(value.withPrefix(Space.EMPTY));
            }
        };
    }

    /// The value as the `key=value` form has to write it, or null when no such word can be built from the source.
    /// A value written hard against its key is not the legacy form at all. One that opens with an `=` cannot be
    /// converted unquoted: `ENV KEY==value` reads back as the legacy form with a value of `==value`.
    private static Docker.@Nullable Argument singleWord(Docker.Argument value) {
        if (value.getPrefix().getWhitespace().isEmpty()) {
            return null;
        }
        String text = ArgumentContents.textWithVariables(value);
        if (ArgumentContents.quoteStyle(value) != null || isSingleWord(text)) {
            return opensWithEquals(value) ? null : value;
        }
        if (text.indexOf('"') >= 0 || text.indexOf('\'') >= 0 || text.indexOf('\\') >= 0) {
            return null;
        }
        return value.withContents(ArgumentContents.of(text, Docker.Literal.QuoteStyle.DOUBLE));
    }

    private static boolean opensWithEquals(Docker.Argument value) {
        List<Docker.ArgumentContent> contents = value.getContents();
        if (contents.isEmpty() || !(contents.get(0) instanceof Docker.Literal)) {
            return false;
        }
        Docker.Literal first = (Docker.Literal) contents.get(0);
        return first.getQuoteStyle() == null && first.getText().startsWith("=");
    }

    /// Whether the shell reads `text` as one word, which it does when every space it holds is quoted or escaped.
    /// A value whose quotes never close is not one.
    private static boolean isSingleWord(String text) {
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && !singleQuoted && i + 1 < text.length()) {
                i++;
            } else if (c == '\'' && !doubleQuoted) {
                singleQuoted = !singleQuoted;
            } else if (c == '"' && !singleQuoted) {
                doubleQuoted = !doubleQuoted;
            } else if (!singleQuoted && !doubleQuoted && Character.isWhitespace(c)) {
                return false;
            }
        }
        return !singleQuoted && !doubleQuoted;
    }
}
