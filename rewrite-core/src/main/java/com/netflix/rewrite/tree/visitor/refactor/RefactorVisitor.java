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
import com.netflix.rewrite.tree.*;
import com.netflix.rewrite.tree.visitor.CursorAstVisitor;
import com.netflix.rewrite.tree.visitor.refactor.op.AddImport;
import com.netflix.rewrite.tree.visitor.refactor.op.ChangeFieldName;
import com.netflix.rewrite.tree.visitor.refactor.op.DeleteStatement;
import com.netflix.rewrite.tree.visitor.refactor.op.RemoveImport;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class RefactorVisitor extends CursorAstVisitor<List<AstTransform>> {
    private final ThreadLocal<List<RefactorVisitor>> andThen = new ThreadLocal<>();

    private Formatter formatter;

    /**
     * For debugging purposes, when a RefactorVisitor isn't properly idempotent across multiple runs.
     */
    @Setter
    private int cycle = 0;

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

    protected <T extends Tree, U extends Tree> List<AstTransform> maybeTransform(T tree,
                                                                                 boolean shouldTransform,
                                                                                 Function<T, List<AstTransform>> callSuper,
                                                                                 Function<T, U> transformNestedElement,
                                                                                 BiFunction<U, Cursor, U> mutation) {
        List<AstTransform> changes = callSuper.apply(tree);
        if (shouldTransform) {
            changes.addAll(transform(transformNestedElement.apply(tree), mutation));
        }
        return changes;
    }

    protected <T extends Tree, U extends Tree> List<AstTransform> maybeTransform(T tree,
                                                                                 boolean shouldTransform,
                                                                                 Function<T, List<AstTransform>> callSuper,
                                                                                 Function<T, U> transformNestedElement,
                                                                                 Function<U, U> mutation) {
        List<AstTransform> changes = callSuper.apply(tree);
        if (shouldTransform) {
            changes.addAll(transform(transformNestedElement.apply(tree), mutation));
        }
        return changes;
    }

    protected <T extends Tree> List<AstTransform> maybeTransform(T tree,
                                                                 boolean shouldTransform,
                                                                 Function<T, List<AstTransform>> callSuper,
                                                                 Function<T, T> mutation) {
        List<AstTransform> changes = callSuper.apply(tree);
        if (shouldTransform) {
            changes.addAll(transform(tree, mutation));
        }
        return changes;
    }

    protected <T extends Tree> List<AstTransform> maybeTransform(T tree,
                                                                 boolean shouldTransform,
                                                                 Function<T, List<AstTransform>> callSuper,
                                                                 BiFunction<T, Cursor, T> mutation) {
        List<AstTransform> changes = callSuper.apply(tree);
        if (shouldTransform) {
            changes.addAll(transform(tree, mutation));
        }
        return changes;
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

    protected void changeFieldName(Type.Class classType, String hasName, String toName) {
        andThen.get().add(new ChangeFieldName(classType, hasName, toName));
    }

    protected void andThen(RefactorVisitor visitor) {
        andThen.get().add(visitor);
    }

    @Override
    public List<AstTransform> defaultTo(Tree t) {
        return new ArrayList<>();
    }

    public String getRuleName() {
        return "uncategorized";
    }

    /**
     * Determines whether this visitor can be run multiple times as a top-level rule.
     */
    public boolean isSingleRun() {
        return false;
    }

    @SuppressWarnings("unchecked")
    protected <U extends Tree> List<AstTransform> transform(U target, BiFunction<U, Cursor, U> mutation) {
        List<AstTransform> changes = new ArrayList<>(1);
        changes.add(new AstTransform(target.getId(), getRuleName(), (Class<Tree>) target.getClass(), (t, c) -> mutation.apply((U) t, c)));
        return changes;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Tree, U extends T> List<AstTransform> transform(Class<T> widenTo, U target, BiFunction<U, Cursor, T> mutation) {
        List<AstTransform> changes = new ArrayList<>(1);
        changes.add(new AstTransform(target.getId(), getRuleName(), (Class<Tree>) widenTo, (t, c) -> mutation.apply((U) t, c)));
        return changes;
    }

    @SuppressWarnings("unchecked")
    protected <U extends Tree> List<AstTransform> transform(U target, Function<U, U> mutation) {
        List<AstTransform> changes = new ArrayList<>(1);
        changes.add(new AstTransform(target.getId(), getRuleName(), (Class<Tree>) target.getClass(), (t, c) -> mutation.apply(((U) t))));
        return changes;
    }
}
