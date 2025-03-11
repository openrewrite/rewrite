/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceStringLiteralValue extends Recipe {

    @Option(displayName = "Old literal `String` value",
            description = "The `String` value to replace.",
            example = "apple")
    String oldLiteralValue;

    @Option(displayName = "New literal `String` value",
            description = "The `String` value to replace with.",
            example = "orange")
    String newLiteralValue;

    @Override
    public String getDisplayName() {
        return "Replace `String` literal";
    }

    @Override
    public String getDescription() {
        return "Replace the value of a complete `String` literal.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                J.Literal lit = super.visitLiteral(literal, ctx);
                if (lit.getType() == JavaType.Primitive.String &&
                    oldLiteralValue.equals(lit.getValue())) {
                    return lit
                            .withValue(newLiteralValue)
                            .withValueSource('"' + newLiteralValue + '"');
                }
                return lit;
            }
        };
    }

}
