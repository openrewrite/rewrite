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
package com.netflix.rewrite.tree.visitor.refactor;

import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.Statement;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.tree.Type;
import com.netflix.rewrite.tree.visitor.CursorAstVisitor;
import com.netflix.rewrite.tree.visitor.refactor.op.AddImport;
import com.netflix.rewrite.tree.visitor.refactor.op.DeleteStatement;
import com.netflix.rewrite.tree.visitor.refactor.op.RemoveImport;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class RefactorVisitor extends CursorAstVisitor<List<AstTransform>> {
    private final ThreadLocal<List<RefactorVisitor>> andThen = new ThreadLocal<>();

    private Formatter formatter;

    public RefactorVisitor() {
        andThen.set(new ArrayList<>());
    }

    @Override
    public List<AstTransform> visitCompilationUnit(Tr.CompilationUnit cu) {
        formatter = new Formatter(cu);
        andThen.get().clear();
        return super.visitCompilationUnit(cu);
    }

    /**
     * Used to build up pipelines of RefactorVisitors.
     *
     * @return Other visitors that are run after all transformations from this visitor have taken place.
     */
    public Iterable<RefactorVisitor> andThen() {
        return andThen.get();
    }

    protected Formatter formatter() {
        return formatter;
    }

    protected List<AstTransform> maybeTransform(boolean shouldTransform,
                                                List<AstTransform> callSuper,
                                                List<AstTransform> transform) {
        if (shouldTransform) {
            callSuper.addAll(transform);
        }
        return callSuper;
    }

    protected void maybeAddImport(@Nullable Type.Class clazz) {
        if (clazz != null) {
            maybeAddImport(clazz.getFullyQualifiedName());
        }
    }

    protected void maybeAddImport(String fullyQualifiedName) {
        AddImport op = new AddImport(fullyQualifiedName, null, true);
        if (!andThen.get().contains(op)) {
            andThen.get().add(op);
        }
    }

    protected void maybeRemoveImport(@Nullable Type.Class clazz) {
        if (clazz != null) {
            maybeRemoveImport(clazz.getFullyQualifiedName());
        }
    }

    protected void maybeRemoveImport(String fullyQualifiedName) {
        RemoveImport op = new RemoveImport(fullyQualifiedName);
        if (!andThen.get().contains(op)) {
            andThen.get().add(op);
        }
    }

    protected void deleteStatement(Statement statement) {
        andThen.get().add(new DeleteStatement(statement.getId()));
    }

    protected void andThen(RefactorVisitor visitor) {
        andThen.get().add(visitor);
    }

    @Override
    public List<AstTransform> defaultTo(Tree t) {
        return new ArrayList<>();
    }

    @Nullable
    public String getRuleName() {
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <U extends Tree> List<AstTransform> transform(U target, Function<U, U> mutation) {
        List<AstTransform> changes = new ArrayList<>(1);
        changes.add(new AstTransform(target.getId(), getRuleName(), (Class<Tree>) target.getClass(), t -> mutation.apply((U) t)));
        return changes;
    }
}
