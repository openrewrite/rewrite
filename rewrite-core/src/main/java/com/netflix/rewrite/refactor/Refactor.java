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
package com.netflix.rewrite.refactor;

import com.netflix.rewrite.internal.lang.NonNullApi;
import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.*;
import com.netflix.rewrite.tree.visitor.RetrieveTreeVisitor;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.FormatVisitor;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import com.netflix.rewrite.tree.visitor.refactor.TransformVisitor;
import com.netflix.rewrite.tree.visitor.refactor.op.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.netflix.rewrite.tree.Formatting.format;
import static com.netflix.rewrite.tree.Tr.randomId;
import static java.util.stream.Collectors.counting;
import static java.util.stream.StreamSupport.stream;

@NonNullApi
public class Refactor {
    private final Tr.CompilationUnit original;

    private final List<RefactorOperation> ops = new ArrayList<>();

    public Refactor(Tr.CompilationUnit original) {
        this.original = original;
    }

    // -------------
    // Custom refactoring visitors
    // -------------

    public <T extends Tree> Refactor run(T t, RefactorVisitor<T> visitor) {
        addOp(t, visitor);
        return this;
    }

    public <T extends Tree> Refactor run(RefactorVisitor<T> visitor) {
        addOp(visitor);
        return this;
    }

    // -------------
    // Compilation Unit Refactoring
    // -------------

    public Refactor addImport(String clazz) {
        return addImport(clazz, null);
    }

    public Refactor addImport(String clazz, @Nullable String staticMethod) {
        return addImport(clazz, staticMethod, false);
    }

    public Refactor addImport(String clazz, @Nullable String staticMethod, boolean onlyIfReferenced) {
        addOp(new AddImport(clazz, staticMethod, onlyIfReferenced));
        return this;
    }

    public Refactor removeImport(String clazz) {
        addOp(new RemoveImport(clazz));
        return this;
    }

    // -------------
    // Class Declaration Refactoring
    // -------------

    public Refactor addField(Tr.ClassDecl target, String clazz, String name) {
        return addField(target, clazz, name, null);
    }

    public Refactor addField(Tr.ClassDecl target, String clazz, String name, @Nullable String init) {
        addOp(target, new AddField(Collections.singletonList(new Tr.Modifier.Private(randomId(), format("", " "))), clazz, name, init));
        addOp(new AddImport(clazz, null, false));
        return this;
    }

    // -------------
    // Field Refactoring
    // -------------

    public Refactor changeFieldType(Iterable<Tr.VariableDecls> targets, String toType) {
        for (Tr.VariableDecls target : targets) {
            addOp(target, new ChangeFieldType(toType));
            if (target.getTypeExpr() != null) {
                Type.Class asClass = TypeUtils.asClass(target.getTypeExpr().getType());
                if (asClass != null) {
                    removeImport(asClass.getFullyQualifiedName());
                }
            }
        }

        if (targets.iterator().hasNext()) {
            addImport(toType);
        }

        return this;
    }

