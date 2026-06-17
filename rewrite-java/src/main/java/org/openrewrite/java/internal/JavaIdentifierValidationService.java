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
package org.openrewrite.java.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.IdentifierValidationService;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Validates that identifier names contain no characters that cannot appear in an identifier of a J-based language.
 * A common parser bug is to store raw source text (e.g. a type expression or a constructor call) into a
 * {@code J.Identifier} name; this service catches that.
 * <p>
 * The default rule flags the structural characters (whitespace, brackets, parentheses, braces, comma, semicolon)
 * that cannot appear in an identifier of any of these languages, while whitelisting quoted/backtick-quoted names
 * (e.g. Groovy {@code 'some name'} or Kotlin {@code `some name`}) which may contain any character. Languages that
 * need different rules can subclass and override {@link #isWhitelisted(String)}.
 */
public class JavaIdentifierValidationService implements IdentifierValidationService {

    private static final String INVALID_CHARS = " \t\r\n()[]{},;";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                if (isInvalid(identifier)) {
                    return identifier.withSimpleName("~~(invalid-identifier)~~>" + identifier.getSimpleName() + "<~~");
                }
                return identifier;
            }
        };
    }

    /**
     * Whether the given identifier's name contains characters that cannot appear in an identifier of this language.
     * Subclasses can override to account for language-specific quoting (e.g. a marker indicating the name was quoted).
     */
    protected boolean isInvalid(J.Identifier identifier) {
        String name = identifier.getSimpleName();
        return !isWhitelisted(name) && containsInvalidChar(name);
    }

    /**
     * Names that may legitimately contain otherwise-invalid characters. Covers quoted identifiers wrapped in
     * backticks (Kotlin), single quotes or double quotes (Groovy).
     */
    protected boolean isWhitelisted(String name) {
        if (name.length() < 2) {
            return false;
        }
        char first = name.charAt(0);
        return (first == '`' || first == '\'' || first == '"') && name.charAt(name.length() - 1) == first;
    }

    protected static boolean containsInvalidChar(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (INVALID_CHARS.indexOf(name.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }
}
