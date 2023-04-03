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
import org.openrewrite.java.tree.Statement;

@Incubating(since = "7.21.0")
public class RemoveUnneededBlock extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove unneeded block";
    }

    @Override
    public String getDescription() {
        return "Flatten blocks into inline statements when possible.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new RemoveUnneededBlockStatementVisitor();
    }

    static class RemoveUnneededBlockStatementVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
            J.Block bl = super.visitBlock(block, ctx);
            J directParent = getCursor().getParentTreeCursor().getValue();
            if (directParent instanceof J.NewClass || directParent instanceof J.ClassDeclaration) {
                // If the direct parent is an initializer block or a static block, skip it
                return bl;
            } else if (bl.getStatements().isEmpty()) {
                // Removal handled by `EmptyBlock`
                return bl;
            }

            // Else perform the flattening on this block.
            Statement lastStatement = bl.getStatements().get(bl.getStatements().size() - 1);
            J.Block flattened = bl.withStatements(ListUtils.flatMap(bl.getStatements(), (i, stmt) -> {
                if (!(stmt instanceof J.Block) || ((J.Block) stmt).getStatements().stream().anyMatch(s -> s instanceof J.VariableDeclarations)) {
                    // blocks are relevant for scoping, so don't flatten them if they contain variable declarations
                    return stmt;
                }
                J.Block nested = (J.Block) stmt;

                return ListUtils.map(nested.getStatements(), (j, inlinedStmt) -> {
                    if (j == 0) {
                        inlinedStmt = inlinedStmt.withPrefix(inlinedStmt.getPrefix()
                                .withComments(ListUtils.concat(nested.getComments(), inlinedStmt.getComments())));
                    }
                    return autoFormat(inlinedStmt, ctx, getCursor());
                });
            }));

            if (flattened == bl) {
                return bl;
            } else if (lastStatement instanceof J.Block) {
                flattened = flattened.withEnd(flattened.getEnd()
                        .withComments(ListUtils.concat(((J.Block) lastStatement).getEnd().getComments(), flattened.getEnd().getComments())));
            }
            return flattened;
        }
    }
}
