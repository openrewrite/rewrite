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
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import com.netflix.rewrite.tree.visitor.refactor.TransformVisitor;
import com.netflix.rewrite.tree.visitor.refactor.op.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.netflix.rewrite.tree.Formatting.format;
import static com.netflix.rewrite.tree.Tr.randomId;

@NonNullApi
public class Refactor {
    private final Tr.CompilationUnit original;

    private final List<RefactorVisitor> ops = new ArrayList<>();

    public Refactor(Tr.CompilationUnit original) {
        this.original = original;
    }

    // -------------
    // Custom refactoring visitors
    // -------------

    public Refactor run(Iterable<RefactorVisitor> visitors) {
        visitors.forEach(ops::add);
        return this;
    }

    public Refactor run(RefactorVisitor visitor) {
        ops.add(visitor);
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
        ops.add(new AddImport(clazz, staticMethod, onlyIfReferenced));
        return this;
    }

    public Refactor removeImport(String clazz) {
        ops.add(new RemoveImport(clazz));
        return this;
    }

    // -------------
    // Class Declaration Refactoring
    // -------------

    public Refactor addField(Tr.ClassDecl target, String clazz, String name) {
        return addField(target, clazz, name, null);
    }

    public Refactor addField(Tr.ClassDecl target, String clazz, String name, @Nullable String init) {
        ops.add(new AddField(target.getId(), Collections.singletonList(new Tr.Modifier.Private(randomId(), format("", " "))), clazz, name, init));
        ops.add(new AddImport(clazz, null, false));
        return this;
    }

    // -------------
    // Field Refactoring
    // -------------

    public Refactor changeFieldType(Iterable<Tr.VariableDecls> targets, String toType) {
        targets.forEach(t -> changeFieldType(t, toType));
        return this;
    }

    public Refactor changeFieldType(Tr.VariableDecls target, String toType) {
        ops.add(new ChangeFieldType(target.getId(), toType));
        return this;
    }

    public Refactor changeFieldName(Type.Class classType, String hasName, String toName) {
        ops.add(new ChangeFieldName(classType, hasName, toName));
        return this;
    }

    public Refactor deleteStatement(Iterable<Statement> targets) {
        targets.forEach(this::deleteStatement);
        return this;
    }

    public Refactor deleteStatement(Statement statement) {
        ops.add(new DeleteStatement(statement.getId()));
        return this;
    }

    // -------------
    // Method Refactoring
    // -------------

    public Refactor changeMethodName(Iterable<Tr.MethodInvocation> targets, String name) {
        targets.forEach(t -> changeMethodName(t, name));
        return this;
    }

    public Refactor changeMethodName(Tr.MethodInvocation target, String toName) {
        ops.add(new ChangeMethodName(target.getId(), toName));
        return this;
    }

    public Refactor changeMethodTargetToStatic(Iterable<Tr.MethodInvocation> targets, String toClass) {
        targets.forEach(t -> changeMethodTargetToStatic(t, toClass));
        return this;
    }

    public Refactor changeMethodTargetToStatic(Tr.MethodInvocation target, String toClass) {
        ops.add(new ChangeMethodTargetToStatic(target.getId(), toClass));
        return this;
    }

    public Refactor changeMethodTarget(Iterable<Tr.MethodInvocation> targets, Tr.VariableDecls.NamedVar namedVar) {
        targets.forEach(t -> changeMethodTarget(t, namedVar));
        return this;
    }

    public Refactor changeMethodTarget(Tr.MethodInvocation target, Tr.VariableDecls.NamedVar namedVar) {
        return changeMethodTarget(target, namedVar.getSimpleName(), TypeUtils.asClass(namedVar.getType()));
    }

    public Refactor changeMethodTarget(Iterable<Tr.MethodInvocation> targets, String namedVar, @Nullable Type.Class clazz) {
        targets.forEach(t -> changeMethodTarget(t, namedVar, clazz));
        return this;
    }

    public Refactor changeMethodTarget(Tr.MethodInvocation target, String namedVar, @Nullable Type.Class clazz) {
        ops.add(new ChangeMethodTargetToVariable(target.getId(), namedVar, clazz));
        return this;
    }

    public Refactor insertArgument(Iterable<Tr.MethodInvocation> targets, int pos, String source) {
        targets.forEach(t -> insertArgument(t, pos, source));
        return this;
    }

    public Refactor insertArgument(Tr.MethodInvocation target, int pos, String source) {
        ops.add(new InsertMethodArgument(target.getId(), pos, source));
        return this;
    }

    public Refactor deleteArgument(Iterable<Tr.MethodInvocation> targets, int pos) {
        targets.forEach(t -> deleteArgument(t, pos));
        return this;
    }

    public Refactor deleteArgument(Tr.MethodInvocation target, int pos) {
        ops.add(new DeleteMethodArgument(target.getId(), pos));
        return this;
    }

    public ReorderMethodArguments reorderArguments(Tr.MethodInvocation target, String... byArgumentNames) {
        ReorderMethodArguments reorder = new ReorderMethodArguments(target.getId(), byArgumentNames);
        ops.add(reorder);
        return reorder;
    }

    // -------------
    // Expression Refactoring
    // -------------

    public Refactor changeLiteral(Iterable<Expression> targets, Function<Object, Object> transform) {
        targets.forEach(t -> changeLiteral(t, transform));
        return this;
    }

    public Refactor changeLiteral(Expression target, Function<Object, Object> transform) {
        ops.add(new ChangeLiteral(target.getId(), transform));
        return this;
    }

    public Refactor changeType(String from, String to) {
        ops.add(new ChangeType(from, to));
        return this;
    }

    // TODO rework this to make it more useful (and don't forget to recurse through pipelined visitors!)
//    public Map<String, Long> stats() {
//        Map<String, Long> stats = new HashMap<>();
//
//        Tr.CompilationUnit acc = original;
//        for (RefactorOperation op : ops) {
//            var target = new RetrieveTreeVisitor(op.getId()).visit(acc);
//            List<AstTransform> transformations = new ArrayList<>(op.getVisitor().visit(target));
//            acc = (Tr.CompilationUnit) new TransformVisitor(transformations).visit(acc);
//            transformations.stream()
//                    .collect(Collectors.groupingBy(AstTransform::getName, counting()))
//                    .forEach((name, count) -> stats.merge(name, count, Long::sum));
//        }
//
//        return stats;
//    }

    /**
     * @return Transformed version of the AST after changes are applied
     */
    public Tr.CompilationUnit fix() {
        Tr.CompilationUnit acc = original;
        for (RefactorVisitor visitor : ops) {
            acc = transformRecursive(acc, visitor);
        }
        return acc;
    }

    private Tr.CompilationUnit transformRecursive(Tr.CompilationUnit acc, RefactorVisitor visitor) {
        // by transforming the AST for each op, we allow for the possibility of overlapping changes
        acc = (Tr.CompilationUnit) new TransformVisitor(visitor.visit(acc)).visit(acc);
        for (RefactorVisitor vis : visitor.andThen()) {
            acc = transformRecursive(acc, vis);
        }
        return acc;
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
}
