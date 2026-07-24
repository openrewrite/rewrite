/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.service;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.J;
import org.openrewrite.python.rpc.PythonRewriteRpc;

import java.util.HashMap;
import java.util.Map;

/**
 * Dispatches {@link org.openrewrite.java.JavaVisitor#maybeAddImport}/{@code maybeRemoveImport} to
 * the Python side over RPC, since Java's {@link ImportService} emits a {@code J.Import} that prints
 * as invalid {@code import a.b.C} and works off an empty {@code getImports()} for Python.
 */
public class PythonImportService extends ImportService {

    private static final String ADD_IMPORT_VISITOR = "org.openrewrite.python.AddImport";
    private static final String REMOVE_IMPORT_VISITOR = "org.openrewrite.python.RemoveImport";

    @Override
    public <P> JavaVisitor<P> addImportVisitor(@Nullable String packageName,
                                               String typeName,
                                               @Nullable String member,
                                               @Nullable String alias,
                                               boolean onlyIfReferenced) {
        return new PythonAddImportVisitor<>(packageName, typeName, member, alias, onlyIfReferenced);
    }

    @Override
    public <P> JavaVisitor<P> removeImportVisitor(String fullyQualifiedName) {
        return new PythonRemoveImportVisitor<>(fullyQualifiedName);
    }

    /**
     * Splits a fully-qualified name into Python's (module, name) pair, e.g. {@code collections.abc}
     * + {@code Iterable}; a name with no dot becomes a plain {@code import <name>} with a null name.
     */
    private static Map<String, Object> moduleAndName(@Nullable String packageName, String typeName) {
        Map<String, Object> options = new HashMap<>();
        if (packageName == null || packageName.isEmpty()) {
            options.put("module", typeName);
            options.put("name", null);
        } else {
            options.put("module", packageName);
            options.put("name", typeName);
        }
        return options;
    }

    /**
     * Runs one of the Python side's own import visitors over RPC, with equality on its fields so
     * {@code maybeAddImport}/{@code maybeRemoveImport} can deduplicate the queued visitors.
     */
    @EqualsAndHashCode(callSuper = false)
    private abstract static class RpcImportVisitor<P> extends JavaVisitor<P> {

        abstract String visitorName();

        abstract Map<String, Object> options();

        @Override
        public @Nullable J visit(@Nullable Tree tree, P p, Cursor parent) {
            return dispatch(tree, p, parent);
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, P p) {
            return dispatch(tree, p, null);
        }

        private @Nullable J dispatch(@Nullable Tree tree, P p, @Nullable Cursor parent) {
            if (!(tree instanceof SourceFile)) {
                return (J) tree;
            }
            PythonRewriteRpc rpc = PythonRewriteRpc.get();
            if (rpc == null) {
                return (J) tree;
            }
            // An import visitor never deletes the file, so a null result is an RPC desync rather than
            // a real deletion; return the file unchanged instead of dropping it from the result set.
            Tree result = rpc.visit(tree, visitorName(), options(), p, parent);
            return result != null ? (J) result : (J) tree;
        }
    }

    /**
     * Dispatches to {@code rewrite.python.add_import.AddImport}.
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    private static class PythonAddImportVisitor<P> extends RpcImportVisitor<P> {
        private final @Nullable String packageName;
        private final String typeName;
        private final @Nullable String member;
        private final @Nullable String alias;
        private final boolean onlyIfReferenced;

        @Override
        String visitorName() {
            return ADD_IMPORT_VISITOR;
        }

        @Override
        Map<String, Object> options() {
            // A `member` is Java's static import, whose Python equivalent imports the member from
            // the type's own module: `from <package>.<type> import <member>`.
            Map<String, Object> options = member == null ?
                    moduleAndName(packageName, typeName) :
                    moduleAndName(packageName == null ? typeName : packageName + "." + typeName, member);
            options.put("alias", alias);
            options.put("only_if_referenced", onlyIfReferenced);
            return options;
        }
    }

    /**
     * Dispatches to {@code rewrite.python.remove_import.RemoveImport}.
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    private static class PythonRemoveImportVisitor<P> extends RpcImportVisitor<P> {
        private final String fullyQualifiedName;

        @Override
        String visitorName() {
            return REMOVE_IMPORT_VISITOR;
        }

        @Override
        Map<String, Object> options() {
            int lastDot = fullyQualifiedName.lastIndexOf('.');
            return lastDot == -1 ?
                    moduleAndName(null, fullyQualifiedName) :
                    moduleAndName(fullyQualifiedName.substring(0, lastDot), fullyQualifiedName.substring(lastDot + 1));
        }
    }
}
