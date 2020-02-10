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
import com.netflix.rewrite.refactor.Refactor;
import com.netflix.rewrite.tree.Statement;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.tree.Type;
import com.netflix.rewrite.tree.visitor.CursorAstVisitor;
import com.netflix.rewrite.tree.visitor.refactor.op.AddImport;
import com.netflix.rewrite.tree.visitor.refactor.op.DeleteStatement;
import com.netflix.rewrite.tree.visitor.refactor.op.RemoveImport;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public abstract class RefactorVisitor extends CursorAstVisitor<List<AstTransform>> {
    private final ThreadLocal<List<RefactorVisitor>> andThen = new ThreadLocal<>();

    public RefactorVisitor() {
        andThen.set(new ArrayList<>());
    }

    @Override
    public List<AstTransform> visitCompilationUnit(Tr.CompilationUnit cu) {
        andThen.get().clear();
        return super.visitCompilationUnit(cu);
    }

    /**
     * Used to build up pipelines of RefactorVisitors.
     * @return Other visitors that are run after all transformations from this visitor have taken place.
     */
    public Iterable<RefactorVisitor> andThen() {
        return andThen.get();
    }

    protected void maybeAddImport(@Nullable Type.Class clazz) {
        if(clazz != null) {
            maybeAddImport(clazz.getFullyQualifiedName());
        }
    }

    protected void maybeAddImport(String fullyQualifiedName) {
        AddImport op = new AddImport(fullyQualifiedName, null, true);
        if(!andThen.get().contains(op)) {
            andThen.get().add(op);
        }
    }

    protected void maybeRemoveImport(@Nullable Type.Class clazz) {
        if(clazz != null) {
            maybeRemoveImport(clazz.getFullyQualifiedName());
        }
    }

    protected void maybeRemoveImport(String fullyQualifiedName) {
        RemoveImport op = new RemoveImport(fullyQualifiedName);
        if(!andThen.get().contains(op)) {
            andThen.get().add(op);
        }
    }

    protected void deleteStatement(Statement statement) {
        andThen.get().add(new DeleteStatement(statement.getId()));
    }

    @Override
    public List<AstTransform> defaultTo(Tree t) {
        return emptyList();
    }

    protected abstract String getRuleName();

    protected <U extends Tree> List<AstTransform> transform(U target, Function<U, U> mutation) {
        return transform(target, getRuleName(), mutation);
    }

    @SuppressWarnings("unchecked")
    protected <U extends Tree> List<AstTransform> transform(U target, String name, Function<U, U> mutation) {
        return singletonList(new AstTransform(target.getId(), name, (Class<Tree>) target.getClass(), t -> mutation.apply((U) t)));
    }
}
