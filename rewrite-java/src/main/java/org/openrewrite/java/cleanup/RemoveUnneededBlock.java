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
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveUnneededBlockStatementVisitor();
    }

    static class RemoveUnneededBlockStatementVisitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
            J.Block bl = (J.Block) super.visitBlock(block, ctx);
            J directParent = getCursor().getParentTreeCursor().getValue();
            if (directParent instanceof J.NewClass || directParent instanceof J.ClassDeclaration) {
                // If the direct parent is an initializer block or a static block, skip it
                return bl;
            }

            return maybeInlineBlock(bl, ctx);
        }

        private J.Block maybeInlineBlock(J.Block block, ExecutionContext ctx) {
            List<Statement> statements = block.getStatements();
            if (statements.isEmpty()) {
                // Removal handled by `EmptyBlock`
                return block;
            }

            // Else perform the flattening on this block.
            Statement lastStatement = statements.get(statements.size() - 1);
            J.Block flattened = block.withStatements(ListUtils.flatMap(statements, (i, stmt) -> {
                J.Block nested;
                if (stmt instanceof J.Try) {
                    J.Try _try = (J.Try) stmt;
                    if (_try.getResources() != null || !_try.getCatches().isEmpty() || _try.getFinally() == null || !_try.getFinally().getStatements().isEmpty()) {
                        return stmt;
                    }
                    nested = _try.getBody();
                } else if (stmt instanceof J.Block) {
                    nested = (J.Block) stmt;
                } else {
                    return stmt;
                }

                // blocks are relevant for scoping, so don't flatten them if they contain variable declarations
                if (i < statements.size() - 1 && nested.getStatements().stream().anyMatch(s -> s instanceof J.VariableDeclarations)) {
                    return stmt;
                }

                return ListUtils.map(nested.getStatements(), (j, inlinedStmt) -> {
                    if (j == 0) {
                        inlinedStmt = inlinedStmt.withPrefix(inlinedStmt.getPrefix()
                                .withComments(ListUtils.concatAll(nested.getComments(), inlinedStmt.getComments())));
                    }
                    return autoFormat(inlinedStmt, ctx, getCursor());
                });
            }));

            if (flattened == block) {
                return block;
            } else if (lastStatement instanceof J.Block) {
                flattened = flattened.withEnd(flattened.getEnd()
                        .withComments(ListUtils.concatAll(((J.Block) lastStatement).getEnd().getComments(), flattened.getEnd().getComments())));
            }
            return flattened;
        }
    }
}
