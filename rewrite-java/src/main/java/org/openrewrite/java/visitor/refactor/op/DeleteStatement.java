/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.visitor.refactor.op;

import org.openrewrite.java.tree.Formatting;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.Tree;
import org.openrewrite.java.visitor.ReferencedTypesVisitor;
import org.openrewrite.java.visitor.refactor.AstTransform;
import org.openrewrite.java.visitor.refactor.ScopedRefactorVisitor;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.java.tree.J.randomId;

/**
 * Deletes standalone statements. Does not include deletion of control statements present in for loops.
 */
public class DeleteStatement extends ScopedRefactorVisitor {
    public DeleteStatement(Statement scope) {
        super(scope.getId());
    }

    @Override
    public String getRuleName() {
        return "core.DeleteStatement";
    }

    @Override
    public List<AstTransform> visitIf(J.If iff) {
        List<AstTransform> changes = super.visitIf(iff);
        if (iff.getThenPart().getId().equals(scope)) {
            changes.addAll(transform(iff, t -> t.withThenPart(emptyBlock())));
        } else if (iff.getElsePart() != null && iff.getElsePart().getId().equals(scope)) {
            changes.addAll(transform(iff, t ->
                    t.getElsePart() == null ?
                            t :
                            t.withElsePart(t.getElsePart().withStatement(emptyBlock()))
            ));
        }
        return changes;
    }

    @Override
    public List<AstTransform> visitForLoop(J.ForLoop forLoop) {
        return maybeTransform(forLoop,
                forLoop.getBody().getId().equals(scope),
                super::visitForLoop,
                t -> t.withBody(emptyBlock())
        );
    }

    @Override
    public List<AstTransform> visitForEachLoop(J.ForEachLoop forEachLoop) {
        return maybeTransform(forEachLoop,
                forEachLoop.getBody().getId().equals(scope),
                super::visitForEachLoop,
                t -> t.withBody(emptyBlock())
        );
    }

    @Override
    public List<AstTransform> visitWhileLoop(J.WhileLoop whileLoop) {
        return maybeTransform(whileLoop,
                whileLoop.getBody().getId().equals(scope),
                super::visitWhileLoop,
                t -> t.withBody(emptyBlock())
        );
    }

    @Override
    public List<AstTransform> visitDoWhileLoop(J.DoWhileLoop doWhileLoop) {
        return maybeTransform(doWhileLoop,
                doWhileLoop.getBody().getId().equals(scope),
                super::visitDoWhileLoop,
                t -> t.withBody(emptyBlock())
        );
    }

    @Override
    public List<AstTransform> visitBlock(J.Block<Tree> block) {
        return maybeTransform(block,
                block.getStatements().stream().anyMatch(s -> s.getId().equals(scope)),
                super::visitBlock,
                t -> t.withStatements(t.getStatements().stream()
                        .filter(s -> !s.getId().equals(scope))
                        .collect(toList()))
        );
    }

    @Override
    public List<AstTransform> visit(Tree tree) {
        if (tree != null && tree.getId().equals(scope)) {
            new ReferencedTypesVisitor().visit(tree).forEach(this::maybeRemoveImport);
        }
        return super.visit(tree);
    }

    private J.Block<Tree> emptyBlock() {
        return new J.Block<>(randomId(), null, emptyList(), Formatting.EMPTY, "");
    }
}
