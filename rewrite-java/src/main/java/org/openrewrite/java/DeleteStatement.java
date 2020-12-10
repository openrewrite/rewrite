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

import org.openrewrite.Formatting;
import org.openrewrite.marker.Markers;
import org.openrewrite.Tree;
import org.openrewrite.java.search.FindReferencedTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

/**
 * Deletes standalone statements. Does not include deletion of control statements present in for loops.
 */
public class DeleteStatement {
    public static class Scoped extends JavaIsoRefactorVisitor {
        private final Statement statement;

        public Scoped(Statement statement) {
            this.statement = statement;
        }

        @Override
        public J.If visitIf(J.If iff) {
            J.If i = super.visitIf(iff);

            if (statement.isScope(i.getThenPart())) {
                i = i.withThenPart(emptyBlock());
            } else if (i.getElsePart() != null && statement.isScope(i.getElsePart())) {
                i = i.withElsePart(i.getElsePart().withStatement(emptyBlock()));
            }

            return i;
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop forLoop) {
            return statement.isScope(forLoop.getBody()) ? forLoop.withBody(emptyBlock()) :
                    super.visitForLoop(forLoop);
        }

        @Override
        public J.ForEachLoop visitForEachLoop(J.ForEachLoop forEachLoop) {
            return statement.isScope(forEachLoop.getBody()) ? forEachLoop.withBody(emptyBlock()) :
                    super.visitForEachLoop(forEachLoop);
        }

        @Override
        public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop) {
            return statement.isScope(whileLoop.getBody()) ? whileLoop.withBody(emptyBlock()) :
                    super.visitWhileLoop(whileLoop);
        }

        @Override
        public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop) {
            return statement.isScope(doWhileLoop.getBody()) ? doWhileLoop.withBody(emptyBlock()) :
                    super.visitDoWhileLoop(doWhileLoop);
        }

        @Override
        public J.Block<J> visitBlock(J.Block<J> block) {
            J.Block<J> b = super.visitBlock(block);

            if (block.getStatements().stream().anyMatch(statement::isScope)) {
                b = b.withStatements(b.getStatements().stream()
                        .filter(s -> !statement.isScope(s))
                        .collect(toList()));
            }

            return b;
        }

        @Override
        public J visitTree(Tree tree) {
            if (statement.isScope(tree)) {
                new FindReferencedTypes().visit(tree).forEach(this::maybeRemoveImport);
            }
            return super.visitTree(tree);
        }

        private J.Block<J> emptyBlock() {
            return new J.Block<>(randomId(), null, emptyList(), emptyList(), Formatting.EMPTY,
                    Markers.EMPTY, new J.Block.End(randomId(), emptyList(), format(""), Markers.EMPTY));
        }
    }
}
