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
package org.openrewrite.java.refactor;

import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.search.FindReferencedTypes;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

/**
 * Deletes standalone statements. Does not include deletion of control statements present in for loops.
 */
public class DeleteStatement extends JavaRefactorVisitor {
    private final Statement statement;

    public DeleteStatement(Statement statement) {
        this.statement = statement;
    }

    @Override
    public J visitIf(J.If iff) {
        J.If i = refactor(iff, super::visitIf);

        if (statement.isScope(i.getThenPart())) {
            i = i.withThenPart(emptyBlock());
        } else if (i.getElsePart() != null && statement.isScope(i.getElsePart())) {
            i = i.withElsePart(i.getElsePart().withStatement(emptyBlock()));
        }

        return i;
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop) {
        return statement.isScope(forLoop.getBody()) ? forLoop.withBody(emptyBlock()) :
                super.visitForLoop(forLoop);
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forEachLoop) {
        return statement.isScope(forEachLoop.getBody()) ? forEachLoop.withBody(emptyBlock()) :
                super.visitForEachLoop(forEachLoop);
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop) {
        return statement.isScope(whileLoop.getBody()) ? whileLoop.withBody(emptyBlock()) :
                super.visitWhileLoop(whileLoop);
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop) {
        return statement.isScope(doWhileLoop.getBody()) ? doWhileLoop.withBody(emptyBlock()) :
                super.visitDoWhileLoop(doWhileLoop);
    }

    @Override
    public J visitBlock(J.Block<J> block) {
        J.Block<J> b = refactor(block, super::visitBlock);

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
        return new J.Block<>(randomId(), null, emptyList(), Formatting.EMPTY, "");
    }
}
