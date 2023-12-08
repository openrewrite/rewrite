/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.recipes;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateMovedPackageClassName extends Recipe {
    @Option(displayName = "The fully qualified className moved from",
        description = "The fully qualified className moved from a old package.",
        example = "org.openrewrite.java.cleanup.UnnecessaryCatch")
    String fullyQualifiedClassNameMovedFrom;

    @Option(displayName = "The fully qualified className moved to",
        description = "The fully qualified className moved to a new package.",
        example = "org.openrewrite.staticanalysis.UnnecessaryCatch")
    String fullyQualifiedClassNameMovedTo;

    @Override
    public String getDisplayName() {
        return "Update moved package class name";
    }

    @Override
    public String getDescription() {
        return "When a class moved from package A to B, update the fully qualified className accordingly.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                if (fieldAccess.toString().equals(fullyQualifiedClassNameMovedFrom)) {
                    return TypeTree.build(fullyQualifiedClassNameMovedTo)
                        .withPrefix(fieldAccess.getPrefix());
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }

            @Override
            public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
                J.Import after = super.visitImport(_import, ctx);
                if (_import != after) {
                    maybeRemoveImport(fullyQualifiedClassNameMovedFrom);
                    maybeAddImport(fullyQualifiedClassNameMovedTo);
                    doAfterVisit(new org.openrewrite.java.OrderImports(true).getVisitor());
                }
                return after;
            }
        };
    }
}
