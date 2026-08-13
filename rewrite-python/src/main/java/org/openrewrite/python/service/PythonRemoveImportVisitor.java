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
import org.openrewrite.python.tree.Py;

import java.util.HashMap;
import java.util.Map;

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
        return true;
    }
}
