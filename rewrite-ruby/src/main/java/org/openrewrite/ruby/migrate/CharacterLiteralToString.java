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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.ruby.RubyIsoVisitor;

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
                if (literal.getType() == JavaType.Primitive.String && literal.getValueSource() != null &&
                    literal.getValueSource().startsWith("?")) {
                    return literal.withType(JavaType.Primitive.String)
                            .withValueSource(String.format("'%s'", literal.getValueSource().substring(1)));
                }
                return literal;
            }
        };
    }
}
