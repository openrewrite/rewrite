/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.DataTableExecutionContextView;
import org.openrewrite.DataTableStore;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.table.ChangedFilesDataTable;
import org.openrewrite.table.ChangedSymbolsDataTable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

/**
 * Phase 2 input producer: walks each Java source file that appears in
 * {@link ChangedFilesDataTable}, emitting one {@link ChangedSymbolsDataTable}
 * row per declared symbol (class, method, field, constructor).
 * <p>
 * v1 coarseness: every top-level or nested class, every method/constructor,
 * and every variable declaration in a changed file is considered "changed".
 * This is deliberately conservative — it avoids the need to compute pre/post
 * LST diffs. A false-positive changed symbol just means that the reachability
 * closure finds a few extra callers, which still produces far better test
 * selection than module-level granularity.
 * <p>
 * For {@code DELETED} files we still emit a coarse class-level row when the
 * LST is available; if the LST is absent (the file was removed from the
 * working tree and never parsed) the command-class producer falls back to
 * emitting a bailout row and the downstream selector takes the safe branch.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class ClassifyChangedSymbols extends ScanningRecipe<ClassifyChangedSymbols.Accumulator> {

    transient ChangedSymbolsDataTable changedSymbols = new ChangedSymbolsDataTable(this);

    @Override
    public String getDisplayName() {
        return "Classify changed symbols from a change set";
    }

    @Override
    public String getDescription() {
        return "Reads `ChangedFilesDataTable` and, for each changed Java source file, emits " +
                "one `ChangedSymbolsDataTable` row per declared class, method, field, or " +
                "constructor. The resulting rows seed Phase 2 reachability analysis.";
    }

    public static class Accumulator {
        /** Repository-relative path → changeType ("ADDED" | "MODIFIED" | "DELETED"). */
        final Map<String, String> changedPathToType = new HashMap<>();
        /** Dedup: (className, memberKind, memberName). */
        final Set<String> emittedKeys = new HashSet<>();
    }

    @Value
    private static class PendingRow {
        String className;
        String memberName;
        String memberKind;
        String changeType;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        Accumulator acc = new Accumulator();
        DataTableStore store = DataTableExecutionContextView.view(ctx).getDataTableStore();
        try (Stream<ChangedFilesDataTable.Row> rows =
                     store.getRows(ChangedFilesDataTable.class)) {
            rows.forEach(row -> {
                if (row.getPath() != null) {
                    String normalized = row.getPath().replace('\\', '/');
                    acc.changedPathToType.put(
                            normalized,
                            row.getChangeType() == null ? "MODIFIED" : row.getChangeType());
                }
            });
        }
        return acc;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (acc.changedPathToType.isEmpty() || !(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                String sourcePath = sourceFile.getSourcePath().toString().replace('\\', '/');
                String changeType = acc.changedPathToType.get(sourcePath);
                if (changeType == null) {
                    return tree;
                }

                JavaSourceFile cu = (JavaSourceFile) tree;
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext c) {
                        JavaType.FullyQualified classType = classDecl.getType();
                        if (classType == null) {
                            return super.visitClassDeclaration(classDecl, c);
                        }
                        String fqn = classType.getFullyQualifiedName();
                        emit(fqn, "", "CLASS");

                        // Walk the class body for members — use the type-attributed members rather
                        // than revisiting through the JavaIsoVisitor (which would miss simple
                        // type info for anonymous nested cases).
                        if (classDecl.getBody() != null) {
                            for (Statement stmt : classDecl.getBody().getStatements()) {
                                if (stmt instanceof J.MethodDeclaration) {
                                    J.MethodDeclaration md = (J.MethodDeclaration) stmt;
                                    JavaType.Method mt = md.getMethodType();
                                    if (mt != null) {
                                        if (mt.isConstructor()) {
                                            emit(fqn, "<constructor>", "CONSTRUCTOR");
                                        } else {
                                            emit(fqn, mt.getName(), "METHOD");
                                        }
                                    } else {
                                        // No type attribution — fall back to the simple name.
                                        String name = md.getSimpleName();
                                        if (md.isConstructor()) {
                                            emit(fqn, "<constructor>", "CONSTRUCTOR");
                                        } else {
                                            emit(fqn, name, "METHOD");
                                        }
                                    }
                                } else if (stmt instanceof J.VariableDeclarations) {
                                    J.VariableDeclarations vd = (J.VariableDeclarations) stmt;
                                    for (J.VariableDeclarations.NamedVariable nv : vd.getVariables()) {
                                        emit(fqn, nv.getSimpleName(), "FIELD");
                                    }
                                }
                            }
                        }

                        return super.visitClassDeclaration(classDecl, c);
                    }

                    private void emit(String className, String memberName, String memberKind) {
                        String key = className + "\0" + memberKind + "\0" + memberName;
                        if (acc.emittedKeys.add(key)) {
                            changedSymbols.insertRow(ctx, new ChangedSymbolsDataTable.Row(
                                    className, memberName, memberKind, changeType));
                        }
                    }
                }.visit(cu, ctx);

                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        return emptyList();
    }

    @Override
    public int maxCycles() {
        return 1;
    }
}
