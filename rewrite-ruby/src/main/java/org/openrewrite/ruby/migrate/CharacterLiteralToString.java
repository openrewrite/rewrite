/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.migrate;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.ruby.RubyIsoVisitor;
import org.openrewrite.ruby.marker.CharacterLiteral;

public class CharacterLiteralToString extends Recipe {

    @Override
    public String getDisplayName() {
        return "Write character literals as strings";
    }

    @Override
    public String getDescription() {
        return "In Ruby 1.9 and later, characters are simply strings of length 1. That is, the literal `?A` is " +
               "the same as the literal `'A'`, and there is really no need for the character literal syntax in " +
               "new code.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RubyIsoVisitor<ExecutionContext>() {
            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                if (!literal.getMarkers().findFirst(CharacterLiteral.class).isPresent() ||
                    !(literal.getValue() instanceof String)) {
                    return literal;
                }
                String value = (String) literal.getValue();
                String quoted = quote(value);
                return quoted == null ? literal :
                        literal.withMarkers(literal.getMarkers().removeByType(CharacterLiteral.class))
                                .withValueSource(quoted);
            }
        };
    }

    /**
     * The character as a string literal that evaluates to it, or {@code null} when it cannot be
     * written as one this way. Single quotes take a printable character as-is; everything else has
     * to be double-quoted so that the escape means what it did after the {@code ?}.
     */
    private static @Nullable String quote(String value) {
        if (value.codePointCount(0, value.length()) != 1) {
            return null;
        }
        int c = value.codePointAt(0);
        switch (c) {
            case '\'':
                return "\"'\"";
            case '\\':
                return "\"\\\\\"";
            case '\n':
                return "\"\\n\"";
            case '\r':
                return "\"\\r\"";
            case '\t':
                return "\"\\t\"";
            case 0x00:
                return "\"\\0\"";
            case 0x07:
                return "\"\\a\"";
            case 0x08:
                return "\"\\b\"";
            case 0x0B:
                return "\"\\v\"";
            case 0x0C:
                return "\"\\f\"";
            case 0x1B:
                return "\"\\e\"";
            default:
                // `?\s` is just a space, which single quotes take literally like any other printable
                return Character.isISOControl(c) ? String.format("\"\\x%02X\"", c) : "'" + value + "'";
        }
    }
}
