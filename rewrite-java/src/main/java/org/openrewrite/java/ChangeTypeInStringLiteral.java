/*
 * Copyright 2020 the original author or authors.
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
    public String getInstanceNameSuffix() {
        // Defensively guard against null values when recipes are first classloaded. This
        // is a temporary workaround until releases of workers/CLI that include the defensive
        // coding in Recipe.
        //noinspection ConstantValue
        if (oldFullyQualifiedTypeName == null || newFullyQualifiedTypeName == null) {
            return getDisplayName();
        }

        String oldShort = oldFullyQualifiedTypeName.substring(oldFullyQualifiedTypeName.lastIndexOf('.') + 1);
        String newShort = newFullyQualifiedTypeName.substring(newFullyQualifiedTypeName.lastIndexOf('.') + 1);
        if (oldShort.equals(newShort)) {
            return String.format("`%s` to `%s`",
                    oldFullyQualifiedTypeName,
                    newFullyQualifiedTypeName);
        } else {
            return String.format("`%s` to `%s`",
                    oldShort, newShort);
        }
    }

    @Override
    public String getDescription() {
        return "Change a given type to another when used in a String literal.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof JavaSourceFile;
            }

            @Override
            public @Nullable Tree preVisit(@Nullable Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof J) {
                    return new JavaChangeTypeLiteralVisitor(oldFullyQualifiedTypeName, newFullyQualifiedTypeName).visit(tree, ctx, requireNonNull(getCursor().getParent()));
                }
                return tree;
            }
        };
    }

    private static class JavaChangeTypeLiteralVisitor extends JavaVisitor<ExecutionContext> {
        private final String newFullyQualifiedTypeName;
        private final Pattern stringLiteralPattern;

        private JavaChangeTypeLiteralVisitor(String oldFullyQualifiedTypeName, String newFullyQualifiedTypeName) {
            this.newFullyQualifiedTypeName = newFullyQualifiedTypeName;
            this.stringLiteralPattern = Pattern.compile("\\b" + oldFullyQualifiedTypeName + "\\b");
        }

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
    }
}
