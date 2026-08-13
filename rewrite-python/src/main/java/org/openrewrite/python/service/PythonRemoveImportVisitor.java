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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.python.tree.Py;

import java.util.HashMap;
import java.util.Map;

import static org.openrewrite.python.internal.PythonImportNames.canonicalFqn;
import static org.openrewrite.python.internal.PythonImportNames.nameString;

/**
 * Dispatches to {@code rewrite.python.remove_import.RemoveImport}.
 */
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
public class PythonRemoveImportVisitor<P> extends RpcImportVisitor<P> {

    private final String module;
    private final @Nullable String name;

    /**
     * Mirrors the Python option of the same name. {@code removeImportVisitor} has no parameter for
     * it, so a service-constructed visitor always leaves an import that is still referenced alone.
     */
    private final boolean onlyIfUnused;

    @JsonIgnore
    public PythonRemoveImportVisitor(String module, @Nullable String name) {
        this(module, name, true);
    }

    @Override
    String visitorName() {
        return "org.openrewrite.python.RemoveImport";
    }

    @Override
    Map<String, Object> options() {
        Map<String, Object> options = new HashMap<>();
        options.put("module", module);
        options.put("name", name);
        options.put("only_if_unused", onlyIfUnused);
        return options;
    }

    @Override
    boolean mightChange(Py.CompilationUnit cu) {
        for (Statement statement : cu.getStatements()) {
            if (statement instanceof J.Import) {
                if (name == null && module.equals(nameString(((J.Import) statement).getQualid()))) {
                    return true;
                }
            } else if (statement instanceof Py.MultiImport && hasCandidate((Py.MultiImport) statement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether any member of the statement is one the Python implementation would consider removing.
     * {@code only_if_unused} is not evaluated here: it can only retain a candidate, so leaving it
     * out risks a dispatch that changes nothing rather than a skip that loses a change.
     */
    private boolean hasCandidate(Py.MultiImport multi) {
        NameTree from = multi.getFrom();
        if (name == null) {
            if (from == null) {
                for (J.Import member : multi.getNames()) {
                    if (module.equals(nameString(member.getQualid()))) {
                        return true;
                    }
                }
                return false;
            }
            if (module.equals(nameString(from))) {
                return !multi.getNames().isEmpty();
            }
            for (J.Import member : multi.getNames()) {
                if (module.equals(canonicalFqn(member))) {
                    return true;
                }
            }
            return false;
        }
        if (from == null) {
            return false;
        }
        boolean syntactic = module.equals(nameString(from));
        String targetFqn = module + "." + name;
        for (J.Import member : multi.getNames()) {
            if (syntactic && name.equals(nameString(member.getQualid())) ||
                    targetFqn.equals(canonicalFqn(member))) {
                return true;
            }
        }
        return false;
    }
}
