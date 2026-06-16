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
package org.openrewrite.scala.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.IdentifierValidationService;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaIsoVisitor;

public class ScalaIdentifierValidationService implements IdentifierValidationService {

    /**
     * Characters that can never appear in a Scala identifier: whitespace, brackets, parentheses, braces,
     * comma, semicolon and double quote. Operator identifiers (e.g. {@code <=}, {@code ::}, {@code +}) are
     * intentionally not flagged, and backtick-quoted identifiers (which may contain any character) are
     * whitelisted separately.
     */
    private static final String INVALID_CHARS = " \t\r\n()[]{},;\"";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ScalaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                String name = identifier.getSimpleName();
                if (!isBacktickQuoted(name) && containsInvalidChar(name)) {
                    return identifier.withSimpleName("~~(invalid-identifier)~~>" + name + "<~~");
                }
                return identifier;
            }
        };
    }

    private static boolean isBacktickQuoted(String name) {
        return name.length() >= 2 && name.charAt(0) == '`' && name.charAt(name.length() - 1) == '`';
    }

    private static boolean containsInvalidChar(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (INVALID_CHARS.indexOf(name.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }
}
