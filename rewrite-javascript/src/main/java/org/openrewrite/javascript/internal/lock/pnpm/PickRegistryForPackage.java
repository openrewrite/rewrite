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
 * Port of @pnpm/config.pick-registry-for-package src/index.ts (pnpm 11.2.2).
 */
final class PickRegistryForPackage {

    private PickRegistryForPackage() {
    }

    static String pickRegistryForPackage(Map<String, String> registries, String packageName,
                                         @Nullable String bareSpecifier) {
        String scope = getScope(packageName, bareSpecifier);
        if (scope == null) {
            return registries.get("default");
        }
        if (scope.isEmpty()) {
            // `"" ?? registries.default` is ""
            return "";
        }
        String registry = registries.get(scope);
        return registry != null ? registry : registries.get("default");
    }

    private static @Nullable String getScope(String pkgName, @Nullable String bareSpecifier) {
        if (bareSpecifier != null && bareSpecifier.startsWith("npm:")) {
            String target = bareSpecifier.substring(4);
            if (!target.isEmpty() && target.charAt(0) == '@') {
                return target.substring(0, Math.max(0, target.indexOf('/')));
            }
            return null;
        }
        if (!pkgName.isEmpty() && pkgName.charAt(0) == '@') {
            return pkgName.substring(0, Math.max(0, pkgName.indexOf('/')));
        }
        return null;
    }
}
