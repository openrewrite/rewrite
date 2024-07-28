/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.util.Calendar;
import java.util.Collections;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddLicenseHeader extends Recipe {
    /**
     * A method pattern that is used to find matching method declarations/invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "License text",
            description = "The license header text without the block comment. May contain ${CURRENT_YEAR} property.",
            example = "Copyright ${CURRENT_YEAR} the original author or authors...")
    String licenseText;

    @Override
    public String getDisplayName() {
        return "Add license header";
    }

    @Override
    public String getDescription() {
        return "Adds license headers to Java source files when missing. Does not override existing license headers.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    if (cu.getComments().isEmpty()) {
                        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}", null);
                        String formattedLicenseText = "\n * " + propertyPlaceholderHelper.replacePlaceholders(licenseText,
                                k -> {
                                    if ("CURRENT_YEAR".equals(k)) {
                                        return Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
                                    }
                                    return System.getProperty(k);
                                }).replace("\n", "\n * ") + "\n ";

                        cu = cu.withComments(Collections.singletonList(
                                new TextComment(true, formattedLicenseText, "\n", Markers.EMPTY)
                        ));
                    }
                    return super.visit(cu, ctx);
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
                // short circuit everything else
                return _import;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                // short circuit everything else
                return classDecl;
            }
        };
    }
}
