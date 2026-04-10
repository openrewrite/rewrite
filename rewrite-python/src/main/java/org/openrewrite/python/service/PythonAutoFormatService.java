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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.tree.J;
import org.openrewrite.python.rpc.PythonRewriteRpc;

/**
 * Auto-format service for Python that dispatches formatting to the Python
 * RPC side, where the actual formatting visitors (blank lines, spaces,
 * indentation, etc.) are implemented.
 */
public class PythonAutoFormatService extends AutoFormatService {

    private static final String AUTO_FORMAT_VISITOR = "org.openrewrite.python.format.AutoFormatVisitor";

    @Override
    public <P> JavaVisitor<P> autoFormatVisitor(@Nullable Tree stopAfter) {
        return new JavaIsoVisitor<P>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, P p, Cursor parent) {
                if (tree == null) {
                    return null;
                }
                PythonRewriteRpc rpc = PythonRewriteRpc.get();
                if (rpc == null) {
                    return (J) tree;
                }
                return (J) rpc.visit(tree, AUTO_FORMAT_VISITOR, p, parent);
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, P p) {
                if (tree == null) {
                    return null;
                }
                if (tree instanceof SourceFile) {
                    PythonRewriteRpc rpc = PythonRewriteRpc.get();
                    if (rpc != null) {
                        return (J) rpc.visit((SourceFile) tree, AUTO_FORMAT_VISITOR, p);
                    }
                }
                return (J) tree;
            }
        };
    }
}
