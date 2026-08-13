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
import org.openrewrite.python.tree.Py;

import java.util.HashMap;
import java.util.Map;

/**
 * Dispatches to {@code rewrite.python.add_import.AddImport}.
 */
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
public class PythonAddImportVisitor<P> extends RpcImportVisitor<P> {

    private final @Nullable String module;
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
        return true;
    }
}
