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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.python.PythonVisitor;
import org.openrewrite.python.internal.PythonBuiltins;
import org.openrewrite.python.tree.Py;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openrewrite.python.internal.PythonImportNames.aliasName;
import static org.openrewrite.python.internal.PythonImportNames.canonicalFqn;
import static org.openrewrite.python.internal.PythonImportNames.nameString;

/**
 * Dispatches to {@code rewrite.python.add_import.AddImport}.
 */
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
public class PythonAddImportVisitor<P> extends RpcImportVisitor<P> {

    private final String module;
    private final @Nullable String name;
    private final @Nullable String alias;
    private final boolean onlyIfReferenced;

    @Override
    String visitorName() {
        return "org.openrewrite.python.AddImport";
    }

    @Override
    Map<String, Object> options() {
        Map<String, Object> options = new HashMap<>();
        options.put("module", module);
        options.put("name", name);
        options.put("alias", alias);
        options.put("only_if_referenced", onlyIfReferenced);
        return options;
    }

    @Override
    boolean mightChange(Py.CompilationUnit cu) {
        // builtins are always available; only import them under an explicit alias.
        if ("builtins".equals(module) && alias == null) {
            return false;
        }
        // A bare builtin name (e.g. `list` when ChangeType retargets a type to a builtin) is not a
        // module, so there is no import that could bind it.
        if (name == null && module.indexOf('.') == -1 && PythonBuiltins.contains(module)) {
            return false;
        }
        if (importExists(cu)) {
            return false;
        }
        return !onlyIfReferenced || isReferenced(cu);
    }

    private boolean importExists(Py.CompilationUnit cu) {
        for (Statement statement : cu.getStatements()) {
            if (statement instanceof J.Import) {
                if (name == null && bindsExactly((J.Import) statement, module)) {
                    return true;
                }
            } else if (statement instanceof Py.MultiImport && satisfies((Py.MultiImport) statement)) {
                return true;
            }
        }
        return false;
    }

    private boolean satisfies(Py.MultiImport multi) {
        NameTree from = multi.getFrom();
        if (name == null) {
            if (from != null) {
                return false;
            }
            for (J.Import member : multi.getNames()) {
                if (bindsExactly(member, module)) {
                    return true;
                }
            }
            return false;
        }
        if (from == null) {
            return false;
        }
        boolean syntactic = nameString(from).equals(module);
        String targetFqn = module + "." + name;
        String requestedBinding = alias != null ? alias : name;
        for (J.Import member : multi.getNames()) {
            if (syntactic && bindsExactly(member, name)) {
                return true;
            }
            // A member canonically matching the request satisfies it only when it binds the same
            // name, so references to that name keep resolving through it.
            if (targetFqn.equals(canonicalFqn(member))) {
                String memberAlias = aliasName(member);
                String bound = memberAlias != null ? memberAlias : nameString(member.getQualid());
                if (requestedBinding.equals(bound)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean bindsExactly(J.Import imp, @Nullable String expected) {
        return nameString(imp.getQualid()).equals(expected) && Objects.equals(alias, aliasName(imp));
    }

    private boolean isReferenced(Py.CompilationUnit cu) {
        String target = alias != null ? alias :
                name != null ? name : module.substring(module.lastIndexOf('.') + 1);
        AtomicBoolean found = new AtomicBoolean();
        new PythonVisitor<AtomicBoolean>() {
            // Identifiers inside an import are bindings rather than uses, so neither kind of
            // import statement is descended into (#8409).
            @Override
            public J visitImport(J.Import imp, AtomicBoolean found) {
                return imp;
            }

            @Override
            public J visitMultiImport(Py.MultiImport multi, AtomicBoolean found) {
                return multi;
            }

            @Override
            public J visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                if (target.equals(identifier.getSimpleName())) {
                    found.set(true);
                }
                return identifier;
            }
        }.visit(cu, found);
        return found.get();
    }
}
