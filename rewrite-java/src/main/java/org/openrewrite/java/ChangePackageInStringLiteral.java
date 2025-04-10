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
import org.openrewrite.java.trait.Literal;
import org.openrewrite.java.trait.Traits;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

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
        TreeVisitor<?, ExecutionContext> condition = new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(@Nullable Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    if (Traits.literal().asVisitor((Literal lit, AtomicBoolean bool) -> {
                        if (!Boolean.TRUE.equals(bool.get())) {
                            String string = lit.getString();
                            if (string != null && string.contains(oldPackageName)) {
                                bool.set(true);
                            }
                        }
                        return lit.getTree();
                    }).reduce(cu, new AtomicBoolean(false), getCursor().getParentTreeCursor()).get()) {
                        return SearchResult.found(cu);
                    }
                }
                return tree;
            }
        };

        return Preconditions.check(condition, new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof JavaSourceFile;
            }

            @Override
            public @Nullable Tree preVisit(@Nullable Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof JavaSourceFile) {
                    return new JavaChangePackageLiteralVisitor(oldPackageName, newPackageName).visit(tree, ctx, requireNonNull(getCursor().getParent()));
                }
                return tree;
            }
        });
    }

    private static class JavaChangePackageLiteralVisitor extends JavaVisitor<ExecutionContext> {
        private final String newPackageName;
        private final Pattern stringLiteralPattern;

        private JavaChangePackageLiteralVisitor(String oldPackageName, String newPackageName) {
            this.newPackageName = newPackageName;
            this.stringLiteralPattern = Pattern.compile("\\b" + oldPackageName + "\\b");
        }

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
    }
}