    public Refactor changeFieldName(Iterable<Tr.VariableDecls> targets, String toName) {
        for (Tr.VariableDecls target : targets) {
            addOp(target, new ChangeFieldName(toName));
        }
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    public Refactor deleteField(Iterable<Tr.VariableDecls> targets) {
        stream(targets.spliterator(), false)
                .filter(variable -> original.cursor(variable) != null)
                .collect(Collectors.groupingBy(variable -> original.cursor(variable).enclosingClass()))
                .forEach((clazz, variables) -> {
                    addOp(clazz, new DeleteField(variables));
                    for (Tr.VariableDecls variable : variables) {
                        variable.getTypeAsClass().ifPresent(varType -> removeImport(varType.getFullyQualifiedName()));
                    }
                });
        return this;
    }

    // -------------
    // Method Refactoring
    // -------------

    public Refactor changeMethodName(Iterable<Tr.MethodInvocation> targets, String toName) {
        targets.forEach(t -> addOp(t, new ChangeMethodName(toName)));
        return this;
    }

    public Refactor changeMethodTargetToStatic(Iterable<Tr.MethodInvocation> targets, String toClass) {
        targets.forEach(t -> {
            addOp(t, new ChangeMethodTargetToStatic(toClass));
            if (t.getType() != null) {
                removeImport(t.getType().getDeclaringType().getFullyQualifiedName());
            }
        });

        if (targets.iterator().hasNext()) {
            addImport(toClass);
        }

        return this;
    }

    public Refactor changeMethodTarget(Iterable<Tr.MethodInvocation> targets, Tr.VariableDecls.NamedVar namedVar) {
        return changeMethodTarget(targets, namedVar.getSimpleName(), TypeUtils.asClass(namedVar.getType()));
    }

    public Refactor changeMethodTarget(Iterable<Tr.MethodInvocation> targets, String namedVar, @Nullable Type.Class clazz) {
        targets.forEach(t -> {
            addOp(t, new ChangeMethodTargetToVariable(namedVar, clazz));

            // if the original is a static method invocation, the import on it's type may no longer be needed
            if (t.getType() != null) {
                removeImport(t.getType().getDeclaringType().getFullyQualifiedName());
            }
        });

        return this;
    }

    public Refactor insertArgument(Iterable<Tr.MethodInvocation> targets, int pos, String source) {
        targets.forEach(t -> addOp(t, new InsertMethodArgument(pos, source)));
        return this;
    }

    public Refactor deleteArgument(Iterable<Tr.MethodInvocation> targets, int pos) {
        targets.forEach(t -> addOp(t, new DeleteMethodArgument(pos)));
        return this;
    }

    public ReorderMethodArguments reorderArguments(Tr.MethodInvocation target, String... byArgumentNames) {
        ReorderMethodArguments reorder = new ReorderMethodArguments(byArgumentNames, new String[0]);
        addOp(target, reorder);
        return reorder;
    }

    // -------------
    // Expression Refactoring
    // -------------

    public Refactor changeLiteral(Iterable<Expression> targets, Function<Object, Object> transform) {
        targets.forEach(t -> addOp(t, new ChangeLiteral(transform)));
        return this;
    }

    public Refactor changeType(String from, String to) {
        addOp(new ChangeType(from, to));
        addOp(new AddImport(to, null, true));
        addOp(new RemoveImport(from));
        return this;
    }

    public Map<String, Long> stats() {
        Map<String, Long> stats = new HashMap<>();

        Tr.CompilationUnit acc = original;
        for (RefactorOperation op : ops) {
            var target = new RetrieveTreeVisitor(op.getId()).visit(acc);
            List<AstTransform<?>> transformations = new ArrayList<>(op.getVisitor().visit(target));
            acc = (Tr.CompilationUnit) new TransformVisitor(transformations).visit(acc);
            transformations.stream()
                    .collect(Collectors.groupingBy(AstTransform::getName, counting()))
                    .forEach((name, count) -> stats.merge(name, count, Long::sum));
        }

        return stats;
    }

    /**
     * @return Transformed version of the AST after changes are applied
     */
    public Tr.CompilationUnit fix() {
        Tr.CompilationUnit acc = original;
        for (RefactorOperation op : ops) {
            var target = new RetrieveTreeVisitor(op.getId()).visit(acc);

            // by transforming the AST for each op, we allow for the possibility of overlapping changes
            List<AstTransform<?>> transformations = new ArrayList<>(op.getVisitor().visit(target));
            acc = (Tr.CompilationUnit) new TransformVisitor(transformations).visit(acc);
        }

        return (Tr.CompilationUnit) new TransformVisitor(new ArrayList<>(new FormatVisitor().visit(acc)))
                .visit(acc);
    }

    public String diff() {
        return diff(null);
    }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit
     */
    public String diff(@Nullable Path relativeTo) {
        return new InMemoryDiffEntry(Paths.get(original.getSourcePath()), relativeTo,
                original.print(), fix().print()).getDiff();
    }

    static class InMemoryDiffEntry extends DiffEntry {
        InMemoryRepository repo;

        InMemoryDiffEntry(Path filePath, @Nullable Path relativeTo, String oldSource, String newSource) {
            this.changeType = ChangeType.MODIFY;

            var relativePath = relativeTo == null ? filePath : relativeTo.relativize(filePath);
            this.oldPath = relativePath.toString();
            this.newPath = relativePath.toString();

            try {
                this.repo = new InMemoryRepository.Builder().build();

                var inserter = repo.getObjectDatabase().newInserter();
                oldId = inserter.insert(Constants.OBJ_BLOB, oldSource.getBytes()).abbreviate(40);
                newId = inserter.insert(Constants.OBJ_BLOB, newSource.getBytes()).abbreviate(40);
                inserter.flush();

                oldMode = FileMode.REGULAR_FILE;
                newMode = FileMode.REGULAR_FILE;
                repo.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        String getDiff() {
            if(oldId.equals(newId)) {
                return "";
            }

            var patch = new ByteArrayOutputStream();
            var formatter = new DiffFormatter(patch);
            formatter.setRepository(repo);
            try {
                formatter.format(this);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new String(patch.toByteArray());
        }
    }

    private void addOp(Tree target, RefactorVisitor<?> visitor) {
        ops.add(new RefactorOperation(target.getId(), visitor));
    }

    private void addOp(RefactorVisitor<?> visitor) {
        ops.add(new RefactorOperation(original.getId(), visitor));
    }

    @AllArgsConstructor
    private static class RefactorOperation {
        @Getter
        @Setter
        UUID id;

        @Getter
        @Setter
        RefactorVisitor<?> visitor;
    }
}
