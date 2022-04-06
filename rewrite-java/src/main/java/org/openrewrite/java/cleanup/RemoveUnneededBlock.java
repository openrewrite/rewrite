/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

@Incubating(since = "7.21.0")
public class RemoveUnneededBlock extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove unneeded block";
    }

    @Override
    public String getDescription() {
        return "Flatten blocks into inline statements when possible";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new RemoveUnneededBlockStatementVisitor();
    }

    static class RemoveUnneededBlockStatementVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
            // Determine the first enclosing NewClass or ClassDeclaration statement
            J.NewClass newClass = getCursor().firstEnclosing(J.NewClass.class);
            J.ClassDeclaration classDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
            // Determine the direct parent
            J directParent = getCursor().dropParentUntil(J.class::isInstance).getValue();

            J.Block bl = super.visitBlock(block, executionContext);

            if (classDeclaration == directParent || newClass == directParent) {
                // If the direct parent is an initializer block or a static block, skip it
                return bl;
            }

            // Else perform the flattening on this block.
            return maybeAutoFormat(bl, block.withStatements(ListUtils.flatMap(bl.getStatements(), stmt -> {
                if (!(stmt instanceof J.Block)) {
                    return stmt;
                }
                J.Block nested = (J.Block) stmt;
                return nested.getStatements();
            })), executionContext, getCursor().getParentOrThrow());
        }
    }
}
