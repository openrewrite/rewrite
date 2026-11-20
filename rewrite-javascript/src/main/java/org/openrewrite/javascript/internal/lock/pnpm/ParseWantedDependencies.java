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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Port of @pnpm/installing.deps-installer src/parseWantedDependencies.ts (pnpm 11.2.2) for a dependency added anew.
 */
final class ParseWantedDependencies {

    private ParseWantedDependencies() {
    }

    /**
     * The bare specifier pnpm wants for {@code alias}: {@code catalog:} where the default catalog holds the one given.
     */
    static String bareSpecifier(String alias, String bareSpecifier,
                                @Nullable Map<String, @Nullable Object> defaultCatalog) {
        Object inDefault = defaultCatalog == null ? null : defaultCatalog.get(alias);
        return Js.truthy(inDefault) && Js.strictEquals(inDefault, bareSpecifier) ? "catalog:" : bareSpecifier;
    }
}
