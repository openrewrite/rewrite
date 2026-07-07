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
package org.openrewrite.python.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.python.PythonIsoVisitor;
import org.openrewrite.python.tree.Py;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Match Python source files that import a given module, by the as-written
 * import syntax rather than by type attribution.
 * <p>
 * This is the host-evaluable counterpart of the Python {@code uses_import(...)}
 * precondition helper. It exists because {@link org.openrewrite.java.search.HasType}
 * (the {@code uses_type} gate) cannot reliably gate import-migration recipes on
 * Python: the type checker canonicalizes aliases (so {@code from typing import List}
 * resolves to {@code list} and never to {@code typing.List}), removed symbols
 * (e.g. {@code from base64 import encodestring}) get no attribution at all, and
 * Python imports never reach {@code TypesInUse} in the first place. Reading the
 * import path off {@link Py.MultiImport} sidesteps all of that.
 * <p>
 * The match is a deliberate superset (module, submodule, or parent module), which
 * is what a precondition wants: over-matching merely runs the gated visitor, while
 * under-matching would skip a file the recipe should change.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UsesImport extends Recipe {

    @Option(displayName = "Module",
            description = "The dotted module path to match against `import` statements, e.g. `datetime` " +
                          "or `os.path`. A file matches if it imports that module, a submodule of it, or " +
                          "a parent module of it. Matched against import syntax, not type attribution.",
            example = "datetime")
    String module;

    String displayName = "Find Python files that import a module";

    String description = "Marks Python source files that import the given module, matching the as-written " +
                         "import path rather than type attribution. Robust to type-checker canonicalization " +
                         "and to removed or unresolvable symbols, which makes it usable as a precondition for " +
                         "import-migration recipes where `HasType` would miss the file.";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", module);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof Py.CompilationUnit;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof Py.CompilationUnit)) {
                    return tree;
                }
                Py.CompilationUnit cu = (Py.CompilationUnit) tree;
                AtomicBoolean found = new AtomicBoolean(false);
                new PythonIsoVisitor<AtomicBoolean>() {
                    @Override
                    public Py.MultiImport visitMultiImport(Py.MultiImport multiImport, AtomicBoolean f) {
                        if (f.get()) {
                            return multiImport;
                        }
                        if (multiImport.getFrom() != null) {
                            // `from <module> import ...` — the module is the `from` clause. The
                            // imported names are symbols, not modules, so we do not descend.
                            if (importPathMatches(dottedName(multiImport.getFrom()), module)) {
                                f.set(true);
                            }
                            return multiImport;
                        }
                        // `import <a>, <b>` — each name's qualid is itself a module.
                        for (J.Import name : multiImport.getNames()) {
                            if (importPathMatches(dottedName(name.getQualid()), module)) {
                                f.set(true);
                                return multiImport;
                            }
                        }
                        return multiImport;
                    }

                    @Override
                    public J.Import visitImport(J.Import import_, AtomicBoolean f) {
                        // A standalone `import <module>` statement (the parser emits a bare
                        // J.Import for these, not a MultiImport). Children of a MultiImport are
                        // never reached here because visitMultiImport does not descend.
                        if (f.get()) {
                            return import_;
                        }
                        if (importPathMatches(dottedName(import_.getQualid()), module)) {
                            f.set(true);
                        }
                        return import_;
                    }
                }.visit(cu, found);
                return found.get() ? SearchResult.found(cu) : cu;
            }
        };
    }

    /**
     * Reconstruct a dotted module path from an import qualid or `from` clause,
     * handling both {@link J.Identifier} (single segment) and nested
     * {@link J.FieldAccess} (e.g. {@code os.path}). Mirrors the Python parser's
     * {@code get_name_string} / {@code get_qualid_name} helpers.
     */
    private static @Nullable String dottedName(@Nullable J expr) {
        if (expr instanceof J.Identifier) {
            return ((J.Identifier) expr).getSimpleName();
        }
        if (expr instanceof J.FieldAccess) {
            J.FieldAccess fa = (J.FieldAccess) expr;
            Expression target = fa.getTarget();
            String targetName = dottedName(target);
            return targetName == null ? fa.getSimpleName() : targetName + "." + fa.getSimpleName();
        }
        return null;
    }

    private static boolean importPathMatches(@Nullable String imported, String query) {
        if (imported == null || imported.isEmpty() || query.isEmpty()) {
            return false;
        }
        // Dotted-boundary aware so `os` does not match `ossaudiodev`.
        return imported.equals(query) ||
               imported.startsWith(query + ".") ||
               query.startsWith(imported + ".");
    }
}
