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
import org.openrewrite.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePackageInStringLiteral extends Recipe {
    @Option(displayName = "Old package name",
            description = "The package name to replace.",
            example = "com.yourorg.foo")
    String oldPackageName;

    @Option(displayName = "New package name",
            description = "New package name to replace the old package name with.",
            example = "com.yourorg.bar")
    String newPackageName;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldPackageName, newPackageName);
    }

    @Override
    public String getDisplayName() {
        return "Rename package name in String literals";
    }

    @Override
    public String getDescription() {
        return "A recipe that will rename a package name in String literals.";
    }

    @Override
    public Validated<Object> validate() {
        return Validated.none()
                .and(Validated.notBlank("oldPackageName", oldPackageName))
                .and(Validated.required("newPackageName", newPackageName));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern stringLiteralPattern = Pattern.compile("\\b" + oldPackageName + "\\b");
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                J.Literal lit = literal;
                if (literal.getType() == JavaType.Primitive.String && lit.getValue() != null) {
                    Matcher matcher = stringLiteralPattern.matcher((String) lit.getValue());
                    if (matcher.find()) {
                        lit = lit.withValue(matcher.replaceAll(newPackageName))
                                .withValueSource(stringLiteralPattern.matcher(lit.getValueSource()).replaceAll(newPackageName));
                    }
                }
                return super.visitLiteral(lit, ctx);
            }
        };
    }
}
