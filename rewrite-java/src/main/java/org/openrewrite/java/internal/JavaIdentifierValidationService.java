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
 * Validates that identifier names contain only characters that can appear in an identifier of the source language.
 * A common parser bug is to store raw source text (e.g. a type expression or a constructor call) into a
 * {@code J.Identifier} name; this service catches that.
 * <p>
 * The rule is an allow-list of characters, so it is specific to each language. This base implementation encodes the
 * Java rule ({@link Character#isJavaIdentifierPart}, which permits letters, digits, {@code _} and {@code $} plus the
 * corresponding Unicode categories). Other languages subclass and override {@link #isValidChar(char)} (and, where the
 * language quotes identifiers, {@link #isWhitelisted(String)} / {@link #isInvalid(J.Identifier)}).
 * <p>
 * Only the character set is validated, not start-of-identifier rules; an LST may legitimately carry synthesized names
 * that would not be legal at the start of a source identifier.
 */
public class JavaIdentifierValidationService implements IdentifierValidationService {

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
     * Whether the identifier's name contains a character that cannot appear in an identifier of this language.
     * Subclasses can override to account for language-specific quoting (e.g. a marker indicating the name was quoted).
     */
    protected boolean isInvalid(J.Identifier identifier) {
        String name = identifier.getSimpleName();
        if (name.isEmpty() || isWhitelisted(name)) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            if (!isValidChar(name.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code c} may appear anywhere in a bare (unquoted) identifier of this language. The Java rule permits
     * letters, digits, {@code _}, {@code $} and the Unicode categories recognized by {@link Character#isJavaIdentifierPart}.
     */
    protected boolean isValidChar(char c) {
        return Character.isJavaIdentifierPart(c);
    }

    /**
     * Names allowed despite containing otherwise-invalid characters:
     * <ul>
     *     <li>{@code *} — wildcard imports (e.g. {@code import a.b.*}) represent the wildcard as a {@code J.Identifier}.</li>
     *     <li>Names beginning with {@code <} — compiler-synthesized names (e.g. {@code <init>}, Kotlin {@code <get>}/{@code <destruct>},
     *     C# {@code <Main>$}) follow this convention; no source identifier in these languages starts with {@code <}.</li>
     * </ul>
     */
    protected boolean isWhitelisted(String name) {
        return "*".equals(name) || name.charAt(0) == '<';
    }
}
