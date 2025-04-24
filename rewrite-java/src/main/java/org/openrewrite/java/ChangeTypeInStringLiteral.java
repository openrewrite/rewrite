/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTypeInStringLiteral extends Recipe {

    @Option(displayName = "Old fully-qualified type name",
            description = "Fully-qualified class name of the original type.",
            example = "org.junit.Assume")
    String oldFullyQualifiedTypeName;

    @Option(displayName = "New fully-qualified type name",
            description = "Fully-qualified class name of the replacement type, or the name of a primitive such as \"int\". The `OuterClassName$NestedClassName` naming convention should be used for nested classes.",
            example = "org.junit.jupiter.api.Assumptions")
    String newFullyQualifiedTypeName;

    @Override
    public String getDisplayName() {
        return "Change type in String literals";
    }


    @Override
    public Validated<Object> validate() {
        return Validated.none()
                .and(Validated.notBlank("oldPackageName", oldFullyQualifiedTypeName))
                .and(Validated.required("newPackageName", newFullyQualifiedTypeName));
    }

    @Override
    public String getDescription() {
        return "Change a given type to another when used in a String literal.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final Pattern stringLiteralPattern = Pattern.compile("\\b" + oldFullyQualifiedTypeName + "\\b");
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                J.Literal lit = literal;
                if (literal.getType() == JavaType.Primitive.String && lit.getValue() != null) {
                    Matcher matcher = stringLiteralPattern.matcher((String) lit.getValue());
                    if (matcher.find()) {
                        lit = lit.withValue(matcher.replaceAll(newFullyQualifiedTypeName))
                                .withValueSource(stringLiteralPattern.matcher(lit.getValueSource()).replaceAll(newFullyQualifiedTypeName));
                    }
                }
                return super.visitLiteral(lit, ctx);
            }
        };
    }
}
