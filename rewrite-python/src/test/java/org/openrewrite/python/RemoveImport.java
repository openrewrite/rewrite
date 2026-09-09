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
package org.openrewrite.python;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

/**
 * Test stand-in for the Python-side {@code rewrite.python.remove_import.RemoveImport} visitor,
 * which registers under this fully-qualified name. Marks the tree with the (module, name) pair
 * it was asked to remove, so tests can assert the dispatch arrived with the right options.
 */
public class RemoveImport extends JavaVisitor<Object> {
    public String module;

    public @Nullable String name;

    @Override
    public @Nullable J preVisit(J tree, Object o) {
        stopAfterPreVisit();
        return SearchResult.found(tree, name == null ? module : module + "." + name);
    }
}
