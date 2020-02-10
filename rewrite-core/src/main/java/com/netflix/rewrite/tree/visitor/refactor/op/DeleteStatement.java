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
package com.netflix.rewrite.tree.visitor.refactor.op;

import com.netflix.rewrite.tree.Formatting;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.tree.visitor.ReferencedTypesVisitor;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.ScopedRefactorVisitor;

import java.util.List;
import java.util.UUID;

import static com.netflix.rewrite.tree.Tr.randomId;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Deletes standalone statements. Does not include deletion of control statements present in for loops.
 */
public class DeleteStatement extends ScopedRefactorVisitor {
    public DeleteStatement(UUID scope) {
        super(scope);
    }

    @Override
    protected String getRuleName() {
        return "delete-statement";
    }

    @Override
    public List<AstTransform> visitIf(Tr.If iff) {
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
    public List<AstTransform> visitForLoop(Tr.ForLoop forLoop) {
        List<AstTransform> changes = super.visitForLoop(forLoop);
        if (forLoop.getBody().getId().equals(scope)) {
            changes.addAll(transform(forLoop, t -> t.withBody(emptyBlock())));
        }
        return changes;
    }

    @Override
    public List<AstTransform> visitForEachLoop(Tr.ForEachLoop forEachLoop) {
        List<AstTransform> changes = super.visitForEachLoop(forEachLoop);
        if (forEachLoop.getBody().getId().equals(scope)) {
            changes.addAll(transform(forEachLoop, t -> t.withBody(emptyBlock())));
        }
        return changes;
    }

    @Override
    public List<AstTransform> visitWhileLoop(Tr.WhileLoop whileLoop) {
        List<AstTransform> changes = super.visitWhileLoop(whileLoop);
        if (whileLoop.getBody().getId().equals(scope)) {
            changes.addAll(transform(whileLoop, t -> t.withBody(emptyBlock())));
        }
        return changes;
    }

    @Override
    public List<AstTransform> visitDoWhileLoop(Tr.DoWhileLoop doWhileLoop) {
        List<AstTransform> changes = super.visitDoWhileLoop(doWhileLoop);
        if (doWhileLoop.getBody().getId().equals(scope)) {
            changes.addAll(transform(doWhileLoop, t -> t.withBody(emptyBlock())));
        }
        return changes;
    }

    @Override
    public List<AstTransform> visitBlock(Tr.Block<Tree> block) {
        List<AstTransform> changes = super.visitBlock(block);
        if (block.getStatements().stream().anyMatch(s -> s.getId().equals(scope))) {
            changes.addAll(transform(block, t -> t.withStatements(t.getStatements().stream()
                    .filter(s -> !s.getId().equals(scope))
                    .collect(toList()))));
        }
        return changes;
    }

    @Override
    public List<AstTransform> visit(Tree tree) {
        if(tree != null && tree.getId().equals(scope)) {
            new ReferencedTypesVisitor().visit(tree).forEach(this::maybeRemoveImport);
        }
        return super.visit(tree);
    }

    private Tr.Block<Tree> emptyBlock() {
        return new Tr.Block<>(randomId(), null, emptyList(), Formatting.EMPTY, "");
    }
}
