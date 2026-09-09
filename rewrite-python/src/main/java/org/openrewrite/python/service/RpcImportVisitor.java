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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.python.tree.Py;
import org.openrewrite.rpc.RewriteRpc;

import java.util.Map;

/**
 * Runs one of the Python side's own import visitors over RPC, with equality on its fields so
 * {@code maybeAddImport}/{@code maybeRemoveImport} can deduplicate the queued visitors.
 */
@EqualsAndHashCode(callSuper = false)
abstract class RpcImportVisitor<P> extends JavaVisitor<P> {

    abstract String visitorName();

    abstract Map<String, Object> options();

    /**
     * Whether the Python implementation could change this file. Transcribes its early returns, so
     * a false answer means the round trip would have handed back the tree it was given.
     */
    abstract boolean mightChange(Py.CompilationUnit cu);

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
        if (tree instanceof Py.CompilationUnit && !mightChange((Py.CompilationUnit) tree)) {
            return (J) tree;
        }
        RewriteRpc rpc = PythonRewriteRpc.get();
        if (rpc == null) {
            // Without a process-manager handle, the peer whose request is being handled is
            // the one that implements the import visitors; see RewriteRpc.current().
            rpc = RewriteRpc.current();
        }
        if (rpc == null) {
            return (J) tree;
        }
        // An import visitor never deletes the file, so a null result is an RPC desync rather than
        // a real deletion; return the file unchanged instead of dropping it from the result set.
        Tree result = rpc.visit(tree, visitorName(), options(), p, parent);
        return result != null ? (J) result : (J) tree;
    }
}
