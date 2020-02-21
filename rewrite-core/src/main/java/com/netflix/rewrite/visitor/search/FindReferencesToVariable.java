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
package com.netflix.rewrite.visitor.search;

import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.visitor.AstVisitor;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class FindReferencesToVariable extends AstVisitor<List<Tree>> {
    private final Tr.Ident variable;

    public FindReferencesToVariable(Tr.Ident variable) {
        this.variable = variable;
    }

    @Override
    public List<Tree> defaultTo(Tree t) {
        return emptyList();
    }

    @Override
    public List<Tree> visitAssign(Tr.Assign assign) {
        return hasReference(assign.getVariable()) ? singletonList(assign) : super.visitAssign(assign);
    }

    @Override
    public List<Tree> visitAssignOp(Tr.AssignOp assignOp) {
        return hasReference(assignOp.getVariable()) ? singletonList(assignOp) : super.visitAssignOp(assignOp);
    }

    @Override
    public List<Tree> visitUnary(Tr.Unary unary) {
        return hasReference(unary.getExpr()) ? singletonList(unary) : super.visitUnary(unary);
    }

    private boolean hasReference(Tree t) {
        return new HasReferenceToVariableInSubtree().visit(t);
    }

    private class HasReferenceToVariableInSubtree extends AstVisitor<Boolean> {
        @Override
        public Boolean defaultTo(Tree t) {
            return false;
        }

        @Override
        public Boolean visitIdentifier(Tr.Ident ident) {
            return ident.getIdent() == variable.getIdent() || super.visitIdentifier(ident);
        }
    }
}
