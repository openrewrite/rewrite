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

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Port of @pnpm/lockfile.types {@code ProjectSnapshot} (pnpm 11.2.2): one importer's specifiers by alias and what
 * each dependency field resolved them to.
 */
@Getter
@Setter
final class ProjectSnapshot {

    static final String[] DEPENDENCIES_FIELDS = {"optionalDependencies", "dependencies", "devDependencies"};

    private Map<String, @Nullable String> specifiers = new LinkedHashMap<>();

    private @Nullable Map<String, @Nullable String> dependencies;

    private @Nullable Map<String, @Nullable String> devDependencies;

    private @Nullable Map<String, @Nullable String> optionalDependencies;

    private @Nullable Object dependenciesMeta;

    private @Nullable Object publishDirectory;

    /**
     * {@code importer[depField]}.
     */
    @Nullable
    Map<String, @Nullable String> field(String depField) {
        switch (depField) {
            case "dependencies":
                return dependencies;
            case "devDependencies":
                return devDependencies;
            case "optionalDependencies":
                return optionalDependencies;
            default:
                throw new IllegalArgumentException("Unknown dependency type \"" + depField + "\"");
        }
    }

    void setField(String depField, @Nullable Map<String, @Nullable String> refs) {
        switch (depField) {
            case "dependencies":
                dependencies = refs;
                break;
            case "devDependencies":
                devDependencies = refs;
                break;
            case "optionalDependencies":
                optionalDependencies = refs;
                break;
            default:
                throw new IllegalArgumentException("Unknown dependency type \"" + depField + "\"");
        }
    }
}
